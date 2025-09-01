package io.cockroachdb.hibachi.web.workload;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.validation.Valid;

import io.cockroachdb.hibachi.web.chart.Metrics;
import io.cockroachdb.hibachi.web.common.MessagePublisher;
import io.cockroachdb.hibachi.web.common.TopicName;
import io.cockroachdb.hibachi.web.common.WebController;
import io.cockroachdb.hibachi.web.editor.ConfigModel;
import io.cockroachdb.hibachi.web.editor.DataSourceCreatedEvent;
import io.cockroachdb.hibachi.web.editor.Slot;

@WebController
@RequestMapping("/workload")
@SessionAttributes("configModel")
public class WorkloadController {
    @Autowired
    private WorkloadExecutor workloadExecutor;

    @Autowired
    private WorkloadManager workloadManager;

    @Autowired
    private MessagePublisher messagePublisher;

    private final Map<String, DataSource> connectionPools = new HashMap<>();

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void modelUpdate() {
        messagePublisher.convertAndSend(TopicName.WORKLOAD_UPDATE, null);
    }

    @EventListener
    public void handle(DataSourceCreatedEvent event) {
        try {
            HikariDataSource dataSource = event.getDataSource().unwrap(HikariDataSource.class);
            Objects.requireNonNull(dataSource.getPoolName(), "pool name is required");
            connectionPools.put(dataSource.getPoolName().toLowerCase(), dataSource);
        } catch (SQLException e) {
            throw new ApplicationContextException("", e);
        }
    }

    @EventListener
    public void handle(WorkloadUpdatedEvent event) {
        messagePublisher.convertAndSend(TopicName.WORKLOAD_REFRESH, null);
    }

    @ModelAttribute("workloadForm")
    public WorkloadForm workloadFormModel(
            @ModelAttribute(value = "configModel", binding = false) ConfigModel configModel
    ) {
        WorkloadForm form = new WorkloadForm();
        form.setDuration(Duration.ofMinutes(5).toSeconds());
        form.setCount(4);
        form.setWorkloadType(WorkloadType.random_wait);

        List<Slot> occupiedSlots = configModel
                .getSlots()
                .stream()
                .filter(Slot::isOccupied).toList();
        form.setSlots(occupiedSlots);

        return form;
    }

    @GetMapping
    public Callable<String> indexPage(@PageableDefault(size = 15) Pageable page,
                                      Model model) {

        Page<WorkloadModel> workloadPage = workloadManager.getWorkloads(page,
                workload -> {
                    return true;
                });

        model.addAttribute("workloadPage", workloadPage);
        model.addAttribute("aggregatedMetrics", workloadManager.getMetricsAggregate(page));

        return () -> "workload";
    }

    @PostMapping(params = "action=add")
    public Callable<String> submitWorkloadForm(@ModelAttribute("workloadForm") @Valid WorkloadForm workloadForm,
                                               BindingResult bindingResult,
                                               Model model) {


        final WorkloadType workloadType = workloadForm.getWorkloadType();
        if (workloadType.isRequiresDataSource() && Objects.isNull(workloadForm.getSlot())) {
            bindingResult.addError(new ObjectError("globalError", "Selected workload requires a datasource!"));
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getFieldErrors());
            model.addAttribute("workloadPage", Page.empty());
            return () -> "workload";
        }

        final DataSource dataSource;

        if (Objects.nonNull(workloadForm.getSlot())) {
            dataSource = connectionPools.get(workloadForm.getSlot().getName().toLowerCase());
            Objects.requireNonNull(dataSource, "dataSource not found");
        } else {
            dataSource = null;
        }

        final Duration duration = Duration.ofSeconds(workloadForm.getDuration());

        IntStream.rangeClosed(1, workloadForm.getCount())
                .forEach(value -> {
                    final Runnable workloadTask = workloadType
                            .startWorkload(dataSource);
                    final WorkloadModel workloadModel = workloadExecutor
                            .submitWorkloadTask(workloadType.getDescription(), duration, workloadTask);
                    workloadManager.addWorkload(workloadModel);
                });

        model.addAttribute("workloadForm", workloadForm);

        return () -> "redirect:workload";
    }

    @GetMapping("{id}")
    public Callable<String> getWorkloadDetails(@PathVariable("id") Integer id, Model model) {
        return () -> {
            model.addAttribute("workload", workloadManager.getWorkloadById(id));
            return "workload-detail";
        };
    }

    @PostMapping(value = "/cancelAll")
    public RedirectView cancelAll() {
        workloadManager.cancelWorkloads();
        return new RedirectView("/workload");
    }

    @PostMapping(value = "/deleteAll")
    public RedirectView deleteAll() {
        workloadManager.deleteWorkloads();
        return new RedirectView("/workload");
    }

    @GetMapping(value = "/cancel/{id}")
    public RedirectView cancel(@PathVariable("id") Integer id) {
        workloadManager.cancelWorkload(id);
        return new RedirectView("/workload");
    }

    @GetMapping(value = "/delete/{id}")
    public RedirectView delete(@PathVariable("id") Integer id) {
        workloadManager.deleteWorkload(id);
        return new RedirectView("/workload");
    }

    @GetMapping("/items")
    public @ResponseBody List<WorkloadModel> getWorkloadItems(Pageable page) {
        return workloadManager.getWorkloads(page, (x) -> true).getContent();
    }

    @GetMapping("/summary")
    public @ResponseBody Metrics getWorkloadSummary(Pageable page) {
        return workloadManager.getMetricsAggregate(page);
    }
}

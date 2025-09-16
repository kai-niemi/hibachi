package io.cockroachdb.hibachi.web.editor;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import io.cockroachdb.hibachi.config.ClosableDataSource;
import io.cockroachdb.hibachi.web.common.MessagePublisher;
import io.cockroachdb.hibachi.web.common.Toast;
import io.cockroachdb.hibachi.web.common.TopicName;
import io.cockroachdb.hibachi.web.common.WebController;
import io.cockroachdb.hibachi.web.editor.model.DataSourceModel;
import io.cockroachdb.hibachi.web.editor.model.HikariConfigModel;
import io.cockroachdb.hibachi.web.editor.model.RootModel;
import static io.cockroachdb.hibachi.web.editor.model.HikariConfigModel.toHikariModel;

@WebController
@RequestMapping("/editor")
@SessionAttributes("configModel")
public class EditorController {
    public static final String DEFAULT_VALIDATION_QUERY = "select version()";

    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private Function<DataSourceModel, ClosableDataSource> dataSourceFactory;

    @ModelAttribute("configModel")
    public ConfigModel configModelForm() {
        ConfigModel form = new ConfigModel();
        form.setNumVCPUs(3 * 8);
        form.setNumInstances(1);
        form.setConnectionLifeTimeSeconds(300L);
        form.setMultiplier(Multiplier.X4);
        form.setAppName("Hibachi");
        form.setUserName("root");
        form.setPassword("");
        form.setUrl("jdbc:postgresql://localhost:26257/defaultdb?sslmode=disable");
        form.setReWriteBatchedInserts(true);
        form.setApplicationModelYaml("# Press 'Generate' to create a sample application.yml");
        form.setSlot(Slot.ONE);
        form.setSlots(List.of(Slot.ONE, Slot.TWO, Slot.THREE, Slot.FOUR));
        form.setConfigProfile(ConfigProfile.OPTIMIZED);
        form.setTraceLogging(false);
        form.applyConfigStrategy();
        return form;
    }

    @GetMapping
    public Callable<String> indexPage() {
        return () -> "editor";
    }

    @PostMapping(params = "action=apply")
    public Callable<String> applyConfigurationStrategy(@ModelAttribute("configModel")
                                                           @Valid ConfigModel configModel,
                                                       BindingResult bindingResult,
                                                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getFieldErrors());
            return () -> "editor";
        }

        return () -> {
            try {
                configModel.applyConfigStrategy();
            } catch (IllegalArgumentException e) {
                bindingResult.addError(new ObjectError("globalError", e.getMessage()));
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("errors", bindingResult.getFieldErrors());
                return "editor";
            }

            model.addAttribute("configModel", configModel);
            return "editor";
        };
    }

    @PostMapping(params = "action=generate")
    public Callable<String> generateConfigurationYaml(@ModelAttribute("configModel")
                                                      @Valid ConfigModel configModel,
                                                      BindingResult bindingResult,
                                                      Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getFieldErrors());
            return () -> "editor";
        }

        return () -> {
            try {
                DataSourceModel dataSourceModel = DataSourceModel.builder()
                        .withHikariConfig(toHikariModel(configModel))
                        .withDriverClassName("org.postgresql.Driver")
                        .withUrl(configModel.getUrl())
                        .withUsername(configModel.getUserName())
                        .withPassword(StringUtils.hasLength(configModel.getPassword()) ? "*****" : null)
                        .withTraceLogging(configModel.isTraceLogging())
                        .build();

                try {
                    String yaml = yamlObjectMapper.writeValueAsString(RootModel.of(dataSourceModel));
                    configModel.setApplicationModelYaml(yaml);
                } catch (JsonProcessingException e) {
                    configModel.setApplicationModelYaml(e.toString());
                }
            } catch (IllegalArgumentException e) {
                bindingResult.addError(new ObjectError("globalError", e.getMessage()));
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("errors", bindingResult.getFieldErrors());
                return "editor";
            }

            model.addAttribute("configModel", configModel);
            return "editor";
        };
    }

    @PostMapping(params = "action=validate")
    public Callable<String> validateDataSource(@ModelAttribute("configModel")
                                               @Valid ConfigModel configModel,
                                               BindingResult bindingResult,
                                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getFieldErrors());
            return () -> "editor";
        }

        return () -> {
            HikariConfigModel hikariConfig = toHikariModel(configModel);
            hikariConfig.setReadOnly(true);

            try (ClosableDataSource dataSource = dataSourceFactory.apply(DataSourceModel.builder()
                    .withHikariConfig(hikariConfig)
                    .withDriverClassName("org.postgresql.Driver")
                    .withUrl(configModel.getUrl())
                    .withUsername(configModel.getUserName())
                    .withPassword(configModel.getPassword())
                    .withTraceLogging(configModel.isTraceLogging())
                    .build())) {

                final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                String query = configModel.getValidationQuery();
                if (StringUtils.hasLength(query)) {
                    Assert.state(query.toUpperCase().startsWith("SELECT "),
                            "Validation query must start with 'SELECT'");
                } else {
                    query = DEFAULT_VALIDATION_QUERY;
                }

                //noinspection SqlSourceToSinkFlow
                model.addAttribute("testSuccess", jdbcTemplate.queryForObject(query, String.class));
            } catch (Exception e) {
                model.addAttribute("testFailure", e.getMessage());
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("errors", bindingResult.getFieldErrors());
                return "editor";
            }

            model.addAttribute("configModel", configModel);
            return "editor";
        };
    }

    @PostMapping(params = "action=pin")
    public Callable<String> pinDataSourceConfig(@ModelAttribute("configModel")
                                          @Valid ConfigModel configModel,
                                                BindingResult bindingResult,
                                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getFieldErrors());
            return () -> "editor";
        }

        return () -> {
            try {
                Slot slot = configModel.getSlots()
                        .stream()
                        .filter(x -> x.equals(configModel.getSlot()))
                        .findFirst()
                        .orElseThrow();
                slot.setOccupied(true);

                messagePublisher.convertAndSend(TopicName.TOAST_MESSAGE,
                        Toast.of("Pinned datasource configuration to slot %s."
                                .formatted(configModel.getSlot().getName())));

                applicationEventPublisher.publishEvent(
                        new DataSourceConfigPinnedEvent(this, configModel));
            } catch (Exception e) {
                bindingResult.addError(new ObjectError("globalError", e.getMessage()));
            }

            if (bindingResult.hasErrors()) {
                model.addAttribute("errors", bindingResult.getFieldErrors());
                return "editor";
            }

            model.addAttribute("configModel", configModel);
            return "editor";
        };
    }

    @PostMapping(params = "action=reset")
    public Callable<String> resetConfiguration(@ModelAttribute("configModel") ConfigModel form,
                                               SessionStatus status) {
        return () -> {
            status.setComplete();
            return "redirect:/editor";
        };
    }
}

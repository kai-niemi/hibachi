package io.cockroachdb.hibachi.web.editor;

import java.io.UncheckedIOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class YamlObjectMapperHelper {
    @Autowired
    @Qualifier("yamlObjectMapper")
    private ObjectMapper yamlObjectMapper;

    public <T> String mapToYaml(T object) {
        try {
            return yamlObjectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T readFromYaml(Class<T> type, String applicationYaml) {
        try {
            return yamlObjectMapper.readerFor(type)
                    .readValue(applicationYaml);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}

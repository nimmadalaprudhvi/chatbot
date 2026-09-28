package com.invoke.chatbot.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoke.chatbot.journey.JourneyStep;
import com.invoke.chatbot.response.Category;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

@Configuration
public class ChatbotConfig {

    private static final String JOURNEY_RESOURCE = "data/journey.json";
    private static final String OFFTOPIC_RESOURCE = "data/offtopic.json";

    @Bean
    public List<JourneyStep> journey(ObjectMapper objectMapper) throws IOException {
        return readResource(
                objectMapper,
                JOURNEY_RESOURCE,
                new TypeReference<List<JourneyStep>>() {
                }
        );
    }

    @Bean
    public List<Category> categories(ObjectMapper objectMapper) throws IOException {
        return readResource(
                objectMapper,
                OFFTOPIC_RESOURCE,
                new TypeReference<List<Category>>() {
                }
        );
    }

    private <T> T readResource(
            ObjectMapper objectMapper,
            String resourcePath,
            TypeReference<T> typeReference) throws IOException {

        ClassPathResource resource = new ClassPathResource(resourcePath);

        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, typeReference);
        }
    }
}

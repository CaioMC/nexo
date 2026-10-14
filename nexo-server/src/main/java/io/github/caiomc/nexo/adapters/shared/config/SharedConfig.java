package io.github.caiomc.nexo.adapters.shared.config;

import java.time.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.caiomc.nexo.adapters.shared.openai.OpenAiHttpClient;
import io.github.caiomc.nexo.adapters.shared.openai.OpenAiSettings;

@Configuration
public class SharedConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAiSettings openAiSettings(OpenAiProperties openAi) {
        return new OpenAiSettings(
                openAi.baseUrl(),
                openAi.apiKey(),
                openAi.model(),
                openAi.embeddingModel(),
                openAi.timeout()
        );
    }

    @Bean
    OpenAiHttpClient openAiHttpClient(
            OpenAiSettings settings,
            ObjectMapper json
    ) {
        return new OpenAiHttpClient(settings, json);
    }
}

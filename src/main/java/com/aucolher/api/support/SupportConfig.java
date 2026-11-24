package com.aucolher.api.support;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Liga as propriedades do Gemini (app.gemini.*), usadas pelo assistente de suporte. */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class SupportConfig {
}

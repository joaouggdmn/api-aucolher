package com.aucolher.api.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * A instrução de sistema do assistente: quem ele é, o que pode responder e as regras
 * do AUcolher que ele conhece. Fica em src/main/resources/support/assistente-prompt.md
 * e é lida uma vez, quando a API sobe.
 */
@Component
public class SupportKnowledge {

    static final String RESOURCE = "support/assistente-prompt.md";

    private final String systemInstruction;

    public SupportKnowledge() {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            this.systemInstruction = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Instrução do assistente não encontrada: " + RESOURCE, e);
        }
    }

    public String systemInstruction() {
        return systemInstruction;
    }
}

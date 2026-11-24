package com.aucolher.api.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.aucolher.api.common.ApiException;
import com.aucolher.api.shared.validation.Sanitizer;
import com.aucolher.api.support.GeminiClient.GeminiException;
import com.aucolher.api.support.GeminiClient.Turn;
import com.aucolher.api.support.dto.SupportMessageDTO;
import com.aucolher.api.support.dto.SupportReplyDTO;

/**
 * Assistente de suporte: valida a pergunta, aplica o limite de mensagens, monta a conversa
 * e pergunta ao Gemini com a instrução de sistema do AUcolher. Ele só responde: não lê nem
 * altera nada no banco, e a conversa não é gravada.
 */
@Service
public class SupportChatService {

    static final int MAX_MESSAGE_LENGTH = 500;
    /** Últimas falas do histórico que vão ao Gemini (10 trocas de pergunta e resposta). */
    static final int MAX_HISTORY_ITEMS = 20;
    static final int MAX_HISTORY_TEXT = 2_000;

    static final String EMPTY_MESSAGE = "Escreva uma pergunta.";
    static final String MESSAGE_TOO_LONG = "A pergunta pode ter até 500 caracteres.";
    static final String UNAVAILABLE = "O assistente está indisponível no momento. Tente de novo mais tarde.";
    static final String TOO_MANY = "Você enviou muitas mensagens em pouco tempo. Aguarde um pouco e tente de novo.";
    static final String NO_ANSWER = "Não consegui responder agora. Pode reformular a pergunta?";

    private final GeminiClient gemini;
    private final GeminiProperties props;
    private final SupportKnowledge knowledge;
    private final SupportRateLimiter limiter;

    public SupportChatService(GeminiClient gemini, GeminiProperties props, SupportKnowledge knowledge,
                              SupportRateLimiter limiter) {
        this.gemini = gemini;
        this.props = props;
        this.knowledge = knowledge;
        this.limiter = limiter;
    }

    /** clientKey identifica quem pergunta, para o limite: "user:{id}" ou "ip:{endereço}". */
    public SupportReplyDTO chat(String clientKey, String message, List<SupportMessageDTO> history) {
        String text = Sanitizer.text(message);
        if (text == null) {
            throw ApiException.badRequest(EMPTY_MESSAGE);
        }
        if (text.length() > MAX_MESSAGE_LENGTH) {
            throw ApiException.badRequest(MESSAGE_TOO_LONG);
        }
        if (!props.configured()) {
            throw ApiException.unavailable(UNAVAILABLE);
        }
        if (!limiter.tryAcquire(clientKey)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY);
        }

        List<Turn> turns = trimHistory(history);
        turns.add(new Turn("user", text));
        try {
            String reply = gemini.generate(knowledge.systemInstruction(), turns);
            return new SupportReplyDTO(reply == null ? NO_ANSWER : reply);
        } catch (GeminiException e) {
            throw ApiException.unavailable(UNAVAILABLE);
        }
    }

    /**
     * Só as últimas falas válidas (papel "user" ou "model", com texto), cada uma cortada no limite.
     * A conversa precisa começar por uma fala da pessoa, então respostas soltas no início saem.
     */
    static List<Turn> trimHistory(List<SupportMessageDTO> history) {
        List<Turn> valid = new ArrayList<>();
        if (history != null) {
            for (SupportMessageDTO item : history) {
                if (item == null) {
                    continue;
                }
                String text = Sanitizer.text(item.text());
                if (text == null || !("user".equals(item.role()) || "model".equals(item.role()))) {
                    continue;
                }
                if (text.length() > MAX_HISTORY_TEXT) {
                    text = text.substring(0, MAX_HISTORY_TEXT);
                }
                valid.add(new Turn(item.role(), text));
            }
        }
        List<Turn> recent = new ArrayList<>(valid.subList(Math.max(0, valid.size() - MAX_HISTORY_ITEMS), valid.size()));
        while (!recent.isEmpty() && "model".equals(recent.get(0).role())) {
            recent.remove(0);
        }
        return recent;
    }
}

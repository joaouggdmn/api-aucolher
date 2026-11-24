package com.aucolher.api.support;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aucolher.api.common.security.CurrentUserProvider;
import com.aucolher.api.support.dto.SupportChatRequestDTO;
import com.aucolher.api.support.dto.SupportReplyDTO;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Assistente de suporte (balão do site). Rota pública: visitante também pergunta.
 * O limite de mensagens conta por conta logada ou, sem login, pelo IP.
 */
@RestController
@RequestMapping("/api/support")
public class SupportChatController {

    private final SupportChatService service;
    private final CurrentUserProvider currentUser;

    public SupportChatController(SupportChatService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping("/chat")
    public SupportReplyDTO chat(@RequestBody(required = false) SupportChatRequestDTO body,
                                HttpServletRequest request) {
        String clientKey = currentUser.optionalUserId()
                .map(id -> "user:" + id)
                .orElseGet(() -> "ip:" + request.getRemoteAddr());
        return service.chat(clientKey, body == null ? null : body.message(), body == null ? null : body.history());
    }
}

package com.aucolher.api.campaign;

/** Ponte para a conta Mercado Pago da ONG (migração 009). Implementada pelo MercadoPagoOAuthService. */
public interface MercadoPagoAccountGateway {

    /** Frase do contrato quando a ONG não pode receber doações. */
    String NOT_RECEIVING_DONATIONS = "Esta ONG não está recebendo doações no momento.";

    /** true se a conta da ONG está CONNECTED. Não carrega nem descriptografa os tokens. */
    boolean isConnected(Long ngoId);

    /**
     * Access token da ONG (já descriptografado e renovado se preciso). Nunca sai do backend.
     * Conta ausente, NEEDS_RECONNECT ou token vencido: BusinessException com NOT_RECEIVING_DONATIONS.
     */
    String accessToken(Long ngoId);
}

package com.aucolher.api.campaign;

import com.aucolher.api.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PROVISÓRIO, até as doações usarem a conta de cada ONG: todas as ONGs usam um único
 * token de teste (app.mercadopago.dev-access-token). Para migrar, apague esta classe e
 * faça o MercadoPagoOAuthService (payment/) implementar MercadoPagoAccountGateway.
 */
@Component
public class PlaceholderMercadoPagoAccountGateway implements MercadoPagoAccountGateway {

    @Value("${app.mercadopago.dev-access-token:}")
    private String devToken;

    @Override
    public boolean isConnected(Long ngoId) {
        return true;
    }

    @Override
    public String accessToken(Long ngoId) {
        if (devToken == null || devToken.isBlank()) {
            throw new BusinessException("Esta ONG não está recebendo doações no momento.");
        }
        return devToken;
    }
}

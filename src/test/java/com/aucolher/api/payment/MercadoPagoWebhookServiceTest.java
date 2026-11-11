package com.aucolher.api.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.aucolher.api.common.ApiException;
import com.aucolher.api.common.DonationGateway;
import com.aucolher.api.common.DonationGateway.DonationRef;

/** O que o webhook faz com a order: aprova, ignora ou pede para o Mercado Pago repetir. */
class MercadoPagoWebhookServiceTest {

    private DonationGateway donations;
    private MercadoPagoOAuthService oauth;
    private MercadoPagoClient client;
    private MercadoPagoWebhookService service;

    @BeforeEach
    void setUp() {
        donations = mock(DonationGateway.class);
        oauth = mock(MercadoPagoOAuthService.class);
        client = mock(MercadoPagoClient.class);
        service = new MercadoPagoWebhookService(donations, oauth, client);
        when(donations.findByMpOrderId("ORD123")).thenReturn(Optional.of(new DonationRef(259L, 1L, "PENDING")));
        when(oauth.getValidAccessToken(1L)).thenReturn("token-da-ong");
    }

    @Test
    void paidOrderApprovesTheDonation() {
        when(client.getOrderState("token-da-ong", "ORD123")).thenReturn(MercadoPagoClient.OrderPaymentState.PAID);

        service.processOrderNotification("ORD123");

        verify(donations).approve(259L);
    }

    @Test
    void unknownOrderIsIgnored() {
        when(donations.findByMpOrderId("OUTRA")).thenReturn(Optional.empty());

        service.processOrderNotification("OUTRA");

        verify(client, never()).getOrderState(anyString(), anyString());
    }

    @Test
    void ngoWithoutTokenIsIgnored() {
        when(oauth.getValidAccessToken(1L)).thenThrow(ApiException.badRequest("Doações indisponíveis no momento."));

        service.processOrderNotification("ORD123");

        verifyNoInteractions(client);
    }

    @Test
    void revokedTokenMarksTheAccountForReconnection() {
        when(client.getOrderState("token-da-ong", "ORD123")).thenThrow(new MercadoPagoApiException(401, "revogado"));

        service.processOrderNotification("ORD123");

        verify(oauth).markNeedsReconnect(1L);
        verify(donations, never()).approve(anyLong());
    }

    @Test
    void badRequestIsNotRevokedAccess() {
        when(client.getOrderState("token-da-ong", "ORD123")).thenThrow(new MercadoPagoApiException(400, "mal formada"));

        service.processOrderNotification("ORD123");

        verify(oauth, never()).markNeedsReconnect(any());
    }

    @Test
    void temporaryFailureAsksMercadoPagoToRetry() {
        when(client.getOrderState("token-da-ong", "ORD123")).thenThrow(new MercadoPagoApiException(0, "rede"));

        ApiException e = assertThrows(ApiException.class, () -> service.processOrderNotification("ORD123"));
        assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        verify(oauth, never()).markNeedsReconnect(any());
    }
}

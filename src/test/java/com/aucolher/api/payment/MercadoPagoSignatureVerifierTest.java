package com.aucolher.api.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

/** Assinatura do webhook do Mercado Pago (api-mercado-pago-ongs §5.2). */
class MercadoPagoSignatureVerifierTest {

    static final String SECRET = "segredo-do-webhook";

    @Test
    void acceptsTheSignatureMadeWithTheSecretAndTheIdInLowercase() throws Exception {
        String v1 = hmac("id:ord01abc;request-id:req-1;ts:1742505638683;");

        assertTrue(MercadoPagoSignatureVerifier.isValid(SECRET, "ts=1742505638683,v1=" + v1, "req-1", "ORD01ABC"));
    }

    @Test
    void refusesATamperedSignature() throws Exception {
        String v1 = hmac("id:ord01abc;request-id:req-1;ts:1742505638683;");

        assertFalse(MercadoPagoSignatureVerifier.isValid(SECRET, "ts=1742505638683,v1=" + v1, "req-1", "ORD99"));
        assertFalse(MercadoPagoSignatureVerifier.isValid(SECRET, "ts=1742505638683,v1=" + v1, "req-2", "ORD01ABC"));
        assertFalse(MercadoPagoSignatureVerifier.isValid(SECRET, "ts=1,v1=" + v1, "req-1", "ORD01ABC"));
        assertFalse(MercadoPagoSignatureVerifier.isValid(SECRET, "ts=1742505638683", "req-1", "ORD01ABC"));
        assertFalse(MercadoPagoSignatureVerifier.isValid(SECRET, null, "req-1", "ORD01ABC"));
    }

    @Test
    void refusesEverythingWithoutASecret() throws Exception {
        String v1 = hmac("id:ord01abc;request-id:req-1;ts:1742505638683;");

        assertFalse(MercadoPagoSignatureVerifier.isValid("", "ts=1742505638683,v1=" + v1, "req-1", "ORD01ABC"));
        assertFalse(MercadoPagoSignatureVerifier.isValid("outro", "ts=1742505638683,v1=" + v1, "req-1", "ORD01ABC"));
    }

    @Test
    void partsMissingFromTheNotificationLeaveTheSignedText() throws Exception {
        assertEquals(hmac("request-id:req-1;ts:1;"),
                MercadoPagoSignatureVerifier.computeSignature(SECRET, null, "req-1", "1"));
        assertEquals(hmac("id:ord1;ts:1;"),
                MercadoPagoSignatureVerifier.computeSignature(SECRET, "ORD1", null, "1"));
    }

    @Test
    void idThatIsNotAlphanumericKeepsItsCase() throws Exception {
        assertEquals(hmac("id:ORD-1;request-id:req-1;ts:1;"),
                MercadoPagoSignatureVerifier.computeSignature(SECRET, "ORD-1", "req-1", "1"));
    }

    private static String hmac(String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }
}

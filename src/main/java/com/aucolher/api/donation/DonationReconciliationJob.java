package com.aucolher.api.donation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consulta periódica (B3 do plano do Mercado Pago): a cada minuto pergunta ao Mercado Pago pelos
 * PIX pendentes e aprova os pagos. Sem ela, quem fecha a janela do PIX antes de pagar ficaria com
 * a doação expirada, porque o polling do modal para e o webhook pode não chegar ou ser recusado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonationReconciliationJob {

    private final DonationService service;

    @Scheduled(initialDelayString = "${app.donations.reconcile-delay-ms:60000}",
            fixedDelayString = "${app.donations.reconcile-delay-ms:60000}")
    public void reconcilePending() {
        for (Long id : service.pendingToReconcile()) {
            try {
                service.reconcile(id);
            } catch (RuntimeException e) {
                // uma doação com problema não impede as outras
                log.warn("Consulta periódica: falha na doação {}: {}", id, e.getClass().getSimpleName());
            }
        }
    }
}

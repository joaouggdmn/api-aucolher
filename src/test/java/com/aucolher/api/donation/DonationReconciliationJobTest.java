package com.aucolher.api.donation;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Consulta periódica (B3): confere cada PIX pendente, e um com problema não trava os outros. */
class DonationReconciliationJobTest {

    @Test
    void oneFailingDonationDoesNotStopTheOthers() {
        DonationService service = mock(DonationService.class);
        when(service.pendingToReconcile()).thenReturn(List.of(10L, 12L));
        doThrow(new IllegalStateException("falhou")).when(service).reconcile(10L);

        new DonationReconciliationJob(service).reconcilePending();

        verify(service).reconcile(10L);
        verify(service).reconcile(12L);
    }
}

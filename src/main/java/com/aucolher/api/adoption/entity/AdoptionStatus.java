package com.aucolher.api.adoption.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status de um pedido de adoção:
 * PENDING → ACCEPTED → AWAITING_DELIVERY → CONCLUDED, ou REJECTED pelo dono,
 * ou CANCELLED (por uma das partes ou pelo sistema — o motivo fica em CancelReason).
 */
public enum AdoptionStatus {
    PENDING,
    ACCEPTED,
    AWAITING_DELIVERY,
    CONCLUDED,
    REJECTED,
    CANCELLED;

    /** Pedidos ainda em andamento: podem ser aceitos, entregues ou cancelados. */
    public static final Set<AdoptionStatus> ACTIVE = EnumSet.of(PENDING, ACCEPTED, AWAITING_DELIVERY);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}

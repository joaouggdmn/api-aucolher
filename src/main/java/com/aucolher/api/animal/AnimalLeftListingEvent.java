package com.aucolher.api.animal;

/**
 * O dono tirou o animal da vitrine por conta própria: marcou como adotado
 * fora da plataforma, tirou do ar (INACTIVE) ou excluiu o anúncio.
 *
 * Publicado dentro da transação da mudança de status — quem escuta (os
 * pedidos de adoção, que cancelam os ativos) roda junto e cai junto se der erro.
 */
public record AnimalLeftListingEvent(Long animalId) {
}

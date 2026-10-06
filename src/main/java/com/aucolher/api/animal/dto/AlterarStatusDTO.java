package com.aucolher.api.animal.dto;

import com.aucolher.api.animal.entity.StatusAnimal;
import jakarta.validation.constraints.NotNull;

/** Corpo do PATCH /api/animais/{id}/status — ex: { "status": "ADOTADO" }. */
public record AlterarStatusDTO(

        @NotNull(message = "Informe o novo status")
        StatusAnimal status
) {}

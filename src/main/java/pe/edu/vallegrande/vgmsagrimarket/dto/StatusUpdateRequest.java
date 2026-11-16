package pe.edu.vallegrande.vgmsagrimarket.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(
        @NotBlank(message = "status es obligatorio")
        String status
) {
}

package com.agrimarket.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(
        @NotBlank(message = "status es obligatorio")
        String status
) {
}

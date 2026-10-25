package com.agrimarket.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductPublicationRequest {

    @NotNull
    private Boolean isPublishedOnline;
}

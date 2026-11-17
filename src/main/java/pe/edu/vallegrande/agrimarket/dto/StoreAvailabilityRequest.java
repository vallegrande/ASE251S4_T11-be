package pe.edu.vallegrande.agrimarket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoreAvailabilityRequest {

    @NotNull
    private Boolean isActive;
}

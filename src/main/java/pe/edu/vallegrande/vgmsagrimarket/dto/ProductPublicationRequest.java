package pe.edu.vallegrande.vgmsagrimarket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductPublicationRequest {

    @NotNull
    private Boolean isPublishedOnline;
}

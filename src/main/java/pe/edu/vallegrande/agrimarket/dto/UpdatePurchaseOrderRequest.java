package pe.edu.vallegrande.agrimarket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class UpdatePurchaseOrderRequest {

    @NotNull(message = "purchaseOrderDate es obligatorio")
    private LocalDateTime purchaseOrderDate;

    @NotBlank(message = "status es obligatorio")
    private String status;

    private BigDecimal totalAmount;

    @NotBlank(message = "supplierId es obligatorio")
    private String supplierId;

    private Integer storeId;

    private String notes;

    @Valid
    @NotEmpty(message = "items no puede estar vacío")
    private List<PurchaseOrderItemDTO> items;
}

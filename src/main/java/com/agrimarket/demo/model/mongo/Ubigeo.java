package com.agrimarket.demo.model.mongo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ubigeos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubigeo {

    @Id
    @NotNull(message = "ubigeo.id es obligatorio")
    @Positive(message = "ubigeo.id debe ser positivo")
    private Long id;

    @NotBlank(message = "ubigeo.department es obligatorio")
    private String department;

    @NotBlank(message = "ubigeo.province es obligatorio")
    private String province;

    @NotBlank(message = "ubigeo.district es obligatorio")
    private String district;
}

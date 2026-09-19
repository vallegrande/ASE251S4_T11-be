package com.agrimarket.demo.model.mongo;

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
    private Long id;

    private String department;
    private String province;
    private String district;
}

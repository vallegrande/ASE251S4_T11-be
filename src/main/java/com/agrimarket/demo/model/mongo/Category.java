package com.agrimarket.demo.model.mongo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    private Long id;

    @NotBlank(message = "categoryName es obligatorio")
    @Size(max = 150, message = "categoryName no puede superar 150 caracteres")
    private String categoryName;

    private Long parentCategoryId;

    @Size(max = 500, message = "description no puede superar 500 caracteres")
    private String description;

    @NotNull(message = "level es obligatorio")
    @Positive(message = "level debe ser > 0")
    private Integer level;

    @NotBlank(message = "code es obligatorio")
    @Size(max = 20, message = "code no puede superar 20 caracteres")
    private String code;

    private Integer orderDisplay;

    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
}

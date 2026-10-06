package com.agrimarket.demo.model.sql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("store")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @Id
    @Column("store_id")
    private Integer storeId;

    @Column("store_name")
    private String storeName;

    @Column("address")
    private String address;

    @Column("phone")
    private String phone;

    @Column("ubigeo_id")
    private Integer ubigeoId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}

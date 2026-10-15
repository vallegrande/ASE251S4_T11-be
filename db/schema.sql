IF DB_ID(N'Agrimarket') IS NULL
BEGIN
    CREATE DATABASE Agrimarket;
END;
GO

USE Agrimarket;
GO

-- Script para una base Agrimarket nueva o vacía.
-- ----------------------------------------------------------------
-- 1. Tiendas. ubigeo_id referencia el catálogo MongoDB y por ello
--    no puede tener una FK SQL Server.
-- ----------------------------------------------------------------
CREATE TABLE dbo.store (
    store_id       INT IDENTITY(1,1) PRIMARY KEY,
    store_name     NVARCHAR(150) NOT NULL,
    address        NVARCHAR(255),
    phone          NVARCHAR(50),
    ubigeo_id      INT,
    is_active      BIT NOT NULL DEFAULT 1,
    created_at     DATETIME2 DEFAULT GETDATE(),
    updated_at     DATETIME2 DEFAULT GETDATE()
);

-- ----------------------------------------------------------------
-- 2. Inventario (qué productos hay en cada tienda + stock)
-- ----------------------------------------------------------------
CREATE TABLE dbo.inventory (
    inventory_id  INT IDENTITY(1,1) PRIMARY KEY,
    product_id    NVARCHAR(100) NOT NULL,
    store_id      INT NOT NULL,
    current_stock INT NOT NULL DEFAULT 0,
    minimum_stock INT NOT NULL DEFAULT 0,
    updated_at    DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT CK_inventory_current_stock CHECK (current_stock >= 0),
    CONSTRAINT CK_inventory_minimum_stock CHECK (minimum_stock >= 0),
    CONSTRAINT FK_inventory_store
        FOREIGN KEY (store_id) REFERENCES dbo.store (store_id)
);

CREATE UNIQUE INDEX UX_inventory_product_store
    ON dbo.inventory (product_id, store_id);

-- ----------------------------------------------------------------
-- 2. Pedido / Venta ("order" es palabra reservada)
-- ----------------------------------------------------------------
CREATE TABLE dbo.[order] (
    order_id         INT IDENTITY(1,1) PRIMARY KEY,
    order_date       DATETIME2 NOT NULL DEFAULT GETDATE(),
    status           NVARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    delivery_type    NVARCHAR(30) NOT NULL,
    delivery_address NVARCHAR(255),
    delivery_date    DATETIME2,
    total_amount     DECIMAL(18,2) NOT NULL DEFAULT 0,
    store_id         INT,
    customer_id      NVARCHAR(100) NOT NULL,
    checkout_key     NVARCHAR(100),
    created_at       DATETIME2 DEFAULT GETDATE(),
    updated_at       DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_order_store
        FOREIGN KEY (store_id) REFERENCES dbo.store (store_id),
    CONSTRAINT CK_order_delivery_type
        CHECK (delivery_type = N'recogida'),
    CONSTRAINT CK_order_status
        CHECK (status IN (
            N'PENDING',
            N'PENDING_PAYMENT',
            N'PAID',
            N'CANCELLED',
            N'PREPARING',
            N'READY_FOR_PICKUP',
            N'COMPLETED'
        ))
);

CREATE INDEX IX_order_customer ON dbo.[order] (customer_id);
CREATE INDEX IX_order_store    ON dbo.[order] (store_id);
CREATE UNIQUE INDEX UX_order_checkout_key
    ON dbo.[order] (checkout_key)
    WHERE checkout_key IS NOT NULL;

-- ----------------------------------------------------------------
-- 3. Pago simulado para el flujo académico (no procesa cobros reales)
-- ----------------------------------------------------------------
CREATE TABLE dbo.payment (
    payment_id     INT IDENTITY(1,1) PRIMARY KEY,
    order_id       INT NOT NULL,
    payment_method NVARCHAR(30) NOT NULL,
    payment_status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount         DECIMAL(18,2) NOT NULL,
    reference_code NVARCHAR(100) NOT NULL,
    cip_code       NVARCHAR(8),
    expires_at     DATETIME2 NOT NULL,
    created_at     DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at     DATETIME2 NOT NULL DEFAULT GETDATE(),
    paid_at        DATETIME2,
    CONSTRAINT FK_payment_order
        FOREIGN KEY (order_id) REFERENCES dbo.[order] (order_id),
    CONSTRAINT UQ_payment_order UNIQUE (order_id),
    CONSTRAINT CK_payment_method
        CHECK (payment_method IN (N'YAPE', N'TARJETA_CREDITO', N'PAGO_EFECTIVO')),
    CONSTRAINT CK_payment_status
        CHECK (payment_status IN (N'PENDING', N'PAID', N'EXPIRED')),
    CONSTRAINT CK_payment_amount CHECK (amount > 0)
);

CREATE UNIQUE INDEX UX_payment_cip_code
    ON dbo.payment (cip_code)
    WHERE cip_code IS NOT NULL;

CREATE INDEX IX_payment_expiration
    ON dbo.payment (payment_status, expires_at);

-- ----------------------------------------------------------------
-- 4. Detalle de Pedido / Venta
-- ----------------------------------------------------------------
CREATE TABLE dbo.order_detail (
    order_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    order_id        INT NOT NULL,
    product_id      NVARCHAR(100) NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_order_detail_order
        FOREIGN KEY (order_id) REFERENCES dbo.[order] (order_id)
        ON DELETE CASCADE,
    CONSTRAINT CK_order_detail_quantity CHECK (quantity > 0),
    CONSTRAINT CK_order_detail_unit_price CHECK (unit_price > 0)
);

CREATE INDEX IX_orderdetail_order   ON dbo.order_detail (order_id);
CREATE INDEX IX_orderdetail_product ON dbo.order_detail (product_id);

-- ----------------------------------------------------------------
-- 5. Orden de Compra a Proveedor
-- ----------------------------------------------------------------
CREATE TABLE dbo.purchase_order (
    purchase_order_id INT IDENTITY(1,1) PRIMARY KEY,
    order_date        DATETIME2 NOT NULL DEFAULT GETDATE(),
    status            NVARCHAR(30) NOT NULL DEFAULT 'PENDING',
    supplier_id       NVARCHAR(100) NOT NULL,
    store_id          INT,
    total_amount      DECIMAL(18,2) NOT NULL DEFAULT 0,
    notes             NVARCHAR(1000),
    created_at        DATETIME2 DEFAULT GETDATE(),
    updated_at        DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_purchase_order_store
        FOREIGN KEY (store_id) REFERENCES dbo.store (store_id),
    CONSTRAINT CK_purchase_order_status
        CHECK (status IN (N'PENDING', N'RECEIVED', N'CANCELLED'))
);

CREATE INDEX IX_po_supplier ON dbo.purchase_order (supplier_id);
CREATE INDEX IX_po_store    ON dbo.purchase_order (store_id);

-- ----------------------------------------------------------------
-- 6. Detalle de Orden de Compra
-- ----------------------------------------------------------------
CREATE TABLE dbo.purchase_order_detail (
    purchase_order_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    purchase_order_id        INT NOT NULL,
    product_id               NVARCHAR(100) NOT NULL,
    quantity                 INT NOT NULL,
    unit_cost                DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_pod_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES dbo.purchase_order (purchase_order_id)
        ON DELETE CASCADE,
    CONSTRAINT CK_purchase_order_detail_quantity CHECK (quantity > 0),
    CONSTRAINT CK_purchase_order_detail_unit_cost CHECK (unit_cost > 0)
);

CREATE INDEX IX_pod_po      ON dbo.purchase_order_detail (purchase_order_id);
CREATE INDEX IX_pod_product ON dbo.purchase_order_detail (product_id);
GO

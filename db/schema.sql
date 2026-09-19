USE Agrimarket;
GO

-- ----------------------------------------------------------------
-- 1. Tienda (todas las FKs la necesitan: order, app_user, etc.)
-- ----------------------------------------------------------------
CREATE TABLE store (
    store_id       INT IDENTITY(1,1) PRIMARY KEY,
    store_name     NVARCHAR(150) NOT NULL,
    address        NVARCHAR(255),
    phone          NVARCHAR(50),
    ubigeo_id      INT,
    created_at     DATETIME2 DEFAULT GETDATE(),
    updated_at     DATETIME2 DEFAULT GETDATE()
);

-- ----------------------------------------------------------------
-- 2. Inventario (qué productos hay en cada tienda + stock)
-- ----------------------------------------------------------------
CREATE TABLE inventory (
    inventory_id  INT IDENTITY(1,1) PRIMARY KEY,
    product_id    NVARCHAR(100) NOT NULL,
    store_id      INT NOT NULL,
    quantity      INT NOT NULL DEFAULT 0,
    unit_cost     DECIMAL(18,2) DEFAULT 0,
    updated_at    DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_inventory_store
        FOREIGN KEY (store_id) REFERENCES store (store_id)
);

CREATE INDEX IX_inventory_product_store
    ON inventory (product_id, store_id);

-- ----------------------------------------------------------------
-- 3. Usuarios del sistema (login)
-- ----------------------------------------------------------------
CREATE TABLE app_user (
    user_id        INT IDENTITY(1,1) PRIMARY KEY,
    username       NVARCHAR(50)  NOT NULL UNIQUE,
    email          NVARCHAR(255) NOT NULL UNIQUE,
    password_hash  NVARCHAR(255) NOT NULL,
    role           NVARCHAR(20)  NOT NULL,
    store_id       INT,
    is_active      BIT DEFAULT 1,
    created_at     DATETIME2 DEFAULT GETDATE(),
    updated_at     DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_app_user_store
        FOREIGN KEY (store_id) REFERENCES store (store_id)
);

-- ----------------------------------------------------------------
-- 4. Pedido / Venta (Brando)   ---- "order" requiere corchetes ---
-- ----------------------------------------------------------------
CREATE TABLE [order] (
    order_id         INT IDENTITY(1,1) PRIMARY KEY,
    order_date       DATETIME2 NOT NULL DEFAULT GETDATE(),
    status           NVARCHAR(30) NOT NULL DEFAULT 'PENDING',
    delivery_type    NVARCHAR(30) NOT NULL,
    delivery_address NVARCHAR(255),
    delivery_date    DATETIME2,
    total_amount     DECIMAL(18,2) NOT NULL DEFAULT 0,
    store_id         INT,
    customer_id      NVARCHAR(100) NOT NULL,
    created_at       DATETIME2 DEFAULT GETDATE(),
    updated_at       DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_order_store
        FOREIGN KEY (store_id) REFERENCES store (store_id)
);

CREATE INDEX IX_order_customer ON [order] (customer_id);
CREATE INDEX IX_order_store    ON [order] (store_id);

-- ----------------------------------------------------------------
-- 5. Detalle de Pedido / Venta
-- ----------------------------------------------------------------
CREATE TABLE order_detail (
    order_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    order_id        INT NOT NULL,
    product_id      NVARCHAR(100) NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_order_detail_order
        FOREIGN KEY (order_id) REFERENCES [order] (order_id)
        ON DELETE CASCADE
);

CREATE INDEX IX_orderdetail_order   ON order_detail (order_id);
CREATE INDEX IX_orderdetail_product ON order_detail (product_id);

-- ----------------------------------------------------------------
-- 6. Orden de Compra a Proveedor (PurchaseOrder - Gian)
-- ----------------------------------------------------------------
CREATE TABLE purchase_order (
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
        FOREIGN KEY (store_id) REFERENCES store (store_id)
);

CREATE INDEX IX_po_supplier ON purchase_order (supplier_id);
CREATE INDEX IX_po_store    ON purchase_order (store_id);

-- ----------------------------------------------------------------
-- 7. Detalle de Orden de Compra
-- ----------------------------------------------------------------
CREATE TABLE purchase_order_detail (
    purchase_order_detail_id INT IDENTITY(1,1) PRIMARY KEY,
    purchase_order_id        INT NOT NULL,
    product_id               NVARCHAR(100) NOT NULL,
    quantity                 INT NOT NULL,
    unit_cost                DECIMAL(18,2) NOT NULL,
    CONSTRAINT FK_pod_purchase_order
        FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (purchase_order_id)
        ON DELETE CASCADE
);

CREATE INDEX IX_pod_po      ON purchase_order_detail (purchase_order_id);
CREATE INDEX IX_pod_product ON purchase_order_detail (product_id);
GO

-- ================================================================
--   DATOS SEMILLA  (1 sola tienda con id = 1)
--   Cuando vuelvas a crear el contenedor de 0,
--   ejecutas todo el script del ARCHIVO y ya tienes store_id=1.
-- ================================================================

SET IDENTITY_INSERT store ON;

INSERT INTO store (store_id, store_name, address, phone, ubigeo_id)
VALUES (
    1,
    'AgriMarket - Tienda Principal Lima',
    'Av. Abancay 333, Cercado de Lima',
    '(01) 420-1234',
    NULL
);

SET IDENTITY_INSERT store OFF;
GO

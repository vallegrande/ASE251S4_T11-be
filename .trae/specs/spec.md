# Especificación: Migración a Persistencia Políglota (MongoDB + SQL Server R2DBC)

## Problema
El backend Agrimarket actualmente usa únicamente MongoDB reactivo para todas las entidades. Se requiere migrar a una arquitectura de persistencia políglota:
- **MongoDB**: para maestros/catálogo (Customer, Supplier, Product, Category, Brand, Ubigeo, Counter)
- **SQL Server (R2DBC reactivo)**: para entidades transaccionales (Order, OrderDetail, PurchaseOrder, PurchaseOrderDetail, Store, Inventory, AppUser)

Todo debe mantenerse 100% no bloqueante (Mono/Flux) para no romper la arquitectura WebFlux existente.

## Usuarios / Stakeholders
- Brando: Dueño del módulo de Order (ventas/pedidos)
- Gian: Dueño del módulo de PurchaseOrder (compras/órdenes de compra)
- Equipo de desarrollo: mantenedores del backend

## Objetivos
1. Reorganizar la estructura de carpetas para separar entidades Mongo vs SQL
2. Agregar soporte R2DBC para SQL Server manteniendo programación reactiva
3. Crear nuevos maestros en MongoDB (Product, Category, Brand) y completar Ubigeo
4. Reducir prioridad de Supplier (mantener solo findById/findAll, quitar controller)
5. Implementar Order + OrderDetail transaccional en SQL Server (R2DBC)
6. Implementar PurchaseOrder + PurchaseOrderDetail transaccional en SQL Server (R2DBC)
7. No romper el funcionamiento existente de Customer

## No Objetivos
- No se elimina el modelo Supplier ni sus datos en MongoDB
- No se toca CorsConfig, GlobalExceptionHandler ni MongoConfig (solo imports si es necesario)
- No se implementa lógica de negocio específica de Gian para PurchaseOrder (solo infraestructura R2DBC)
- No se usa JPA/Hibernate, solo R2DBC puro
- No se crean archivos de documentación .md adicionales

## Requisitos Funcionales

### RF1: Reorganización de Carpetas
- Mover clases modelo Mongo existentes: Customer.java, Supplier.java, Ubigeo.java, Counter.java a `model/mongo/`
- Mover repositorios Mongo existentes: CustomerRepository.java, SupplierRepository.java a `repository/mongo/`
- Crear `model/sql/` y `repository/sql/` para entidades relacionales
- Actualizar imports en service, rest y config afectados por el movimiento

### RF2: Dependencias R2DBC
- Agregar `spring-boot-starter-data-r2dbc` en pom.xml
- Agregar `io.r2dbc:r2dbc-mssql` (scope runtime) en pom.xml

### RF3: Configuración SQL Server
- En application.yaml, agregar configuración `spring.r2dbc` con:
  - url: `r2dbc:mssql://${SQLSERVER_HOST:localhost}:${SQLSERVER_PORT:1433}/${SQLSERVER_DATABASE:Agrimarket}?encrypt=false&trustServerCertificate=true`
  - username: `${SQLSERVER_USER:sa}`
  - password: `${SQLSERVER_PASSWORD:Sql12345678!}`
- Crear `config/R2dbcConfig.java` con bean `TransactionalOperator` para transacciones atómicas cabecera+detalle
- Crear script SQL con tablas: store, inventory, app_user, [order], order_detail, purchase_order, purchase_order_detail

### RF4: Nuevos Maestros en MongoDB (CRUD reactivo completo)
**Product**:
- Campos: name, activeIngredient, presentation, basePrice, imageUrl, description, sku, categoryId, brandId, isActive, metadata (JSON libre), createdAt, updatedAt, deletedAt, restoredAt
- Patrón: igual que Customer.java (soft delete con isActive/deletedAt/restoredAt, generación de ID por secuencia)
- Endpoints: GET /api/products, GET /api/products/{id}, GET /api/products/status/{isActive}, POST /api/products, PUT /api/products/{id}, PATCH /api/products/{id}/delete, PATCH /api/products/{id}/restore

**Category**:
- Campos: categoryName, parentCategoryId, description, level, code, orderDisplay, isActive, createdAt, updatedAt, deletedAt, restoredAt
- Endpoints: mismo patrón que Product bajo /api/categories

**Brand**:
- Campos: brandName, isActive, createdAt, updatedAt, deletedAt, restoredAt
- Endpoints: mismo patrón que Product bajo /api/brands

**Ubigeo** (ya existe el modelo, falta infraestructura):
- Campos: department, province, district
- Como no tiene ID propio y es un record, se definirá como documento embebido. Crear UbigeoRepository (para consulta masiva si se requiere más adelante), UbigeoService, UbigeoController con listado y búsqueda por department/province/district.

### RF5: Baja de Prioridad de Supplier
- Eliminar SupplierController.java
- Mantener Supplier.java, SupplierRepository.java (ya movidos en RF1)
- Mantener SupplierService mínimo: solo findById y findAll (eliminar create, update, logicalDelete, restore, findByStatus)
- Actualizar SupplierServiceImpl acorde
- No eliminar datos de la colección supplier en MongoDB

### RF6: Order + OrderDetail (SQL Server R2DBC - Brando)
**Entidad Order (@Table("order"))**:
- order_id (PK, identity/auto-incremental int)
- order_date (LocalDateTime)
- status (String)
- delivery_type (String)
- delivery_address (String)
- delivery_date (LocalDateTime)
- total_amount (BigDecimal)
- store_id (int, FK a store.store_id)
- customer_id (String, referencia lógica a MongoDB - no FK real)
- created_at (LocalDateTime)
- updated_at (LocalDateTime)

**Entidad OrderDetail (@Table("order_detail"))**:
- order_detail_id (PK, identity/auto-incremental int)
- quantity (int)
- unit_price (BigDecimal)
- product_id (String, referencia lógica a MongoDB - no FK real)
- order_id (int, FK real a [order].order_id)

**OrderRepository** y **OrderDetailRepository** (ReactiveCrudRepository R2DBC)

**OrderService / OrderServiceImpl**:
- listarTodosConDetalle(): Flux<OrderWithDetails> - cada pedido con su lista de detalles
- obtenerPorIdConDetalle(Integer id): Mono<OrderWithDetails>
- crearPedido(CreateOrderRequest dto): Mono<OrderWithDetails>
  - Valida customer_id exista en MongoDB (customerRepository.existsById)
  - Inserta cabecera (Order)
  - Inserta cada detalle con el order_id generado
  - Todo envuelto en TransactionalOperator para atomicidad
- editarPedido(Integer id, UpdateOrderRequest dto): Mono<OrderWithDetails>
  - Actualiza campos de cabecera
  - Elimina detalles anteriores y re-inserta los nuevos (reemplazo completo del detalle)
  - Transaccional

**OrderController**:
- GET /api/orders → listar todos con detalle
- GET /api/orders/{id} → obtener por id con detalle
- POST /api/orders → crear pedido
- PUT /api/orders/{id} → editar pedido

Nota: Se requieren DTOs para entrada (CreateOrderRequest, UpdateOrderRequest, OrderItemDTO) y salida (OrderWithDetailsDTO).

### RF7: PurchaseOrder + PurchaseOrderDetail (SQL Server R2DBC - Gian)
Mismo patrón que RF6, pero para compras:

**Entidad PurchaseOrder (@Table("purchase_order"))**:
- purchase_order_id (PK, identity/auto-incremental int)
- purchase_order_date (LocalDateTime)
- status (String)
- total_amount (BigDecimal)
- supplier_id (String, referencia lógica a MongoDB)
- store_id (int, FK a store.store_id)
- notes (String)
- created_at (LocalDateTime)
- updated_at (LocalDateTime)

**Entidad PurchaseOrderDetail (@Table("purchase_order_detail"))**:
- purchase_order_detail_id (PK, identity/auto-incremental int)
- quantity (int)
- unit_price (BigDecimal)
- product_id (String, referencia lógica a MongoDB)
- purchase_order_id (int, FK real a purchase_order.purchase_order_id)

**PurchaseOrderRepository** y **PurchaseOrderDetailRepository**

**PurchaseOrderService / PurchaseOrderServiceImpl**:
- listarTodosConDetalle()
- obtenerPorIdConDetalle(Integer id)
- crearPurchaseOrder(CreatePurchaseOrderRequest dto)
  - Valida supplier_id exista en MongoDB (supplierRepository.existsById o findById)
  - Transaccional cabecera + detalle
- editarPurchaseOrder(Integer id, UpdatePurchaseOrderRequest dto)
  - Transaccional

**PurchaseOrderController**:
- GET /api/purchase-orders
- GET /api/purchase-orders/{id}
- POST /api/purchase-orders
- PUT /api/purchase-orders/{id}

### RF8: No Romper lo Existente
- Customer (Mongo) sigue funcionando exactamente igual: mismos endpoints, misma lógica, mismos campos
- Solo se modifican imports si las clases se movieron de carpeta

## Requisitos No Funcionales

### RNF1: 100% Reactivo
- Todas las operaciones DB devuelven Mono/Flux
- No se usa `.block()` en ningún lado
- No se usa JPA/Hibernate ni JDBC bloqueante

### RNF2: Atomicidad Transaccional
- Creación y edición de Order y PurchaseOrder usan TransactionalOperator
- Si falla la inserción de cualquier detalle, hace rollback completo

### RNF3: Compilación
- `mvn clean compile` debe pasar sin errores

### RNF4: Convenciones de Código
- Seguir el estilo existente: Lombok (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor, @RequiredArgsConstructor)
- Inyectar dependencias por constructor (usando @RequiredArgsConstructor)
- Manejo de excepciones con ResourceNotFoundException existente
- Soft delete siguiendo el patrón Customer (isActive + deletedAt + restoredAt)
- Generación de IDs de secuencia para entidades Mongo usando SequenceGeneratorService existente

## Restricciones / Dependencias
- Spring Boot 4.1.1 + Java 21 (no cambiar versiones)
- SQL Server local en Docker: localhost:1433, usuario sa, password Sql12345678!, BD Agrimarket
- MongoDB reactivo permanece como está (MONGODB_URI por variable de entorno)
- Supplier no se borra de MongoDB, solo se quita su controller y CRUD completo
- No crear tablas de Mongo en SQL ni viceversa

## Supuestos
- IDs de entidades SQL son INT auto-incrementales (identity)
- Referencias lógicas a MongoDB (customer_id, product_id, supplier_id en tablas SQL) se almacenan como String/varchar (porque Mongo usa ObjectId o Long como String)
- store_id es FK real a store (tabla SQL), aunque no se implemente el CRUD de Store en esta fase
- El script SQL con definiciones exactas de tablas (tipos, constraints, índices) puede requerir ajustes si Gian/Brando proporcionan sus definiciones oficiales del semestre pasado

## Preguntas Abiertas
1. **Definiciones exactas de tablas SQL**: ¿Puedes proporcionar las definiciones exactas (tipos de datos, constraints, longitudes de varchar) de tu script SQL Server del semestre pasado para: store, inventory, app_user, [order], order_detail, purchase_order, purchase_order_detail? Actualmente crearé definiciones razonables basadas en los campos mencionados, pero pueden requerir ajuste posterior.
2. **¿Qué hacer con UbigeoRepository?** Al ser un record sin @Id propio, ¿debe ser un documento embebido únicamente (sin repositorio/collection propia) o quieres una colección ubigeos separada? Por defecto asumiré colección separada con un id compuesto o secuencial.

## Criterios de Aceptación

### Rule: RF1 - Reorganización completada
- Las 4 clases modelo Mongo están en `model/mongo/`
- Los 2 repositorios Mongo están en `repository/mongo/`
- Existen carpetas `model/sql/` y `repository/sql/` vacías o con contenido
- Todos los imports en service/, rest/, config/ actualizados correctamente
- Evidencia: `mvn clean compile` pasa

### Rule: RF2 - Dependencias R2DBC agregadas
- pom.xml contiene `spring-boot-starter-data-r2dbc`
- pom.xml contiene `io.r2dbc:r2dbc-mssql` con scope runtime
- Evidencia: revisión de pom.xml + `mvn dependency:tree` muestra ambas dependencias

### Rule: RF3 - Configuración SQL Server lista
- application.yaml tiene bloque `spring.r2dbc` con url/username/password y placeholders correctos
- Existe R2dbcConfig.java con bean TransactionalOperator definido
- Existe script SQL (schema.sql o carpeta /db) con CREATE TABLE para las 7 tablas requeridas
- Evidencia: revisión de archivos

### Rule: RF4 - Nuevos maestros Mongo con CRUD completo
- Product: Model + Repository + Service(interface/impl) + Controller existen y siguen patrón Customer
- Category: Model + Repository + Service(interface/impl) + Controller existen y siguen patrón Customer
- Brand: Model + Repository + Service(interface/impl) + Controller existen y siguen patrón Customer
- Ubigeo: Repository + Service + Controller existen con operaciones de listado/búsqueda
- Evidencia: archivos existen, compilación pasa, endpoints REST declarados

### Rule: RF5 - Supplier reducido correctamente
- SupplierController.java eliminado
- SupplierService.java solo declara findById y findAll
- SupplierServiceImpl.java solo implementa findById y findAll
- Supplier.java y SupplierRepository.java intactos (salvo movimiento de carpeta)
- Evidencia: archivos revisados

### Rule: RF6 - Order + OrderDetail R2DBC funcionales
- Order.java (@Table("order")) y OrderDetail.java (@Table("order_detail")) existen en model/sql/
- OrderRepository y OrderDetailRepository extienden ReactiveCrudRepository en repository/sql/
- OrderService/OrderServiceImpl tienen los 4 métodos solicitados
- CrearPedido valida customer_id en Mongo y usa TransactionalOperator
- EditarPedido actualiza cabecera + reemplaza detalle en transacción
- OrderController tiene los 4 endpoints solicitados
- DTOs existen para request/response
- Evidencia: archivos existen, compilación pasa, TransactionalOperator se usa en create/edit

### Rule: RF7 - PurchaseOrder + PurchaseOrderDetail R2DBC funcionales
- PurchaseOrder.java y PurchaseOrderDetail.java existen en model/sql/
- PurchaseOrderRepository y PurchaseOrderDetailRepository existen en repository/sql/
- PurchaseOrderService/Impl tienen 4 métodos (listar, obtener, crear, editar)
- CrearPurchaseOrder valida supplier_id en Mongo y usa TransactionalOperator
- PurchaseOrderController tiene 4 endpoints
- DTOs existen para request/response
- Evidencia: archivos existen, compilación pasa

### Rule: RF8 - Customer intacto
- CustomerController, CustomerService, CustomerServiceImpl tienen la misma lógica que antes
- Mismos endpoints, mismas firmas, mismo comportamiento
- Solo cambian imports por reorganización de carpetas
- Evidencia: diff de lógica muestra solo cambios de import

### Rubric: RNF1 - Calidad de programación reactiva
- Escala 0-2
- 0: Se detecta `.block()` o llamadas bloqueantes
- 1: Todo Mono/Flux pero con conversiones innecesarias o flatMap anidados excesivos
- 2: Toda la cadena reactiva es idiomática, sin bloqueos, compose adecuado
- Umbral de aprobación: >=1
- Evidencia: revisión de código fuente service/impl y repository

### Rubric: RNF2 - Atomicidad transaccional
- Escala 0-2
- 0: No se usa TransactionalOperator en create/edit de Order ni PurchaseOrder
- 1: Se usa TransactionalOperator en uno de los dos módulos (Order o PurchaseOrder)
- 2: Se usa TransactionalOperator en ambos módulos, create y edit
- Umbral de aprobación: >=1
- Evidencia: revisión de OrderServiceImpl.create/edit y PurchaseOrderServiceImpl.create/edit

### Rule: RNF3 - Compilación exitosa
- `mvn clean compile` exit code 0
- Evidencia: salida del comando

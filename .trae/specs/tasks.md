# Plan de Implementación: Migración a Persistencia Políglota

## Mapa de ACs a Tareas

| AC | Tareas que la cubren |
|---|---|
| RF1 Reorganización | T1, T2 |
| RF2 Dependencias R2DBC | T3 |
| RF3 Config SQL Server | T4, T5, T6 |
| RF4 Nuevos maestros Mongo | T7, T8, T9, T10 |
| RF5 Supplier reducido | T11 |
| RF6 Order + OrderDetail | T12, T13, T14, T15, T16 |
| RF7 PurchaseOrder + PO-Detail | T17, T18, T19, T20, T21 |
| RF8 Customer intacto | T1 (verificación) |
| RNF1 Reactivo | Todas las tareas (TR implícito) |
| RNF2 Atomicidad | T15, T20 |
| RNF3 Compilación | T22 |

---

## Task 1: Reorganizar carpetas - Mover modelos Mongo a model/mongo/

**Prioridad**: high
**Depende de**: ninguna
**Status**: completed

### Descripción
Crear carpeta `model/mongo/` y mover:
- Customer.java
- Supplier.java
- Ubigeo.java
- Counter.java

Actualizar el `package` declaration de cada archivo y actualizar imports en:
- service/* (CustomerService/Impl, SupplierService/Impl, SequenceGeneratorService)
- repository/* (CustomerRepository, SupplierRepository) - aunque también se mueven en T2
- rest/* (CustomerController, SupplierController)
- config/MongoConfig.java no requiere cambio (no importa modelos directamente)
- Asegurarse que Customer.java siga importando Ubigeo correctamente (ahora es el mismo paquete)

### Test Requirements
- **TR1 (rule)**: Los 4 archivos existen en `src/main/java/com/agrimarket/demo/model/mongo/` con package `com.agrimarket.demo.model.mongo`
- **TR2 (rule)**: CustomerController.java compila (importa Customer desde el nuevo paquete)
- **TR3 (rule)**: CustomerServiceImpl.java compila (importa Customer y CustomerRepository correctamente)

### Evidencia de finalización
Archivos movidos, packages actualizados, imports corregidos.

---

## Task 2: Reorganizar carpetas - Mover repositorios Mongo a repository/mongo/

**Prioridad**: high
**Depende de**: T1 (por los imports de modelos)
**Status**: completed

### Descripción
Crear carpeta `repository/mongo/` y mover:
- CustomerRepository.java
- SupplierRepository.java

Actualizar `package` y los imports de los modelos (ahora vienen de `com.agrimarket.demo.model.mongo`).
Actualizar imports en:
- service/* (CustomerServiceImpl, SupplierServiceImpl)
- Configs (si alguno importa repositorios directamente, que no es el caso)
- Crear `repository/sql/` (vacío por ahora) y `model/sql/` (vacío por ahora)

### Test Requirements
- **TR1 (rule)**: Los 2 repositorios existen en `repository/mongo/` con package correcto
- **TR2 (rule)**: Existen las carpetas vacías `repository/sql/` y `model/sql/`
- **TR3 (rule)**: CustomerServiceImpl tiene imports válidos a repository/mongo

### Evidencia de finalización
Archivos movidos, carpetas sql creadas, imports corregidos.

---

## Task 3: Agregar dependencias R2DBC en pom.xml

**Prioridad**: high
**Depende de**: ninguna
**Status**: completed

### Descripción
Agregar 2 dependencias en pom.xml dentro de `<dependencies>`:
1. `spring-boot-starter-data-r2dbc` (sin scope, compile)
2. `io.r2dbc:r2dbc-mssql` con `<scope>runtime</scope>`

No se eliminan dependencias existentes.

### Test Requirements
- **TR1 (rule)**: pom.xml contiene `<artifactId>spring-boot-starter-data-r2dbc</artifactId>`
- **TR2 (rule)**: pom.xml contiene `<artifactId>r2dbc-mssql</artifactId>` con `<scope>runtime</scope>`
- **TR3 (rule)**: `mvn dependency:resolve` termina con éxito (prueba rápida)

### Evidencia de finalización
pom.xml editado, dependencias descargables.

---

## Task 4: Configurar R2DBC en application.yaml

**Prioridad**: high
**Depende de**: ninguna
**Status**: completed

### Descripción
En `application.yaml`, agregar bloque `spring.r2dbc` con:
```yaml
spring:
  r2dbc:
    url: r2dbc:mssql://${SQLSERVER_HOST:localhost}:${SQLSERVER_PORT:1433}/${SQLSERVER_DATABASE:Agrimarket}?encrypt=false&trustServerCertificate=true
    username: ${SQLSERVER_USER:sa}
    password: ${SQLSERVER_PASSWORD:Sql12345678!}
```

No tocar los bloques `data.mongodb` existentes ni `autoconfigure.exclude`.

### Test Requirements
- **TR1 (rule)**: application.yaml tiene la propiedad `spring.r2dbc.url` con el patrón exacto (placeholders + query params)
- **TR2 (rule)**: application.yaml tiene `spring.r2dbc.username` y `spring.r2dbc.password` con los placeholders correctos
- **TR3 (rule)**: No se eliminó/modificó `spring.data.mongodb` ni `spring.autoconfigure.exclude`

### Evidencia de finalización
application.yaml actualizado.

---

## Task 5: Crear R2dbcConfig.java con TransactionalOperator

**Prioridad**: high
**Depende de**: T3 (las clases R2DBC deben estar en classpath)
**Status**: completed

### Descripción
Crear `config/R2dbcConfig.java` con:
- `@Configuration`
- Bean `R2dbcTransactionManager` que reciba `ConnectionFactory` (inyectado por Spring Boot desde application.yaml)
- Bean `TransactionalOperator` que use el R2dbcTransactionManager

Nota: El nombre `transactionalOperator` es importante para luego inyectarlo en los servicios. No modificar MongoConfig.java.

### Test Requirements
- **TR1 (rule)**: R2dbcConfig.java existe con `@Configuration`
- **TR2 (rule)**: Declara un bean `R2dbcTransactionManager`
- **TR3 (rule)**: Declara un bean `TransactionalOperator` (return type `TransactionalOperator` de `org.springframework.r2dbc.connection`)

### Evidencia de finalización
Archivo creado, compila.

---

## Task 6: Crear script SQL con tablas para SQL Server

**Prioridad**: high
**Depende de**: ninguna
**Status**: completed

### Descripción
Crear carpeta `db/` en la raíz del proyecto (o `src/main/resources/db/`) y dentro `schema.sql` con las siguientes tablas (usar definiciones razonables si no se proporcionan las oficiales):

1. **store** (store_id INT PK identity, store_name VARCHAR, address VARCHAR, phone VARCHAR, ubigeo_id INT?, created_at DATETIME, updated_at DATETIME)
2. **inventory** (inventory_id INT PK identity, product_id VARCHAR, store_id INT FK, quantity INT, unit_cost DECIMAL, updated_at DATETIME)
3. **app_user** (user_id INT PK identity, username VARCHAR UNIQUE, password_hash VARCHAR, email VARCHAR, customer_id VARCHAR, role VARCHAR, is_active BIT, created_at DATETIME)
4. **[order]** (order_id INT PK identity, order_date DATETIME, status VARCHAR, delivery_type VARCHAR, delivery_address VARCHAR, delivery_date DATETIME, total_amount DECIMAL(18,2), store_id INT FK, customer_id VARCHAR, created_at DATETIME, updated_at DATETIME)
5. **order_detail** (order_detail_id INT PK identity, quantity INT, unit_price DECIMAL(18,2), product_id VARCHAR, order_id INT FK a [order])
6. **purchase_order** (purchase_order_id INT PK identity, purchase_order_date DATETIME, status VARCHAR, total_amount DECIMAL(18,2), supplier_id VARCHAR, store_id INT FK, notes VARCHAR, created_at DATETIME, updated_at DATETIME)
7. **purchase_order_detail** (purchase_order_detail_id INT PK identity, quantity INT, unit_price DECIMAL(18,2), product_id VARCHAR, purchase_order_id INT FK a purchase_order)

Usar `IF NOT EXISTS` para crear tablas de forma idempotente. Agregar FK constraints. Usar corchetes `[order]` porque ORDER es palabra reservada.

### Test Requirements
- **TR1 (rule)**: Existe `schema.sql` con sentencias CREATE TABLE para las 7 tablas
- **TR2 (rule)**: La tabla `[order]` usa corchetes en el nombre
- **TR3 (rule)**: order_detail.order_id tiene FK a [order].order_id
- **TR4 (rule)**: purchase_order_detail.purchase_order_id tiene FK a purchase_order.purchase_order_id
- **TR5 (rule)**: customer_id, product_id, supplier_id en tablas SQL son VARCHAR (referencias lógicas)

### Evidencia de finalización
Archivo schema.sql creado con todas las tablas.

---

## Task 7: Nuevos maestros Mongo - Product (Model + Repository + Service + Controller)

**Prioridad**: high
**Depende de**: T1, T2 (estructura de carpetas lista)
**Status**: completed

### Descripción
Crear CRUD reactivo completo para Product siguiendo el patrón de Customer:

**Model** (model/mongo/Product.java):
- @Document(collection = "products")
- Campos: @Id Long id, name, activeIngredient, presentation, basePrice (BigDecimal), imageUrl, description, sku, categoryId (Long/String), brandId (Long/String), isActive (Boolean default true), metadata (org.bson.Document o Map<String, Object>), createdAt, updatedAt, deletedAt, restoredAt
- Lombok: @Data @Builder @NoArgsConstructor @AllArgsConstructor

**Repository** (repository/mongo/ProductRepository.java):
- Extiende ReactiveMongoRepository<Product, Long>
- Método: Flux<Product> findByIsActive(Boolean isActive)

**Service Interface** (service/ProductService.java):
- findAll, findByStatus(Boolean), findById(Long), create(Product), update(Long, Product), logicalDelete(Long), restore(Long)

**ServiceImpl** (service/ProductServiceImpl.java):
- Usa SequenceGeneratorService.generateSequence("products_sequence")
- Mismo patrón de timestamps que CustomerServiceImpl
- Usa ResourceNotFoundException

**Controller** (rest/ProductController.java):
- @RequestMapping("/api/products")
- GET /, GET /status/{isActive}, GET /{id}, POST /, PUT /{id}, PATCH /{id}/delete, PATCH /{id}/restore
- Mismo patrón que CustomerController

### Test Requirements
- **TR1 (rule)**: Los 5 archivos existen (Model, Repository, Service, ServiceImpl, Controller)
- **TR2 (rule)**: Product.java tiene todos los campos requeridos incluyendo metadata, categoryId, brandId
- **TR3 (rule)**: ProductServiceImpl usa SequenceGeneratorService con "products_sequence"
- **TR4 (rule)**: Controller tiene los 7 endpoints requeridos
- **TR5 (rubric)**: Fidelidad al patrón Customer (escala 0-2):
  - 2: Identica estructura de métodos, manejo de timestamps, soft delete
  - 1: Estructura similar pero con diferencias menores
  - 0: Estructura muy distinta
  Umbral: >=1

### Evidencia de finalización
5 archivos creados, compilan.

---

## Task 8: Nuevos maestros Mongo - Category (Model + Repository + Service + Controller)

**Prioridad**: high
**Depende de**: T1, T2
**Status**: completed

### Descripción
Crear CRUD para Category siguiendo el mismo patrón:

**Model** (model/mongo/Category.java):
- @Document(collection = "categories")
- @Id Long id, categoryName, parentCategoryId (Long), description, level (Integer), code, orderDisplay (Integer), isActive, createdAt, updatedAt, deletedAt, restoredAt
- Secuencia: "categories_sequence"
- Controller: /api/categories

### Test Requirements
- **TR1 (rule)**: 5 archivos existen
- **TR2 (rule)**: Category.java tiene parentCategoryId, level, code, orderDisplay
- **TR3 (rule)**: Controller en /api/categories con los 7 endpoints

### Evidencia de finalización
Archivos creados.

---

## Task 9: Nuevos maestros Mongo - Brand (Model + Repository + Service + Controller)

**Prioridad**: high
**Depende de**: T1, T2
**Status**: completed

### Descripción
Crear CRUD para Brand siguiendo el patrón (más simple, solo brandName):

**Model** (model/mongo/Brand.java):
- @Document(collection = "brands")
- @Id Long id, brandName, isActive, createdAt, updatedAt, deletedAt, restoredAt
- Secuencia: "brands_sequence"
- Controller: /api/brands

### Test Requirements
- **TR1 (rule)**: 5 archivos existen
- **TR2 (rule)**: Brand.java tiene al menos brandName, isActive, timestamps y soft delete
- **TR3 (rule)**: Controller en /api/brands

### Evidencia de finalización
Archivos creados.

---

## Task 10: Completar Ubigeo (Repository + Service + Controller)

**Prioridad**: medium
**Depende de**: T1, T2
**Status**: completed

### Descripción
Ubigeo ya existe como record en model/mongo/Ubigeo.java. Como no tiene @Id, se requiere una decisión (Supuesto: colección separada con id secuencial).

**Opción elegida (supuesto)**: Crear un wrapper/document UbigeoDocument.java si es necesario, o convertir el record a una clase normal con @Id. Para simplicidad, convertir Ubigeo de record a clase con @Id Long id y los 3 campos (department, province, district). O mantener el record y crear un UbigeoRepository sin ReactiveMongoRepository (solo ReactiveMongoTemplate). 

Alternativa más simple y alineada con el supuesto del spec: convertir Ubigeo a clase normal con @Id (igual que Counter) y darle CRUD mínimo (listar, buscar por id, buscar por departamento). Por ser "maestro de catálogo simple", el CRUD será: listar todos, buscar por id, buscar por department.

**Cambios**:
- Convertir Ubigeo.java a clase normal (no record) con @Id Long id, department, province, district. Agregar Lombok, @Document(collection="ubigeos")
- Repository (repository/mongo/UbigeoRepository.java): ReactiveMongoRepository<Ubigeo, Long> + Flux<Ubigeo> findByDepartment(String) + findByDepartmentAndProvince(String, String)
- Service: findAll(), findById(Long), findByDepartment(String)
- Controller: /api/ubigeos con GET /, GET /{id}, GET /department/{department}
- No se requiere soft delete para Ubigeo (catálogo maestro generalmente no se borra)
- Secuencia: "ubigeos_sequence" en create (si se agrega POST crear). Por minimalismo: solo listar y buscar.

Alternativa más fiel al "record" existente: Si no se quiere modificar Ubigeo, simplemente crear un UbigeoService que devuelva Flux.empty() por ahora, y un Controller dummy. Pero el usuario dijo "falta repository/service/controller", así que asumimos CRUD mínimo.

### Test Requirements
- **TR1 (rule)**: UbigeoRepository.java existe en repository/mongo/
- **TR2 (rule)**: UbigeoService / UbigeoServiceImpl existen con findAll y findById
- **TR3 (rule)**: UbigeoController existe en /api/ubigeos con al menos 2 endpoints GET

### Evidencia de finalización
Archivos creados.

---

## Task 11: Reducir Supplier (eliminar controller, reducir service)

**Prioridad**: medium
**Depende de**: T1, T2 (movimientos ya hechos)
**Status**: completed

### Descripción
1. Eliminar `rest/SupplierController.java` completamente (delete file)
2. Modificar `service/SupplierService.java`: Eliminar todos los métodos excepto `findAll()` y `findById(Long id)`.
3. Modificar `service/SupplierServiceImpl.java`: Eliminar implementación de create/update/delete/restore/findByStatus. Solo quedan findAll() (delega a repository) y findById() (delega a repository). Eliminar la inyección de SequenceGeneratorService si ya no se usa en el impl.
4. Mantener Supplier.java y SupplierRepository.java intactos.

### Test Requirements
- **TR1 (rule)**: SupplierController.java no existe en rest/
- **TR2 (rule)**: SupplierService.java declara solo 2 métodos: findAll y findById
- **TR3 (rule)**: SupplierServiceImpl.java implementa solo los 2 métodos y NO inyecta SequenceGeneratorService
- **TR4 (rule)**: Supplier.java y SupplierRepository.java siguen existiendo (verificado por existencia)

### Evidencia de finalización
Archivo eliminado, servicios reducidos.

---

## Task 12: Crear entidades SQL - Order.java y OrderDetail.java

**Prioridad**: high
**Depende de**: T2 (carpeta model/sql/ existe)
**Status**: completed

### Descripción
Crear dos clases en `model/sql/`:

**Order.java** (@Table("order") - corchetes no van en annotation, Spring lo maneja con quote o con nombre "order" sin corchetes; verificar que use @Table con nombre "order" y que R2DBC lo quotee. Algunas implementaciones usan backticks o corchetes según dialecto - para MSSQL usar el nombre y Spring configura el dialecto correcto):
```
@Table("order")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Order {
    @Id
    private Integer orderId;
    private LocalDateTime orderDate;
    private String status;
    private String deliveryType;
    private String deliveryAddress;
    private LocalDateTime deliveryDate;
    private BigDecimal totalAmount;
    private Integer storeId;
    private String customerId; // referencia lógica a Mongo
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**OrderDetail.java**:
```
@Table("order_detail")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderDetail {
    @Id
    private Integer orderDetailId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String productId; // referencia lógica a Mongo
    private Integer orderId; // FK a order.orderId
}
```

Usar `@Id` de `org.springframework.data.annotation.Id` (no JPA). Usar `@Table` de `org.springframework.data.relational.core.mapping.Table`.

### Test Requirements
- **TR1 (rule)**: Order.java y OrderDetail.java existen en model/sql/
- **TR2 (rule)**: Order usa @Table("order") y campos: orderId, orderDate, status, deliveryType, deliveryAddress, deliveryDate, totalAmount, storeId, customerId, createdAt, updatedAt
- **TR3 (rule)**: OrderDetail usa @Table("order_detail") y campos: orderDetailId, quantity, unitPrice, productId, orderId
- **TR4 (rule)**: customerId y productId son String (no Long/int), orderId y orderDetailId son Integer

### Evidencia de finalización
Archivos creados.

---

## Task 13: Crear OrderRepository y OrderDetailRepository (R2DBC)

**Prioridad**: high
**Depende de**: T12
**Status**: completed

### Descripción
Crear repositorios en `repository/sql/`:

**OrderRepository.java**:
- Extiende `ReactiveCrudRepository<com.agrimarket.demo.model.sql.Order, Integer>`
- Métodos opcionales: Flux<Order> findByCustomerId(String customerId), Flux<Order> findByStatus(String status)

**OrderDetailRepository.java**:
- Extiende `ReactiveCrudRepository<OrderDetail, Integer>`
- Flux<OrderDetail> findByOrderId(Integer orderId)
- Mono<Void> deleteByOrderId(Integer orderId) (para reemplazo del detalle en editar)

### Test Requirements
- **TR1 (rule)**: OrderRepository.java existe, extiende ReactiveCrudRepository<Order, Integer>
- **TR2 (rule)**: OrderDetailRepository.java existe, tiene findByOrderId y deleteByOrderId
- **TR3 (rule)**: Ambos en package `com.agrimarket.demo.repository.sql`

### Evidencia de finalización
Archivos creados.

---

## Task 14: Crear DTOs para Order

**Prioridad**: high
**Depende de**: ninguna
**Status**: completed

### Descripción
Crear DTOs en `service/dto/` o en un paquete `dto/`. Optar por `com.agrimarket.demo.dto/` o dentro de service. Usemos `dto/` a nivel de demo (paquete plano por ahora como el resto, o en subcarpetas; mantener simple: `dto/order/` o todo en `dto/`).

Crear:
- **OrderItemDTO** (para detalle dentro de create/update): quantity (Integer), unitPrice (BigDecimal), productId (String)
- **CreateOrderRequest**: orderDate, status, deliveryType, deliveryAddress, deliveryDate, totalAmount, storeId, customerId, List<OrderItemDTO> items
- **UpdateOrderRequest**: mismos campos que CreateOrderRequest (incluye items nuevos para reemplazo)
- **OrderWithDetailsDTO**: todos los campos de Order + List<OrderDetailDTO> items (OrderDetailDTO = OrderDetail todos los campos)
- (Opcional: OrderDetailDTO es mismo que OrderDetail pero se puede crear una copia. Por simplicidad, se puede reusar OrderDetail en el DTO de salida.)

Usar records si es posible (Java 21 soporta records), o clases con Lombok.

### Test Requirements
- **TR1 (rule)**: Existen DTOs para Create, Update, Item, y Response WithDetails
- **TR2 (rule)**: CreateOrderRequest tiene customerId (String) y List<OrderItemDTO> items

### Evidencia de finalización
Archivos DTO creados.

---

## Task 15: Crear OrderService + OrderServiceImpl (con transacciones)

**Prioridad**: high
**Depende de**: T13, T14, T5 (TransactionalOperator), T1 (CustomerRepository movido)
**Status**: completed

### Descripción
**OrderService.java** (interface en service/):
- Flux<OrderWithDetailsDTO> findAllWithDetails()
- Mono<OrderWithDetailsDTO> findByIdWithDetails(Integer id)
- Mono<OrderWithDetailsDTO> create(CreateOrderRequest request)
- Mono<OrderWithDetailsDTO> update(Integer id, UpdateOrderRequest request)

**OrderServiceImpl.java**:
Inyectar:
- OrderRepository orderRepository
- OrderDetailRepository orderDetailRepository
- CustomerRepository customerRepository (de repository/mongo)
- TransactionalOperator transactionalOperator (de R2dbcConfig)

**findAllWithDetails()**:
- orderRepository.findAll() -> flatMap por cada order: buscar detalles por orderId, construir OrderWithDetailsDTO -> Flux
- (Usar flatMap + zipWith o flatMap secuencial)

**findByIdWithDetails(id)**:
- orderRepository.findById(id) -> si empty, ResourceNotFoundException
- zipWith orderDetailRepository.findByOrderId(id) -> construir DTO

**create(request)**:
- Paso 1: Validar customerId -> customerRepository.existsById(Long.valueOf(request.getCustomerId())) o findById. Si no existe, Mono.error ResourceNotFoundException("Customer no existe").
- Paso 2: Construir Order entity. Poner createdAt/updatedAt = now. orderId es null (auto-incremental).
- Paso 3: Envolver en TransactionalOperator.transactional(...) el siguiente flujo:
  3a. orderRepository.save(order) -> devuelve order con orderId generado
  3b. Para cada item en request.items: construir OrderDetail con orderId = saved.getOrderId()
  3c. Flux.fromIterable(items).flatMap(orderDetailRepository::save).collectList()
  3d. Construir OrderWithDetailsDTO final (saved order + items guardados)
- Devolver el Mono transaccional.

**update(id, request)**:
- Validar que pedido exista (findById). Si no, 404.
- Envolver en TransactionalOperator.transactional:
  - Actualizar campos del pedido (status, delivery*, etc.), poner updatedAt = now
  - orderRepository.save(updatedOrder)
  - orderDetailRepository.deleteByOrderId(id)
  - Guardar los nuevos detalles (igual que en create)
  - Devolver OrderWithDetailsDTO

### Test Requirements
- **TR1 (rule)**: OrderService/Impl existen con los 4 métodos
- **TR2 (rule)**: create() valida customer_id contra CustomerRepository (Mongo)
- **TR3 (rule)**: create() usa transactionalOperator.transactional(flujo)
- **TR4 (rule)**: update() usa transactionalOperator.transactional(flujo) y hace deleteByOrderId + re-insert
- **TR5 (rule)**: No se usa `.block()` en ningún lado

### Evidencia de finalización
Archivos creados, compilan, lógica transaccional correcta.

---

## Task 16: Crear OrderController (REST)

**Prioridad**: high
**Depende de**: T15
**Status**: completed

### Descripción
Crear rest/OrderController.java:
- @RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
- Inyecta OrderService

Endpoints:
- GET / -> orderService.findAllWithDetails() (devuelve Flux<OrderWithDetailsDTO>)
- GET /{id} -> orderService.findByIdWithDetails(id).map(ResponseEntity::ok).switchIfEmpty(404)
- POST / -> orderService.create(@RequestBody CreateOrderRequest)
- PUT /{id} -> orderService.update(id, @RequestBody UpdateOrderRequest)

### Test Requirements
- **TR1 (rule)**: OrderController.java existe con @RequestMapping("/api/orders")
- **TR2 (rule)**: Tiene los 4 endpoints: GET list, GET by id, POST create, PUT update

### Evidencia de finalización
Controlador creado.

---

## Task 17: Crear entidades SQL - PurchaseOrder y PurchaseOrderDetail

**Prioridad**: high
**Depende de**: T2
**Status**: completed

### Descripción
En model/sql/:

**PurchaseOrder.java** (@Table("purchase_order")):
- @Id Integer purchaseOrderId
- LocalDateTime purchaseOrderDate
- String status
- BigDecimal totalAmount
- String supplierId (referencia lógica a Mongo Supplier)
- Integer storeId
- String notes
- LocalDateTime createdAt
- LocalDateTime updatedAt
- Lombok y @Table

**PurchaseOrderDetail.java** (@Table("purchase_order_detail")):
- @Id Integer purchaseOrderDetailId
- Integer quantity
- BigDecimal unitPrice
- String productId
- Integer purchaseOrderId (FK)
- Lombok y @Table

### Test Requirements
- **TR1 (rule)**: Ambos archivos existen en model/sql/
- **TR2 (rule)**: PurchaseOrder tiene supplierId String y storeId Integer
- **TR3 (rule)**: PurchaseOrderDetail tiene purchaseOrderId Integer y productId String

### Evidencia de finalización
Archivos creados.

---

## Task 18: Crear PurchaseOrderRepository y PurchaseOrderDetailRepository

**Prioridad**: high
**Depende de**: T17
**Status**: completed

### Descripción
En repository/sql/:

**PurchaseOrderRepository**: ReactiveCrudRepository<PurchaseOrder, Integer>
**PurchaseOrderDetailRepository**: ReactiveCrudRepository<PurchaseOrderDetail, Integer>
- findByPurchaseOrderId(Integer purchaseOrderId) : Flux
- deleteByPurchaseOrderId(Integer purchaseOrderId) : Mono<Void>

### Test Requirements
- **TR1 (rule)**: Ambos repositorios existen
- **TR2 (rule)**: PurchaseOrderDetailRepository tiene findByPurchaseOrderId y deleteByPurchaseOrderId

### Evidencia de finalización
Archivos creados.

---

## Task 19: Crear DTOs para PurchaseOrder

**Prioridad**: high
**Depende de**: T14 se puede reutilizar patrón
**Status**: completed

### Descripción
Crear DTOs en dto/ o reutilizar estructura:
- **PurchaseOrderItemDTO**: quantity, unitPrice, productId
- **CreatePurchaseOrderRequest**: purchaseOrderDate, status, totalAmount, supplierId, storeId, notes, List<PurchaseOrderItemDTO> items
- **UpdatePurchaseOrderRequest**: mismos campos + items
- **PurchaseOrderWithDetailsDTO**: campos de PurchaseOrder + List<PurchaseOrderDetail> items

### Test Requirements
- **TR1 (rule)**: Existen DTOs de Create/Update/Item/WithDetails para PurchaseOrder
- **TR2 (rule)**: CreatePurchaseOrderRequest tiene supplierId String

### Evidencia de finalización
Archivos DTO creados.

---

## Task 20: Crear PurchaseOrderService + PurchaseOrderServiceImpl (transaccional)

**Prioridad**: high
**Depende de**: T18, T19, T5, T11 (SupplierService reducido pero disponible, o SupplierRepository directamente)
**Status**: completed

### Descripción
**PurchaseOrderService** interface en service/:
- findAllWithDetails() : Flux<PurchaseOrderWithDetailsDTO>
- findByIdWithDetails(Integer id) : Mono
- create(CreatePurchaseOrderRequest) : Mono
- update(Integer id, UpdatePurchaseOrderRequest) : Mono

**PurchaseOrderServiceImpl**:
Inyectar:
- PurchaseOrderRepository, PurchaseOrderDetailRepository
- SupplierRepository (de repository/mongo) para validar supplier_id
- TransactionalOperator

Mismos patrones que Task 15:
- create: validar supplierRepository.existsById(Long.valueOf(request.supplierId)) o findById -> ResourceNotFoundException si no existe
- transaccional: guardar cabecera -> guardar items con purchaseOrderId generado
- update: encontrar -> actualizar cabecera -> deleteByPurchaseOrderId -> re-insert items -> todo transaccional

### Test Requirements
- **TR1 (rule)**: Service/Impl existen con 4 métodos
- **TR2 (rule)**: create valida supplier_id contra SupplierRepository
- **TR3 (rule)**: create usa TransactionalOperator
- **TR4 (rule)**: update usa TransactionalOperator + deleteByPurchaseOrderId + re-insert

### Evidencia de finalización
Archivos creados.

---

## Task 21: Crear PurchaseOrderController (REST)

**Prioridad**: high
**Depende de**: T20
**Status**: completed

### Descripción
rest/PurchaseOrderController.java:
- @RestController @RequestMapping("/api/purchase-orders")
- Endpoints: GET /, GET /{id}, POST /, PUT /{id}
- Inyecta PurchaseOrderService

### Test Requirements
- **TR1 (rule)**: Controller existe en /api/purchase-orders
- **TR2 (rule)**: 4 endpoints requeridos existen

### Evidencia de finalización
Controller creado.

---

## Task 22: Verificación final - Compilación y arreglo de errores

**Prioridad**: high
**Depende de**: TODAS las anteriores (T1 a T21)
**Status**: completed

### Descripción
Ejecutar `mvn clean compile` desde la raíz del proyecto. Arreglar todos los errores de compilación que aparezcan (imports faltantes, paquetes mal escritos, Lombok no procesado, etc.).

### Test Requirements
- **TR1 (rule)**: `mvn clean compile` exit code == 0
- **TR2 (rule)**: No hay warnings de tipo "cannot find symbol" en la salida final
- **TR3 (rule)**: Customer, Supplier (mantenidos), Product, Category, Brand, Order, PurchaseOrder - todos sus servicios y controllers compilan

### Evidencia de finalización
Captura o log de la salida de mvn clean compile con BUILD SUCCESS.

---

## Notas de Orden de Ejecución (recomendado)
1. **Bloque A - Infraestructura**: T1, T2 (carpetas) -> T3 (deps) -> T4, T5, T6 (config SQL)
2. **Bloque B - Maestros Mongo**: T7 (Product), T8 (Category), T9 (Brand), T10 (Ubigeo)
3. **Bloque C - Supplier reducido**: T11
4. **Bloque D - Order**: T12 -> T13 -> T14 -> T15 -> T16
5. **Bloque E - PurchaseOrder**: T17 -> T18 -> T19 -> T20 -> T21
6. **Bloque F - Verificación**: T22

Los Bloques B y C pueden hacerse en paralelo con Bloque D/E (son independientes). Bloque A es prerequisito de todo.

---

## Completion Evidence Global

Fecha de implementación: 2026-09-18.

### Evidencias por Tarea (resumen)

| Tarea | Evidencia de Completado |
|---|---|
| T1 (modelos Mongo a model/mongo/) | Existen [Customer.java](file:///c:/Users/Brando/Documents/ASE251S4_T11-be/src/main/java/com/agrimarket/demo/model/mongo/Customer.java), Supplier.java, Ubigeo.java, Counter.java en model/mongo/ con package `com.agrimarket.demo.model.mongo`. Archivos antiguos eliminados. Customer y Supplier siguen importando Ubigeo (mismo paquete, sin import). |
| T2 (repos Mongo a repository/mongo/) | CustomerRepository + SupplierRepository existen en repository/mongo/. Carpetas model/sql/ y repository/sql/ creadas (vacías al inicio, luego pobladas). Imports actualizados en CustomerServiceImpl, SupplierServiceImpl. |
| T3 (R2DBC pom.xml) | pom.xml contiene `spring-boot-starter-data-r2dbc` (línea 52) y `r2dbc-mssql` scope runtime (línea 55-57). `mvn dependency:resolve` exitoso (confirmado por compile). |
| T4 (yaml r2dbc) | application.yaml tiene `spring.r2dbc.url` con placeholders SQLSERVER_* + encrypt=false + trustServerCertificate=true. username y password con defaults sa/Sql12345678!. spring.data.mongodb intacto. |
| T5 (R2dbcConfig) | [R2dbcConfig.java](file:///c:/Users/Brando/Documents/ASE251S4_T11-be/src/main/java/com/agrimarket/demo/config/R2dbcConfig.java) declara R2dbcTransactionManager y TransactionalOperator beans. |
| T6 (schema.sql) | [schema.sql](file:///c:/Users/Brando/Documents/ASE251S4_T11-be/db/schema.sql) contiene CREATE TABLE IF NOT EXISTS para: store, inventory, app_user, [order], order_detail, purchase_order, purchase_order_detail (7 tablas). FKs: order_detail -> order, purchase_order_detail -> purchase_order, inventory -> store. customer_id/product_id/supplier_id = NVARCHAR(100) (referencias lógicas). |
| T7 (Product CRUD) | 5 archivos creados: Product.java (model/mongo, @Document products, id Long, name, activeIngredient, presentation, basePrice BigDecimal, imageUrl, description, sku, categoryId, brandId, isActive, metadata Map<String,Object>, timestamps soft delete), ProductRepository, ProductService, ProductServiceImpl (secuencia products_sequence), ProductController /api/products con 7 endpoints. |
| T8 (Category CRUD) | 5 archivos: Category.java @Document categories. Campos: categoryName, parentCategoryId Long, description, level Integer, code, orderDisplay Integer + isActive + soft delete timestamps. Secuencia categories_sequence. Controller /api/categories 7 endpoints. |
| T9 (Brand CRUD) | 5 archivos: Brand.java @Document brands. brandName + isActive + timestamps soft delete. Secuencia brands_sequence. Controller /api/brands 7 endpoints. |
| T10 (Ubigeo repo/svc/ctrl) | Ubigeo.java convertido de record a clase: @Id Long id, @Document ubigeos, department, province, district, Lombok. UbigeoRepository (findByDepartment, findByDepartmentAndProvince), UbigeoService/Impl findAll/findById/findByDepartment, UbigeoController /api/ubigeos (3 endpoints GET). |
| T11 (Supplier reducido) | rest/SupplierController.java eliminado. SupplierService.java reducido a solo findAll/findById (2 métodos). SupplierServiceImpl.java: solo findAll/findById, inyección de SequenceGeneratorService ELIMINADA (no lo usa más). Supplier.java y SupplierRepository intactos. |
| T12 (Order + OrderDetail) | [Order.java](file:///c:/Users/Brando/Documents/ASE251S4_T11-be/src/main/java/com/agrimarket/demo/model/sql/Order.java) @Table("order"), campos orderId INT @Id, orderDate, status, deliveryType, deliveryAddress, deliveryDate, totalAmount BigDecimal, storeId Integer, customerId String, createdAt/updatedAt LocalDateTime. OrderDetail.java @Table order_detail, orderDetailId @Id, quantity Integer, unitPrice BigDecimal, productId String, orderId Integer FK. |
| T13 (Order repos) | OrderRepository extends ReactiveCrudRepository<Order, Integer>. OrderDetailRepository extends ReactiveCrudRepository + findByOrderId(Flux) + deleteByOrderId(Mono<Void>). |
| T14 (Order DTOs) | 4 DTOs creados en dto/: OrderItemDTO (quantity/unitPrice/productId), CreateOrderRequest, UpdateOrderRequest, OrderWithDetailsDTO (from(Order, List<OrderDetail>) static factory). |
| T15 (OrderService transaccional) | OrderService interface 4 métodos. OrderServiceImpl: Inyecta OrderRepository, OrderDetailRepository, CustomerRepository (Mongo), TransactionalOperator. create() valida customer via customerRepository.findById -> ResourceNotFound si no. Guarda order, guarda items con orderId. Todo dentro de `transactionalOperator.transactional(flow)` (línea 75). update() findById -> actualiza order, `deleteByOrderId` + re-inserta items, todo transaccional (línea 108). Sin `.block()`. |
| T16 (OrderController) | [OrderController](file:///c:/Users/Brando/Documents/ASE251S4_T11-be/src/main/java/com/agrimarket/demo/rest/OrderController.java) en /api/orders. GET /, GET /{id} ResponseEntity ok/notFound, POST /, PUT /{id}. |
| T17 (PO + PO-Detail) | PurchaseOrder.java @Table purchase_order: purchaseOrderId INT @Id, purchaseOrderDate, status, totalAmount BigDecimal, supplierId String, storeId Integer, notes, createdAt/updatedAt. PurchaseOrderDetail.java @Table purchase_order_detail: purchaseOrderDetailId @Id, quantity, unitPrice BigDecimal, productId String, purchaseOrderId Integer FK. |
| T18 (PO repos) | PurchaseOrderRepository ReactiveCrudRepository. PurchaseOrderDetailRepository + findByPurchaseOrderId + deleteByPurchaseOrderId. |
| T19 (PO DTOs) | 4 DTOs: PurchaseOrderItemDTO record, CreatePurchaseOrderRequest record, UpdatePurchaseOrderRequest record, PurchaseOrderWithDetailsDTO Lombok. Todos en package com.agrimarket.demo.dto. |
| T20 (PurchaseOrderService transaccional) | PurchaseOrderService interface. PurchaseOrderServiceImpl inyecta: 2 repos SQL, SupplierRepository Mongo, TransactionalOperator. create(): 1) supplierRepository.findById(Long.valueOf(supplierId)) -> 404 si no existe. 2) flow save cabecera + save items envuelto en `flow.as(transactionalOperator::transactional)` (línea 77). update(): 1) findById po. 2) dentro de transacción: supplier validado de nuevo, actualiza campos PO, deleteByPurchaseOrderId, re-inserta items, todo as transactional (línea 113). Sin .block(). |
| T21 (PO Controller) | [PurchaseOrderController](file:///c:/Users/Brando/Documents/ASE251S4_T11-be/src/main/java/com/agrimarket/demo/rest/PurchaseOrderController.java) @RequestMapping /api/purchase-orders. GET /, GET /{id}, POST /, PUT /{id}. |
| T22 (Compilación) | `mvnw clean compile` BUILD SUCCESS exit code 0 (17.6s, 59 source files compilados). GetDiagnostics = [] (0 errores 0 warnings). Warnings solo de Lombok sun.misc.Unsafe (esperados, no fatal). Grep global ".block()" = 0 matches. |

### Resumen cuantitativo
- **59 archivos .java** compilados
- **Nuevos maestros Mongo**: 4 entidades x 5 archivos = 20 + Ubigeo modificado = 21 archivos
- **Order R2DBC**: 11 archivos (2 model + 2 repo + 4 dto + 2 service + 1 controller)
- **PurchaseOrder R2DBC**: 11 archivos (igual estructura)
- **Supplier reducido**: 1 eliminado, 2 modificados
- **Infraestructura**: 1 pom editado, 1 yaml editado, 1 config creado, 1 sql creado
- **Reorganización inicial**: 6 archivos movidos (4 model + 2 repo), imports corregidos en 7+ archivos


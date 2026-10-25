##EJECUCION DEL BACKEND DE FORMA LOCAL Y CREACION DEL ADMIN

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

En Postman, abre `POST http://localhost:8080/api/staff-users`.
En Authorization, selecciona **Bearer Token** y pega el valor token que te devolvió el login.
Envía un body como este. La contraseña la eliges tú; no hace falta generar una clave aleatoria:

**Registrar a un ADMIN**

```json
{
  "email": "admin@agrimarket.local",
  "password": "AdminLocal2026!",
  "firstName": "Administrador",
  "lastName": "Local",
  "role": "ADMIN",
  "isActive": true
}
```

**Iniciar sesión con el usuario ADMIN** en `POST http://localhost:8080/api/auth/login`:

```json
{
  "email": "admin@agrimarket.local",
  "password": "AdminLocal2026!"
}
```

**Importante:** si la base MongoDB está vacía y todavía no existe ningún usuario interno, crea el primer ADMIN en `/api/staff-users` con **No Auth**; aún no tienes un token. Si ya existe un usuario interno, inicia sesión con un ADMIN existente y usa su Bearer token para crear otro. El backend rechaza crear un segundo usuario interno sin autenticación de ADMIN.

---

# Datos de prueba para Postman

Los ejemplos registran datos de prueba para MongoDB y SQL Server. Los IDs se generan al crear cada registro: copia los IDs devueltos y reemplaza en los siguientes JSON los valores de ejemplo.

## Preparación

1. Levanta MongoDB, SQL Server y el backend.
2. Crea o inicia sesión con un ADMIN y copia el token de `/api/auth/login`.
3. En Postman, configura **Authorization → Bearer Token** en las solicitudes protegidas. Pega el token sin escribir `Bearer` delante.
4. Usa **Body → raw → JSON**.
5. Crea los maestros antes que los productos, y los productos/clientes/proveedores/tiendas antes que inventario y transacciones.

El backend precarga el ubigeo **ID 4: Lima / Cañete / San Vicente de Cañete**. Verifica los IDs de productos, clientes, proveedores y tiendas con las respuestas o los endpoints GET; no asumas que comienzan en 1 si ya hay datos en las bases.

## 1. MongoDB: cuatro categorías

Envía cada JSON por separado a `POST http://localhost:8080/api/categories`. Guarda el ID de cada respuesta.

```json
{
  "categoryName": "Semillas",
  "parentCategoryId": null,
  "description": "Semillas para cultivos agrícolas",
  "level": 1,
  "code": "SEM-TEST-01",
  "orderDisplay": 1,
  "isActive": true
}
```

```json
{
  "categoryName": "Fertilizantes",
  "parentCategoryId": null,
  "description": "Fertilizantes para uso agrícola",
  "level": 1,
  "code": "FER-TEST-01",
  "orderDisplay": 2,
  "isActive": true
}
```

```json
{
  "categoryName": "Herramientas agrícolas",
  "parentCategoryId": null,
  "description": "Herramientas para labores agrícolas",
  "level": 1,
  "code": "HER-TEST-01",
  "orderDisplay": 3,
  "isActive": true
}
```

```json
{
  "categoryName": "Protección de cultivos",
  "parentCategoryId": null,
  "description": "Productos para el control de plagas y enfermedades de los cultivos",
  "level": 1,
  "code": "PRO-TEST-01",
  "orderDisplay": 4,
  "isActive": true
}
```

## 2. MongoDB: tres marcas

Envía cada JSON por separado a `POST http://localhost:8080/api/brands`. Guarda los IDs de las respuestas.

```json
{
  "brandName": "AgroAndes Test",
  "isActive": true
}
```

```json
{
  "brandName": "CampoFertil Test",
  "isActive": true
}
```

```json
{
  "brandName": "ValleVerde Test",
  "isActive": true
}
```

## 3. MongoDB: tres proveedores

Envía cada JSON por separado a `POST http://localhost:8080/api/suppliers`. Guarda los IDs: los necesitarás como `supplierId` en la orden de compra.

```json
{
  "businessName": "Proveedor Agrícola San Vicente Test",
  "docType": "RUC",
  "docNumber": "20987654321",
  "phone": "900111201",
  "email": "proveedor1@agrimarket.test",
  "address": "Jr. Los Agricultores 120, San Vicente de Cañete",
  "ubigeo": {
    "id": 4,
    "department": "Lima",
    "province": "Cañete",
    "district": "San Vicente de Cañete"
  },
  "contactPerson": "Contacto Proveedor Uno",
  "isActive": true
}
```

```json
{
  "businessName": "Insumos del Valle Test",
  "docType": "RUC",
  "docNumber": "20987654322",
  "phone": "900111202",
  "email": "proveedor2@agrimarket.test",
  "address": "Av. Mariscal Benavides 240, San Vicente de Cañete",
  "ubigeo": {
    "id": 4,
    "department": "Lima",
    "province": "Cañete",
    "district": "San Vicente de Cañete"
  },
  "contactPerson": "Contacto Proveedor Dos",
  "isActive": true
}
```

```json
{
  "businessName": "Distribuidora Agraria Cañete Test",
  "docType": "RUC",
  "docNumber": "20987654323",
  "phone": "900111203",
  "email": "proveedor3@agrimarket.test",
  "address": "Calle Comercio 350, San Vicente de Cañete",
  "ubigeo": {
    "id": 4,
    "department": "Lima",
    "province": "Cañete",
    "district": "San Vicente de Cañete"
  },
  "contactPerson": "Contacto Proveedor Tres",
  "isActive": true
}
```

## 4. MongoDB: un cliente

Envía a `POST http://localhost:8080/api/customers` con el token ADMIN o VENDEDOR. Los tipos permitidos son `DNI` (8 dígitos), `RUC` (11 dígitos) y `CE` (9 dígitos). Nombres, apellidos, email, teléfono, dirección, ubigeo, contraseña y fecha de registro son obligatorios. `lastPurchaseDate` y `notes` son opcionales. El password debe tener al menos 12 caracteres y el servicio lo guarda cifrado.

```json
{
  "docType": "DNI",
  "docNumber": "74821563",
  "firstName": "Cliente",
  "lastName": "Prueba Agrimarket",
  "email": "cliente.prueba@agrimarket.test",
  "phone": "900111204",
  "address": "Jr. Los Naranjos 145, San Vicente de Cañete",
  "ubigeo": {
    "id": 4,
    "department": "Lima",
    "province": "Cañete",
    "district": "San Vicente de Cañete"
  },
  "password": "ClientePrueba2026!",
  "totalVisits": 0,
  "lastPurchaseDate": null,
  "customerSince": "2026-10-21",
  "notes": "Cliente de prueba creado desde Postman",
  "isActive": true
}
```

Guarda el `id` devuelto; úsalo como `customerId` en la orden de venta.

Para la carga masiva, usa `POST http://localhost:8080/api/customers/import` con el archivo `samples/clientes-carga-masiva.csv` como campo multipart `file`. El CSV incluye correo y contraseña, además de los campos requeridos por el formulario. `ubigeoId` debe corresponder al mismo distrito indicado en `department`, `province` y `district`; esos tres nombres completan el objeto de ubigeo que guarda el cliente. No incluyas `id` del cliente, porque lo genera la base. Los datos y contraseñas del archivo son ficticios y solo para pruebas.

Para consultar las categorías activas disponibles, usa `GET http://localhost:8080/api/categories/status/true`. La tienda pública consulta `GET http://localhost:8080/api/storefront/products?storeId={storeId}`; no se deben inventar categorías o productos en el frontend.

## 5. MongoDB: un producto

Envía a `POST http://localhost:8080/api/products`. Sustituye `categoryId` y `brandId` por los IDs reales de la categoría Semillas y la marca elegida. Guarda el `id` devuelto para inventario y transacciones.

```json
{
  "name": "Semillas de maíz de prueba",
  "activeIngredient": "Maíz",
  "presentation": "Bolsa de 1 kg",
  "basePrice": 24.90,
  "imageUrl": null,
  "description": "Producto de prueba para órdenes e inventario",
  "sku": "MAIZ-TEST-001",
  "categoryId": "1",
  "brandId": "1",
  "metadata": {
    "cultivo": "Maíz",
    "peso": "1 kg"
  },
  "isActive": true,
  "isPublishedOnline": false
}
```

`categoryId`, `brandId` y `productId` se envían como texto aunque sean IDs numéricos de MongoDB. Reemplaza los `"1"` de ejemplo con los valores que recibas. `isPublishedOnline` inicia en `false`: el producto no aparecerá en la tienda web hasta que un ADMIN lo publique y tenga stock en una tienda.

Las consultas y modificaciones administrativas de `/api/products` requieren token ADMIN o VENDEDOR; crear, editar, publicar, restaurar y desactivar requieren token ADMIN.

Para publicar un producto en la tienda, envía `PATCH http://localhost:8080/api/products/{productId}/publication` con token ADMIN:

```json
{
  "isPublishedOnline": true
}
```

La tienda pública consulta `GET http://localhost:8080/api/storefront/products?storeId={storeId}` sin token. Devuelve solo productos activos, publicados y con stock mayor que cero en esa tienda activa.

## 6. SQL Server: cuatro tiendas en San Vicente de Cañete

Envía cada JSON por separado a `POST http://localhost:8080/api/stores` con token ADMIN. La imagen de referencia muestra estas sucursales; el ubigeo `4` corresponde a San Vicente de Cañete. Guarda cada `storeId` devuelto.

```json
{
  "storeName": "Agrimarket - Sucursal Principal",
  "address": "Jr. José Gálvez 113, al costado del semáforo, San Vicente de Cañete",
  "phone": "977693392",
  "ubigeoId": 4
}
```

```json
{
  "storeName": "Agrimarket - Sucursal San Agustín 280",
  "address": "Jr. San Agustín N° 280, San Vicente de Cañete",
  "phone": "977693392",
  "ubigeoId": 4
}
```

```json
{
  "storeName": "Agrimarket - Sucursal San Agustín 330",
  "address": "Jr. San Agustín N° 330, San Vicente de Cañete",
  "phone": "946544845",
  "ubigeoId": 4
}
```

```json
{
  "storeName": "Agrimarket - Sucursal Herbay Alto",
  "address": "Parque de Herbay Alto, calle principal, San Vicente de Cañete",
  "phone": "977693392",
  "ubigeoId": 4
}
```

## 7. SQL Server: un registro de inventario

Envía a `POST http://localhost:8080/api/inventory` con token ADMIN o VENDEDOR. Sustituye `productId` por el ID real del producto y `storeId` por el ID real de una tienda.

```json
{
  "productId": "1",
  "storeId": 1,
  "currentStock": 50,
  "minimumStock": 5
}
```

En el panel administrativo puedes consultar el inventario de un producto y tienda con `GET /api/inventory?productId={productId}&storeId={storeId}`. Si ya existe, actualízalo con `PUT /api/inventory/{inventoryId}`; si no existe, créalo con `POST /api/inventory`. Un producto con inventario en más de una tienda tiene un registro independiente por tienda.

## 8. SQL Server: una orden de venta

Envía a `POST http://localhost:8080/api/orders` con token ADMIN o VENDEDOR. Sustituye `customerId`, `productId` y `storeId` por IDs existentes. El backend calcula `totalAmount` desde `items`; no hace falta mandarlo.

```json
{
  "orderDate": "2026-10-21T15:00:00",
  "status": "PENDING_PAYMENT",
  "deliveryType": "recogida",
  "deliveryAddress": null,
  "deliveryDate": null,
  "storeId": 1,
  "customerId": "1",
  "items": [
    {
      "quantity": 2,
      "unitPrice": 24.90,
      "productId": "1"
    }
  ]
}
```

Esta ruta registra la orden y sus detalles, pero no crea un pago ni descuenta stock. Para el flujo de compra de un cliente, usa carrito y checkout.

## 9. SQL Server: una orden de compra

Envía a `POST http://localhost:8080/api/purchase-orders` con token ADMIN o VENDEDOR. Cambia `supplierId`, `storeId` y `productId` por IDs existentes. Una orden nueva debe iniciar en `PENDING`.

```json
{
  "purchaseOrderDate": "2026-10-21T15:10:00",
  "status": "PENDING",
  "supplierId": "1",
  "storeId": 1,
  "notes": "Reposición de prueba para la sucursal",
  "items": [
    {
      "quantity": 20,
      "unitPrice": 15.00,
      "productId": "1"
    }
  ]
}
```

La orden de compra no incrementa el inventario al crearla. Cuando recibas la mercadería, cambia su estado a `RECEIVED` con `PATCH /api/purchase-orders/{id}/status`; ese paso suma las cantidades al inventario de la tienda.

## 10. Carrito, checkout y pago simulado

El pago no se crea con un POST directo a la tabla `payment`: se genera durante el checkout. Inicia sesión como CLIENTE para obtener su token.

Agrega un producto al carrito con `POST http://localhost:8080/api/cart/items`, token del CLIENTE:

```json
{
  "productId": "1",
  "quantity": 1
}
```

Luego ejecuta `POST http://localhost:8080/api/cart/checkout`, también con token del CLIENTE. Usa el ID real de una tienda con stock suficiente:

```json
{
  "storeId": 1,
  "paymentMethod": "YAPE"
}
```

Los métodos permitidos son `YAPE`, `TARJETA_CREDITO` y `PAGO_EFECTIVO`. El pago es simulado, no cobra dinero real. Si la respuesta devuelve un `paymentId`, la confirmación de prueba se solicita con `POST http://localhost:8080/api/payments/{paymentId}/simulate-confirmation`, usando el token del mismo CLIENTE.

## Ubigeo

Los ubigeos son un catálogo precargado por el backend y solo tienen rutas GET; no se registran por POST. Para San Vicente de Cañete usa el ID `4`, como en los ejemplos.

## Orden recomendado de carga

1. Inicia o crea el ADMIN y configura su token.
2. Crea categorías, marcas y proveedores en MongoDB.
3. Crea el cliente y el producto en MongoDB.
4. Crea las cuatro tiendas en SQL Server.
5. Registra inventario con el producto y una tienda existentes.
6. Crea la orden de venta o usa el flujo de carrito y checkout.
7. Crea la orden de compra; al recibirla, actualiza su estado para sumar stock.

Las rutas y los formatos de estos ejemplos corresponden al backend actual. No uses arrays en los endpoints individuales; cada maestro y tienda de esta guía se crea con una solicitud separada.

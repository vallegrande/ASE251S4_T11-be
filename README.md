# vg-ms-agrimarket

Microservicio Spring Boot reactivo para la gestión de Agrimarket. Usa Java 21,
MongoDB reactivo y SQL Server mediante R2DBC.

Paquete base: `pe.edu.vallegrande.agrimarket`.

## Requisitos

- Java 21
- Docker (opcional, para ejecutar las bases de datos)
- Credenciales y variables de conexión para MongoDB y SQL Server

## Ejecutar

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

Configura `MONGODB_URI`, `SQLSERVER_HOST`, `SQLSERVER_PORT`, `SQLSERVER_DATABASE`,
`SQLSERVER_USER` y `SQLSERVER_PASSWORD` antes de iniciar la aplicación.
El puerto HTTP predeterminado es `8080`; la documentación OpenAPI está disponible
en `/swagger-ui.html`.

## Pruebas

```powershell
.\mvnw.cmd test
```

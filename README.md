# Inventario para eventos

API REST para controlar el material que un planeador de eventos **presta** durante un evento (licores, menaje y cristalería): se recibe, se documenta con fotos, se devuelve y se cuadra. El inventario **no pertenece** a quien lo opera: es custodia temporal, y las fotos son la evidencia que lo respalda.

## Flujo de un evento

```
PLANEADO ──recibir──▶ RECIBIDO ──devolver──▶ DEVUELTO ──cerrar──▶ CERRADO
   │                     │
   │ se registran        │ se registran devueltas, rotas,
   │ los productos       │ perdidas y consumidas
   │ recibidos           │
```

Reglas de negocio:

- Los productos solo se agregan o quitan con el evento en `PLANEADO`.
- No se puede **recibir** sin al menos un producto y una foto de `RECEPCION`.
- La devolución de un producto se registra con el evento en `RECIBIDO`; la suma de `devuelta + rota + perdida + consumida` no puede superar lo recibido.
- Solo los **licores** pueden tener cantidad `consumida` (una botella abierta cuenta como consumida).
- No se puede **devolver** si algún producto no cuadra (`recibida = devuelta + rota + perdida + consumida`) ni sin una foto de `DEVOLUCION`.
- Un evento `CERRADO` no admite cambios ni fotos nuevas.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security) |
| Base de datos | PostgreSQL 17 (Docker) |
| Migraciones | Flyway |
| Autenticación | JWT (HS256), sin estado |
| Documentación | SpringDoc / Swagger UI |
| Build | Maven |

## Requisitos

- JDK 25 (o superior; el proyecto compila con `release 25`)
- Maven 3.9+
- Docker y Docker Compose

## Puesta en marcha

1. Crear el archivo `.env` en la raíz (no se sube a Git):

   ```
   DB_NAME=inventario
   DB_USER=inventario
   DB_PASSWORD=inventario
   DB_PORT=5434
   ```

2. Levantar la base de datos y comprobar que quede `healthy`:

   ```bash
   docker compose up -d
   docker compose ps
   ```

3. Arrancar la aplicación (perfil `dev` por defecto). Flyway aplica las migraciones al iniciar:

   ```bash
   mvn spring-boot:run
   ```

4. Abrir la documentación interactiva: <http://localhost:8080/swagger-ui.html>

### Puertos

| Puerto | Uso |
|---|---|
| 8080 | API (solo `127.0.0.1` en desarrollo) |
| 5434 | PostgreSQL del proyecto (solo `127.0.0.1`) |

Antes de cambiar o asignar un puerto, verifica que esté libre: `sudo ss -ltnp | grep -E ':(8080|5434)\b'`.

## Configuración

| Variable / propiedad | Descripción |
|---|---|
| `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` | Conexión a PostgreSQL (`.env` para Docker Compose) |
| `STORAGE_ROOT` | Carpeta donde se guardan las fotos. Por defecto `~/inventario-uploads` |
| `JWT_SECRET` | Clave de firma de los tokens (mínimo 32 caracteres). Obligatoria fuera del perfil `dev` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Usuario único de la aplicación (contraseña de 12+ caracteres). Obligatorias fuera del perfil `dev` |
| `app.security.jwt.expiration` | Vigencia del token (por defecto `12h`) |
| `app.evidencia.obligatoria` | Exige fotos para recibir y devolver (por defecto `true`) |

En el perfil `dev`, el secreto y el usuario están en `application-dev.yml` y **solo sirven para desarrollo**.

## API

Todas las rutas, salvo el login, exigen `Authorization: Bearer <token>`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Devuelve el token JWT |
| GET, POST | `/api/productos` | Listar (filtro `?tipo=`) y crear |
| GET, PUT, DELETE | `/api/productos/{id}` | Detalle, actualizar y desactivar (borrado lógico) |
| PUT, GET, DELETE | `/api/productos/{id}/foto` | Foto de referencia del producto |
| GET, POST | `/api/planeadores` | Listar y crear |
| GET, PUT, DELETE | `/api/planeadores/{id}` | Detalle, actualizar y eliminar |
| GET, POST | `/api/eventos` | Listar y crear |
| GET | `/api/eventos/{id}` | Evento con sus productos |
| PUT, DELETE | `/api/eventos/{id}/items/{productoId}` | Registrar o quitar un producto recibido |
| POST | `/api/eventos/{id}/recibir` | PLANEADO → RECIBIDO |
| PUT | `/api/eventos/{id}/items/{productoId}/devolucion` | Registrar la devolución de un producto |
| POST | `/api/eventos/{id}/devolver` | RECIBIDO → DEVUELTO (exige cuadre) |
| POST | `/api/eventos/{id}/cerrar` | DEVUELTO → CERRADO |
| GET | `/api/eventos/{id}/reporte` | Totales por tipo, detalle, cuadre y fotos |
| POST, GET | `/api/eventos/{id}/fotos` | Subir (multipart) y listar fotos de evidencia |
| GET, DELETE | `/api/fotos/{id}/archivo`, `/api/fotos/{id}` | Descargar y eliminar una foto |

Los errores siguen el formato `ProblemDetail` (RFC 9457): `400` datos inválidos, `401` sin credenciales, `404` no encontrado, `409` regla de negocio o conflicto de datos, `413` archivo demasiado grande.

## Pruebas

```bash
mvn test                              # todas (las de integración requieren Docker)
mvn test -Dtest='!*IntegrationTest'   # solo unitarias, sin Docker
```

- **Unitarias:** dominio, reglas de negocio de `EventoService` (Mockito), detección de imágenes y almacenamiento.
- **Integración:** aplicación completa con MockMvc, seguridad JWT y PostgreSQL real en un contenedor (Testcontainers). Usan el perfil `test`.
## Estructura

```
src/main/java/com/eventos/inventario
├── auth/          login y emisión de tokens
├── security/      configuración de Spring Security y JWT
├── compartido/    entidad base, excepciones, almacenamiento de imágenes
├── producto/      catálogo genérico de productos
├── planeador/     quien presta el inventario
├── evento/        ciclo de custodia: recibir, devolver, cuadrar
├── foto/          evidencia fotográfica
└── reporte/       resumen del evento
src/main/resources
├── application*.yml
└── db/migration/  migraciones de Flyway (V1__esquema_inicial.sql, ...)
```

Cada módulo sigue la misma forma: `Controller → Service → Repository`, con DTOs de entrada y salida (los controladores nunca devuelven entidades). Los módulos se comunican por servicios, no accediendo a repositorios ajenos.

## Migraciones

Una migración aplicada **nunca se edita**: los cambios van en una nueva (`V2__...sql`). En desarrollo, si hace falta empezar de cero: `docker compose down -v` (borra los datos de esta base).

## Respaldos

La evidencia vive en **dos lugares** que deben respaldarse juntos: la base de datos (volumen `inventario-data`) y la carpeta de fotos (`STORAGE_ROOT`). Una foto sin su registro, o un registro sin su archivo, es evidencia rota.

## Hoja de ruta

- [x] Modelo de datos y migración inicial
- [x] Productos, planeadores y eventos
- [x] Recepción, devolución y cuadre
- [x] Evidencia fotográfica y foto de referencia
- [x] Reporte del evento
- [x] Autenticación con JWT
- [x] Pruebas automáticas (reglas del cuadre y estados)
- [ ] Protección contra fuerza bruta en el login y HTTPS antes de exponer el servidor
- [ ] Respaldo conjunto de base de datos y fotos
- [ ] Aplicación móvil con modo offline y sincronización
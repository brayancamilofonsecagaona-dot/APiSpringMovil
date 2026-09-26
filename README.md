# Lecto API

API REST del proyecto **Lecto**, una app Android para digitalizar apuntes de clase con OCR.
La app toma una foto del apunte, extrae el texto y lo organiza en **materias** y **notas**.
Esta API es la parte remota del sistema: guarda los datos de cada usuario en una base de datos
PostgreSQL y permite que la app sincronice su base local con el servidor.

La app Android vive en otro repositorio; este repo contiene solo la API y el esquema de la base de datos.

- **Producción:** https://apispringmovil.onrender.com
- **Documentación de endpoints:** [docs/endpoints.md](docs/endpoints.md)

---

## Stack

| Pieza | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4 (Web MVC, Data JPA, Validation) |
| Base de datos | PostgreSQL (alojada en Supabase) |
| Migraciones | Flyway |
| Autenticación | Firebase Authentication, validado con Firebase Admin SDK |
| Despliegue | Render, con Docker |
| Build | Maven (incluye el wrapper `mvnw`) |

### ¿Por qué Spring Boot?

La app Android está escrita en Java, así que usar Spring Boot permite que todo el equipo trabaje
con **el mismo lenguaje** en el cliente y en el servidor: los modelos, las validaciones y la forma de
pensar el código se parecen en ambos lados. Además, Spring Boot trae resuelto lo que la API necesita
(JPA para la base de datos, validación de DTOs, manejo centralizado de errores) y hay una librería
oficial de Firebase Admin para Java con la que se validan los tokens.

---

## Cómo levantarla en local

### Requisitos

- JDK 21
- Docker (para la base de datos PostgreSQL)
- IntelliJ IDEA (o Maven desde la terminal)
- El archivo JSON de la **cuenta de servicio de Firebase** del proyecto
  (Firebase Console → Configuración del proyecto → Cuentas de servicio → *Generar nueva clave privada*).
  Este archivo es secreto: no se sube al repositorio.

### 1. Levantar PostgreSQL con Docker

```bash
docker run --name lecto-db \
  -e POSTGRES_DB=lecto \
  -e POSTGRES_USER=lecto \
  -e POSTGRES_PASSWORD=lecto \
  -p 5432:5432 \
  -d postgres:16
```

No hace falta crear las tablas a mano: al arrancar, **Flyway** ejecuta las migraciones de
`src/main/resources/db/migration` y crea el esquema (ver [Esquema de la base de datos](#esquema-de-la-base-de-datos)).

Para volver a arrancar el contenedor otro día: `docker start lecto-db`.

### 2. Variables de entorno

La configuración está en `src/main/resources/application.properties` y se toma de variables de entorno:

| Variable | Obligatoria | Valor por defecto | Descripción |
|---|---|---|---|
| `DB_HOST` | No | `localhost` | Host de PostgreSQL |
| `DB_PORT` | No | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | No | `lecto` | Nombre de la base de datos |
| `DB_USER` | **Sí** | — | Usuario de la base de datos |
| `DB_PASSWORD` | **Sí** | — | Contraseña de la base de datos |
| `DB_SSLMODE` | No | `disable` | Modo SSL de la conexión JDBC (`disable` en local, `require` con Supabase) |
| `FIREBASE_CREDENTIALS` | **Sí** | — | El **contenido completo** del JSON de la cuenta de servicio (el texto, no la ruta al archivo) |
| `PORT` | No | `8080` | Puerto HTTP. Render lo inyecta solo; en local no hace falta |

Si falta `FIREBASE_CREDENTIALS`, `DB_USER` o `DB_PASSWORD`, la aplicación no arranca.

Con el contenedor del paso anterior, los valores en local son:

```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=lecto
DB_USER=lecto
DB_PASSWORD=lecto
DB_SSLMODE=disable
FIREBASE_CREDENTIALS={"type":"service_account","project_id":"...", ... }
```

> **Consejo:** el JSON de Firebase viene en varias líneas. Conviene pegarlo en **una sola línea**
> (minificado) para que no se corte al ponerlo como variable de entorno. Los `\n` dentro de
> `private_key` se dejan tal cual.

### 3. Configurarlas en IntelliJ IDEA

1. Abre `src/main/java/com/lecto/demo/DemoApplication.java` y ejecútala una vez con el botón ▶
   para que IntelliJ cree la configuración de ejecución.
2. Ve a **Run → Edit Configurations…** y selecciona `DemoApplication`.
3. Verifica que el **JDK sea 21**.
4. En **Environment variables** haz clic en el ícono de la derecha (📄) y agrega cada variable
   en la tabla con el botón **+** (nombre a la izquierda, valor a la derecha).
   Usar la tabla es más seguro que escribir todo en una línea, porque el JSON de Firebase tiene
   comillas y caracteres especiales.
   Si no ves el campo, actívalo en **Modify options → Environment variables**.
5. Guarda y ejecuta. Cuando arranque, abre http://localhost:8080/health y debería responder:

```json
{ "status": "up", "db": "up" }
```

### Alternativa: desde la terminal

PowerShell (Windows):

```powershell
$env:DB_USER="lecto"; $env:DB_PASSWORD="lecto"
$env:FIREBASE_CREDENTIALS = Get-Content -Raw ruta\a\cuenta-servicio.json
.\mvnw.cmd spring-boot:run
```

Bash (Linux/macOS/Git Bash):

```bash
export DB_USER=lecto DB_PASSWORD=lecto
export FIREBASE_CREDENTIALS="$(cat ruta/a/cuenta-servicio.json)"
./mvnw spring-boot:run
```

### Alternativa: la API también en Docker

```bash
docker build -t lecto-api .
docker run --rm -p 8080:8080 --env-file .env lecto-api
```

Donde `.env` tiene las variables del paso 2 (una por línea, `FIREBASE_CREDENTIALS` en una sola línea y sin comillas).
Si la base corre en otro contenedor del mismo equipo, usa `DB_HOST=host.docker.internal` en lugar de `localhost`.
No subas el `.env` al repositorio.

---

## Despliegue en Render

La API se despliega en Render como **Web Service con runtime Docker**, usando el `Dockerfile` de la raíz:

1. **Etapa de compilación** (`maven:3.9-eclipse-temurin-21`): descarga las dependencias y genera el `.jar`
   con `mvn clean package -DskipTests`.
2. **Etapa de ejecución** (`eclipse-temurin:21-jre`): copia solo el `.jar` y lo ejecuta con `java -jar app.jar`.

Render construye la imagen en cada push a la rama configurada y vuelve a desplegar.
Render inyecta la variable `PORT` y la API la usa (`server.port=${PORT:8080}`).

### Variables de entorno en Render

En el servicio → **Environment**:

| Variable | Valor |
|---|---|
| `DB_HOST` | Host de la base en Supabase (Project Settings → Database → Connection string) |
| `DB_PORT` | Puerto que indique Supabase (normalmente `5432`) |
| `DB_NAME` | `postgres` (o el nombre que tenga la base en Supabase) |
| `DB_USER` | Usuario que indique Supabase |
| `DB_PASSWORD` | Contraseña de la base de Supabase |
| `DB_SSLMODE` | `require` (Supabase exige SSL) |
| `FIREBASE_CREDENTIALS` | Contenido completo del JSON de la cuenta de servicio |

`PORT` **no** se configura: la pone Render.

> **Nota sobre Supabase:** la conexión directa de Supabase (`db.<proyecto>.supabase.co`) usa IPv6.
> Si Render no logra conectarse, usa el **Session pooler** que ofrece Supabase en la misma pantalla
> (host `...pooler.supabase.com`, usuario con el formato `postgres.<id-del-proyecto>`).

Para el *Health Check Path* de Render se puede usar `/health`, que es público y además comprueba la conexión a la base.

Si el servicio está en el plan gratuito de Render, se duerme tras un rato sin tráfico y la primera
petición después de eso puede tardar bastante en responder mientras arranca.

---

## Estructura del proyecto

```
src/main/java/com/lecto/demo/
├── DemoApplication.java      Punto de entrada de Spring Boot
├── config/                   Configuración al arrancar
├── security/                 Filtro que valida el token de Firebase
├── controller/               Endpoints REST
├── service/                  Lógica de negocio
├── repository/               Acceso a datos (Spring Data JPA)
├── entity/                   Entidades JPA (mapeo de tablas)
├── dto/                      Objetos de entrada y salida del JSON
└── exception/                Manejo centralizado de errores

src/main/resources/
├── application.properties    Configuración (lee variables de entorno)
└── db/migration/             Migraciones de Flyway (V1__esquema.sql)
```

Qué hace cada capa:

- **`config`** — `FirebaseConfig` inicializa el SDK de Firebase Admin una sola vez al arrancar,
  leyendo las credenciales de `FIREBASE_CREDENTIALS`.
- **`security`** — `FirebaseTokenFilter` intercepta todas las peticiones (menos `/health` y `OPTIONS`),
  valida el token `Bearer` con Firebase, crea el usuario en la base si es la primera vez que entra
  y deja el `uid` en el request para que lo lean los controllers.
- **`controller`** — Reciben la petición HTTP, validan el cuerpo con `@Valid`, leen el `uid` del
  request y delegan en el service. Devuelven el código HTTP adecuado (200, 201, 204).
  Hay uno por recurso: `HealthController`, `MateriaController`, `NotaController`, `UsuarioController`, `SyncController`.
- **`service`** — Reglas del negocio: que cada usuario solo vea lo suyo, el borrado lógico,
  el control de versiones por `fecha_modificacion`, no borrar materias con notas, etc.
  Cuando algo falla lanzan `ResponseStatusException` con el código correspondiente (404, 409…).
- **`repository`** — Interfaces de Spring Data JPA. La mayoría de consultas se derivan del nombre
  del método; el listado de materias usa una consulta JPQL que además cuenta las notas activas.
- **`entity`** — Clases `Materia`, `Nota` y `Usuario` mapeadas a las tablas `materias`, `notas` y `usuarios`.
  En la base, el borrado lógico es la columna booleana `eliminado`.
- **`dto`** — Lo que entra y sale en el JSON. Los DTOs de respuesta nunca exponen `usuario_id`
  y traducen `eliminado` al campo `estado` (**1 = activo, 0 = eliminado**).
  Todos los campos del JSON van en **snake_case** (`spring.jackson.property-naming-strategy=SNAKE_CASE`).
- **`exception`** — `GlobalExceptionHandler` convierte cualquier excepción de los controllers en una
  respuesta JSON con el formato `{"error": "mensaje"}`.

---

## Esquema de la base de datos

El esquema **lo crea y lo versiona Flyway**, no Hibernate. Al arrancar, Flyway aplica en orden los
scripts de `src/main/resources/db/migration` que aún no se hayan ejecutado y lleva el registro en la
tabla `flyway_schema_history`.

Hibernate está configurado con:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Es decir, Hibernate **solo verifica** que las entidades coincidan con las tablas existentes y
**nunca crea ni modifica** tablas. Si una entidad no cuadra con el esquema, la aplicación no arranca.

Para cambiar el esquema:

1. Crea un nuevo archivo `V2__descripcion.sql`, `V3__...`, etc. en `db/migration`.
2. **No edites** una migración que ya se aplicó (por ejemplo `V1__esquema.sql`): Flyway guarda su checksum
   y la aplicación fallará al arrancar si el archivo cambia.
3. Ajusta las entidades para que coincidan con el nuevo esquema.

> `V1__esquema.sql` inserta datos de prueba (un usuario `uid-prueba-1`, una materia y una nota).
> `V2__limpiar_datos_prueba.sql` los borra, así que no quedan en ninguna base después de aplicar todas las migraciones.

---

## Limitaciones conocidas

- **Relojes en la sincronización:** `GET /sync` compara `fecha_modificacion`, pero esa fecha viene del reloj
  del celular al crear y editar, y del reloj del servidor al borrar. Si el reloj de un dispositivo está
  desfasado, sus cambios podrían no llegar a otro dispositivo.
- **Búsqueda limitada:** la búsqueda con `?q=` en `GET /notas` solo busca en el texto de la nota, no en el título.
- **Los PUT reemplazan el recurso completo:** si no se manda `imagen_url` (en notas) o `foto_url` (en el perfil),
  se borra el valor guardado.

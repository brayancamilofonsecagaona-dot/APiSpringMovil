# Lecto API — Documentación de endpoints

**URL base (producción):** `https://apispringmovil.onrender.com`
**URL base (local):** `http://localhost:8080`

## Índice

- [Convenciones generales](#convenciones-generales)
- [Autenticación](#autenticación)
- [Errores comunes a todos los endpoints protegidos](#errores-comunes-a-todos-los-endpoints-protegidos)
- [Salud](#salud)
  - [GET /health](#get-health)
- [Materias](#materias)
  - [GET /materias](#get-materias)
  - [GET /materias/{id}](#get-materiasid)
  - [POST /materias](#post-materias)
  - [PUT /materias/{id}](#put-materiasid)
  - [DELETE /materias/{id}](#delete-materiasid)
- [Notas](#notas)
  - [GET /notas](#get-notas)
  - [GET /notas/{id}](#get-notasid)
  - [POST /notas](#post-notas)
  - [PUT /notas/{id}](#put-notasid)
  - [DELETE /notas/{id}](#delete-notasid)
- [Usuarios](#usuarios)
  - [GET /usuarios/me](#get-usuariosme)
  - [POST /usuarios](#post-usuarios)
  - [PUT /usuarios/me](#put-usuariosme)
  - [DELETE /usuarios/me](#delete-usuariosme)
- [Sincronización](#sincronización)
  - [GET /sync](#get-sync)

---

## Convenciones generales

- **Formato:** todas las peticiones y respuestas con cuerpo usan JSON.
  En las peticiones con cuerpo se debe enviar la cabecera `Content-Type: application/json`.
- **Nombres de campos:** siempre en **snake_case** (`fecha_creacion`, `materia_id`, `nombre_usuario`…).
- **Campo `estado`:** en las respuestas de materias y notas, `1` = activo y `0` = eliminado.
  En la base de datos se guarda como la columna booleana `eliminado`; el campo `estado` solo existe en el JSON
  y **no se envía** en las peticiones (si se envía, se ignora).
- **IDs:** materias y notas usan **UUID generados por la app** (no por el servidor). El id de usuario es el
  **UID de Firebase**.
- **Fechas:** formato ISO-8601 en UTC, por ejemplo `2026-09-26T15:30:00Z`. Las respuestas pueden incluir
  fracciones de segundo (`2026-09-26T15:30:00.123456Z`).
- **Borrado lógico:** eliminar una materia o una nota no borra la fila; la marca como eliminada.
  Los registros eliminados ya no aparecen en los listados ni en el detalle (responden 404), pero sí en
  `GET /sync` con `estado: 0`.
- **Aislamiento por usuario:** cada usuario solo ve y modifica sus propios datos. Si se pide una materia o
  nota de otro usuario, la API responde `404` (no confirma que exista).
- **Campos desconocidos:** si el cuerpo trae campos que la API no conoce, se ignoran.

### Formato de los errores

Todos los errores responden con un JSON con la clave `error`:

```json
{ "error": "Materia no encontrada" }
```

Los errores de validación de campos (400) agregan la clave `campos`, con un mensaje por cada campo que falló:

```json
{
  "error": "Datos inválidos",
  "campos": {
    "color": "El color debe ser un hexadecimal válido (ej. #FF5733)",
    "fecha_modificacion": "La fecha de modificación es obligatoria"
  }
}
```

Las claves dentro de `campos` usan el mismo nombre en snake_case que el campo del JSON (`fecha_modificacion`, `materia_id`, `nombre_usuario`…).

---

## Autenticación

Todos los endpoints, **excepto `GET /health`**, requieren un **ID token de Firebase Authentication**.
La API no maneja contraseñas ni inicios de sesión: el usuario inicia sesión en la app con Firebase y
la app envía el token que Firebase le entrega.

### Cómo se envía

En la cabecera `Authorization`, con el prefijo `Bearer ` (con un espacio):

```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...
```

### Cómo obtener el token en la app Android

Después de que el usuario inicia sesión con Firebase Auth:

```java
FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
usuario.getIdToken(false).addOnSuccessListener(resultado -> {
    String token = resultado.getToken();
    // Enviar en cada petición: "Authorization: Bearer " + token
});
```

El token **vence a la hora**. `getIdToken(false)` devuelve el token guardado y lo renueva solo si ya venció.
Si la API responde `401 "Token inválido o expirado"`, se puede forzar la renovación con `getIdToken(true)` y reintentar.

### Cómo obtener un token para probar (Postman / curl)

Con un usuario de correo y contraseña ya creado en Firebase, se puede pedir un token a la API REST de Firebase
usando la *Web API Key* del proyecto (Firebase Console → Configuración del proyecto → General):

```http
POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=<WEB_API_KEY>
Content-Type: application/json

{ "email": "usuario@correo.com", "password": "********", "returnSecureToken": true }
```

El valor del campo `idToken` de la respuesta es el token que se manda en la cabecera `Authorization`.

### Qué hace la API con el token

1. Verifica el token con Firebase Admin (firma, vencimiento y que sea del proyecto correcto).
2. Si es la **primera vez** que ese usuario llama a la API, crea automáticamente su registro en la tabla
   `usuarios` con los datos del token:
   - `correo`: el del token (si no viene, se usa `<uid>@lecto.local`).
   - `nombre`: el del token (si no viene, `"Usuario Lecto"`).
   - `nombre_usuario`: la parte del correo antes de la `@` (máximo 35 caracteres); si ya está en uso,
     se le agrega un número (`brayan`, `brayan1`, `brayan2`…).
3. Toma el `uid` del token como identificador del usuario para toda la petición.
   Nunca se envía el id del usuario en el cuerpo ni en la URL.

---

## Errores comunes a todos los endpoints protegidos

Estos errores pueden salir en **cualquier** endpoint que requiere token, además de los específicos de cada uno:

| Código | Mensaje (`error`) | Cuándo ocurre |
|---|---|---|
| 401 | `Token ausente o mal formado` | No se envió la cabecera `Authorization` o no empieza por `Bearer `. |
| 401 | `Token inválido o expirado` | Firebase rechazó el token: vencido, firma inválida, de otro proyecto, etc. |
| 500 | `Error interno de autenticación` | Falla del servidor al validar el token (por ejemplo, Firebase no está inicializado). |
| 503 | `No se pudo registrar el usuario` | El token es válido pero no se pudo crear el usuario en la base (base caída o conflicto al insertar). |
| 404 | `La ruta no existe` | La ruta no existe (con token válido). |
| 405 | `Método <MÉTODO> no permitido en esta ruta` | La ruta existe pero no con ese método, por ejemplo `PATCH /materias`. |
| 415 | `El tipo de contenido no es soportado, usa application/json` | Una petición con cuerpo (POST/PUT) no envió `Content-Type: application/json`. |
| 500 | `Ocurrió un error interno` | Cualquier error inesperado del servidor. El detalle queda en los logs. |

Nota: la validación del token ocurre **antes** que todo lo demás. Por eso, una ruta inexistente o un método no
permitido llamados **sin token** responden `401`, no `404`/`405`.

---

## Salud

### GET /health

Indica si la API está viva y si tiene conexión con la base de datos. Útil para despertar el servicio en
Render o como *health check*.

- **Requiere token:** No
- **Parámetros:** ninguno
- **Cuerpo:** ninguno

**Respuesta `200 OK`** — API y base de datos funcionando:

```json
{
  "status": "up",
  "db": "up"
}
```

**Errores:**

| Código | Respuesta | Significado |
|---|---|---|
| 503 | `{"status": "degraded", "db": "down"}` | La API responde pero no pudo consultar la base de datos. |

---

## Materias

Objeto **materia** que devuelve la API:

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador, generado por la app. |
| `nombre` | string | Nombre de la materia (máx. 60). |
| `color` | string | Color hexadecimal `#RRGGBB`. |
| `icono` | string | Nombre del ícono (máx. 30). |
| `fecha_creacion` | fecha ISO | Fecha de creación (la envía la app). |
| `fecha_modificacion` | fecha ISO | Última modificación. |
| `estado` | int | `1` = activa, `0` = eliminada. |
| `cantidad_notas` | long | Número de notas activas de la materia. |

### GET /materias

Lista las materias **activas** del usuario autenticado, ordenadas por nombre (A–Z), con su número de notas activas.

- **Requiere token:** Sí
- **Parámetros:** ninguno
- **Cuerpo:** ninguno

**Respuesta `200 OK`:**

```json
[
  {
    "id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
    "nombre": "Móviles",
    "color": "#FFC93D",
    "icono": "codigo",
    "fecha_creacion": "2026-09-20T14:00:00Z",
    "fecha_modificacion": "2026-09-20T14:00:00Z",
    "estado": 1,
    "cantidad_notas": 3
  }
]
```

Si el usuario no tiene materias, responde `[]`.

**Errores:** solo los [comunes](#errores-comunes-a-todos-los-endpoints-protegidos).

---

### GET /materias/{id}

Devuelve el detalle de una materia del usuario.

- **Requiere token:** Sí
- **Parámetros de ruta:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Id de la materia. |

- **Cuerpo:** ninguno

**Respuesta `200 OK`:**

```json
{
  "id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
  "nombre": "Móviles",
  "color": "#FFC93D",
  "icono": "codigo",
  "fecha_creacion": "2026-09-20T14:00:00Z",
  "fecha_modificacion": "2026-09-20T14:00:00Z",
  "estado": 1,
  "cantidad_notas": 3
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'id' no es válido` | El `id` de la URL no es un UUID. |
| 404 | `Materia no encontrada` | No existe, es de otro usuario o está eliminada. |

---

### POST /materias

Crea una materia. El `id` lo genera la app (UUID), para que la misma materia tenga el mismo id en la base
local y en el servidor.

- **Requiere token:** Sí
- **Parámetros:** ninguno
- **Cuerpo:**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `id` | UUID | Sí | No puede existir ya en la base. |
| `nombre` | string | Sí | No vacío, máx. 60 caracteres. |
| `color` | string | Sí | Hexadecimal `#RRGGBB` (ej. `#FF5733`). |
| `icono` | string | Sí | No vacío, máx. 30 caracteres. |
| `fecha_creacion` | fecha ISO | Sí | |
| `fecha_modificacion` | fecha ISO | Sí | |

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "nombre": "Cálculo",
  "color": "#4A90E2",
  "icono": "calculadora",
  "fecha_creacion": "2026-09-26T15:30:00Z",
  "fecha_modificacion": "2026-09-26T15:30:00Z"
}
```

**Respuesta `201 Created`:**

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "nombre": "Cálculo",
  "color": "#4A90E2",
  "icono": "calculadora",
  "fecha_creacion": "2026-09-26T15:30:00Z",
  "fecha_modificacion": "2026-09-26T15:30:00Z",
  "estado": 1,
  "cantidad_notas": 0
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `Datos inválidos` (+ `campos`) | Falta un campo obligatorio o no cumple las reglas (color inválido, nombre vacío o muy largo, etc.). |
| 400 | `El cuerpo de la petición JSON está mal formado o es inválido` | JSON mal escrito, o un valor con formato incorrecto (UUID o fecha inválidos). |
| 409 | `La materia ya existe` | Ya hay una materia con ese `id` (de cualquier usuario, incluso si está eliminada). Suele pasar cuando la app reintenta un envío que ya había llegado. |
| 409 | `Error de integridad en la base de datos` | La base de datos rechazó el registro por una restricción. |

---

### PUT /materias/{id}

Actualiza una materia. Se reemplazan `nombre`, `color`, `icono` y `fecha_modificacion`.

- **Requiere token:** Sí
- **Parámetros de ruta:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Id de la materia a actualizar. |

- **Cuerpo:** los mismos campos y reglas que en [POST /materias](#post-materias). Además:
  - `id` es obligatorio en el cuerpo, pero **se ignora**: siempre se actualiza la materia del `id` de la URL.
  - `fecha_creacion` es obligatoria, pero **se ignora**: la fecha de creación nunca cambia.
  - **Control de versiones:** si `fecha_modificacion` es **anterior** a la guardada en el servidor, la
    actualización se rechaza con `409` (gana la versión más reciente). Si es igual o posterior, se acepta.

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "nombre": "Cálculo Integral",
  "color": "#E94E77",
  "icono": "calculadora",
  "fecha_creacion": "2026-09-26T15:30:00Z",
  "fecha_modificacion": "2026-09-26T16:10:00Z"
}
```

**Respuesta `200 OK`:**

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "nombre": "Cálculo Integral",
  "color": "#E94E77",
  "icono": "calculadora",
  "fecha_creacion": "2026-09-26T15:30:00Z",
  "fecha_modificacion": "2026-09-26T16:10:00Z",
  "estado": 1,
  "cantidad_notas": 0
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'id' no es válido` | El `id` de la URL no es un UUID. |
| 400 | `Datos inválidos` (+ `campos`) | Algún campo no cumple las reglas. |
| 400 | `El cuerpo de la petición JSON está mal formado o es inválido` | JSON mal escrito o con valores de formato incorrecto. |
| 404 | `Materia no encontrada` | No existe, es de otro usuario o está eliminada. |
| 409 | `La versión enviada es más antigua que la guardada` | La `fecha_modificacion` enviada es anterior a la del servidor. |
| 409 | `Error de integridad en la base de datos` | La base de datos rechazó el cambio por una restricción. |

---

### DELETE /materias/{id}

Elimina una materia (**borrado lógico**): la marca como eliminada y pone `fecha_modificacion` en la hora
actual del servidor, para que `GET /sync` informe el borrado.

Solo se puede eliminar una materia que **no tenga notas activas**.

- **Requiere token:** Sí
- **Parámetros de ruta:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Id de la materia a eliminar. |

- **Cuerpo:** ninguno

**Respuesta `204 No Content`** (sin cuerpo).

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'id' no es válido` | El `id` de la URL no es un UUID. |
| 404 | `Materia no encontrada` | No existe, es de otro usuario o ya estaba eliminada. |
| 409 | `No se puede eliminar una materia que tiene notas` | La materia todavía tiene notas activas; hay que eliminarlas o moverlas antes. |

---

## Notas

Objeto **nota** que devuelve la API:

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador, generado por la app. |
| `materia_id` | UUID | Materia a la que pertenece. |
| `titulo` | string | Título (máx. 120). |
| `texto` | string | Texto de la nota (el resultado del OCR, editable). Puede ser `""`. |
| `imagen_url` | string o `null` | URL de la imagen del apunte (opcional). |
| `fecha_creacion` | fecha ISO | Fecha de creación (la envía la app). |
| `fecha_modificacion` | fecha ISO | Última modificación. |
| `estado` | int | `1` = activa, `0` = eliminada. |

### GET /notas

Lista las notas **activas** del usuario. Permite filtrar por materia y buscar por palabra clave.

- **Requiere token:** Sí
- **Parámetros de consulta (query):**

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `materia_id` | UUID | No | Devuelve solo las notas de esa materia. |
| `q` | string | No | Busca notas cuyo **`texto`** contenga esta cadena, sin distinguir mayúsculas/minúsculas. **No busca en el título.** Si viene vacío o solo con espacios, se ignora. |

- **Cuerpo:** ninguno

Los resultados salen de la más reciente a la más antigua según `fecha_creacion`.

Si `materia_id` no existe o es de otro usuario, responde `200` con `[]` (no da error).

**Ejemplos:**

```
GET /notas
GET /notas?materia_id=a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d
GET /notas?q=bucle
GET /notas?materia_id=a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d&q=bucle
```

**Respuesta `200 OK`:**

```json
[
  {
    "id": "3f6c2a1e-8b7d-4c1a-9e2f-5a0b1c2d3e4f",
    "materia_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
    "titulo": "Estructuras de control",
    "texto": "El bucle for se usa cuando se conoce el número de iteraciones...",
    "imagen_url": "https://ejemplo.com/apuntes/3f6c2a1e.jpg",
    "fecha_creacion": "2026-09-22T10:15:00Z",
    "fecha_modificacion": "2026-09-22T10:20:00Z",
    "estado": 1
  }
]
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'materia_id' no es válido` | `materia_id` no es un UUID. |

---

### GET /notas/{id}

Devuelve una nota del usuario.

- **Requiere token:** Sí
- **Parámetros de ruta:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Id de la nota. |

- **Cuerpo:** ninguno

**Respuesta `200 OK`:**

```json
{
  "id": "3f6c2a1e-8b7d-4c1a-9e2f-5a0b1c2d3e4f",
  "materia_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
  "titulo": "Estructuras de control",
  "texto": "El bucle for se usa cuando se conoce el número de iteraciones...",
  "imagen_url": null,
  "fecha_creacion": "2026-09-22T10:15:00Z",
  "fecha_modificacion": "2026-09-22T10:20:00Z",
  "estado": 1
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'id' no es válido` | El `id` de la URL no es un UUID. |
| 404 | `Nota no encontrada` | No existe, es de otro usuario o está eliminada. |

---

### POST /notas

Crea una nota dentro de una materia del usuario. El `id` lo genera la app (UUID).

- **Requiere token:** Sí
- **Parámetros:** ninguno
- **Cuerpo:**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `id` | UUID | Sí | No puede existir ya en la base. |
| `materia_id` | UUID | Sí | Debe ser una materia activa del usuario. |
| `titulo` | string | Sí | No vacío, máx. 120 caracteres. |
| `texto` | string | Sí | Puede ser `""`, pero **no** `null`. Máx. 50 000 caracteres. |
| `imagen_url` | string | No | Máx. 500 caracteres. Admite `null`. |
| `fecha_creacion` | fecha ISO | Sí | |
| `fecha_modificacion` | fecha ISO | Sí | |

```json
{
  "id": "9b2f8d4e-1c3a-4e5f-8a7b-6c5d4e3f2a1b",
  "materia_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
  "titulo": "Ciclo de vida de una Activity",
  "texto": "onCreate, onStart, onResume, onPause, onStop, onDestroy",
  "imagen_url": null,
  "fecha_creacion": "2026-09-26T15:45:00Z",
  "fecha_modificacion": "2026-09-26T15:45:00Z"
}
```

**Respuesta `201 Created`:**

```json
{
  "id": "9b2f8d4e-1c3a-4e5f-8a7b-6c5d4e3f2a1b",
  "materia_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
  "titulo": "Ciclo de vida de una Activity",
  "texto": "onCreate, onStart, onResume, onPause, onStop, onDestroy",
  "imagen_url": null,
  "fecha_creacion": "2026-09-26T15:45:00Z",
  "fecha_modificacion": "2026-09-26T15:45:00Z",
  "estado": 1
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `Datos inválidos` (+ `campos`) | Falta un campo obligatorio o no cumple las reglas (título vacío, `texto` nulo, etc.). |
| 400 | `El cuerpo de la petición JSON está mal formado o es inválido` | JSON mal escrito o con valores de formato incorrecto (UUID o fecha inválidos). |
| 404 | `La materia indicada no existe o no te pertenece` | `materia_id` no existe, es de otro usuario o está eliminada. |
| 409 | `La nota ya existe` | Ya hay una nota con ese `id` (de cualquier usuario, incluso si está eliminada). Suele pasar al reintentar un envío. |
| 409 | `Error de integridad en la base de datos` | La base de datos rechazó el registro por una restricción. |

Si el `id` ya existe, se responde `409` aunque la materia tampoco sea válida (se valida primero el `id`).

---

### PUT /notas/{id}

Actualiza una nota. Se reemplazan `titulo`, `texto`, `imagen_url`, `fecha_modificacion` y, si cambia, `materia_id`
(mover la nota a otra materia).

- **Requiere token:** Sí
- **Parámetros de ruta:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Id de la nota a actualizar. |

- **Cuerpo:** los mismos campos y reglas que en [POST /notas](#post-notas). Además:
  - `id` es obligatorio en el cuerpo, pero **se ignora**: siempre se actualiza la nota del `id` de la URL.
  - `fecha_creacion` es obligatoria, pero **se ignora**.
  - `imagen_url` se reemplaza con lo que llegue: si se omite o se manda `null`, la nota queda **sin imagen**.
  - Si `materia_id` es distinto al actual, se valida que la nueva materia sea activa y del usuario.
  - **Control de versiones:** si `fecha_modificacion` es **anterior** a la guardada, se rechaza con `409`.

```json
{
  "id": "9b2f8d4e-1c3a-4e5f-8a7b-6c5d4e3f2a1b",
  "materia_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
  "titulo": "Ciclo de vida de una Activity",
  "texto": "onCreate → onStart → onResume → onPause → onStop → onDestroy",
  "imagen_url": "https://ejemplo.com/apuntes/9b2f8d4e.jpg",
  "fecha_creacion": "2026-09-26T15:45:00Z",
  "fecha_modificacion": "2026-09-26T16:30:00Z"
}
```

**Respuesta `200 OK`:**

```json
{
  "id": "9b2f8d4e-1c3a-4e5f-8a7b-6c5d4e3f2a1b",
  "materia_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
  "titulo": "Ciclo de vida de una Activity",
  "texto": "onCreate → onStart → onResume → onPause → onStop → onDestroy",
  "imagen_url": "https://ejemplo.com/apuntes/9b2f8d4e.jpg",
  "fecha_creacion": "2026-09-26T15:45:00Z",
  "fecha_modificacion": "2026-09-26T16:30:00Z",
  "estado": 1
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'id' no es válido` | El `id` de la URL no es un UUID. |
| 400 | `Datos inválidos` (+ `campos`) | Algún campo no cumple las reglas. |
| 400 | `El cuerpo de la petición JSON está mal formado o es inválido` | JSON mal escrito o con valores de formato incorrecto. |
| 404 | `Nota no encontrada` | No existe, es de otro usuario o está eliminada. |
| 404 | `La materia indicada no existe o no te pertenece` | Se intentó mover la nota a una materia inexistente, eliminada o de otro usuario. |
| 409 | `La versión enviada es más antigua que la guardada` | La `fecha_modificacion` enviada es anterior a la del servidor. |
| 409 | `Error de integridad en la base de datos` | La base de datos rechazó el cambio por una restricción. |

---

### DELETE /notas/{id}

Elimina una nota (**borrado lógico**): la marca como eliminada y pone `fecha_modificacion` en la hora actual
del servidor, para que `GET /sync` informe el borrado.

- **Requiere token:** Sí
- **Parámetros de ruta:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Id de la nota a eliminar. |

- **Cuerpo:** ninguno

**Respuesta `204 No Content`** (sin cuerpo).

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'id' no es válido` | El `id` de la URL no es un UUID. |
| 404 | `Nota no encontrada` | No existe, es de otro usuario o ya estaba eliminada. |

---

## Usuarios

El usuario se crea **automáticamente** la primera vez que llama a cualquier endpoint protegido con un token
válido (ver [Autenticación](#qué-hace-la-api-con-el-token)). Todos los endpoints trabajan sobre el usuario
del token; no hay forma de consultar o modificar a otro usuario.

Objeto **usuario** que devuelve la API:

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | string | UID de Firebase. |
| `correo` | string | Correo de Firebase. No se puede cambiar desde la API. |
| `nombre` | string | Nombre visible (máx. 80). |
| `nombre_usuario` | string | Nombre de usuario único (3–40). |
| `foto_url` | string o `null` | URL de la foto de perfil. |
| `fecha_creacion` | fecha ISO | Fecha de registro en la API. |

### GET /usuarios/me

Devuelve el perfil del usuario autenticado.

- **Requiere token:** Sí
- **Parámetros:** ninguno
- **Cuerpo:** ninguno

**Respuesta `200 OK`:**

```json
{
  "id": "Xk3pQ9rTz1aBcD4eF5gH6iJ7kL8m",
  "correo": "brayan@correo.com",
  "nombre": "Brayan",
  "nombre_usuario": "brayan",
  "foto_url": null,
  "fecha_creacion": "2026-09-20T13:55:00Z"
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 404 | `Usuario no encontrado` | El usuario no está en la base. En la práctica no debería ocurrir, porque el filtro lo crea antes. |

---

### POST /usuarios

Confirma el registro del usuario después de iniciar sesión. El usuario ya fue creado por el filtro de
autenticación, así que este endpoint **solo devuelve el perfil** con código `201`. Siempre responde `201`,
aunque el usuario ya existiera de antes.

- **Requiere token:** Sí
- **Parámetros:** ninguno
- **Cuerpo:** ninguno (si se envía, se ignora)

**Respuesta `201 Created`:**

```json
{
  "id": "Xk3pQ9rTz1aBcD4eF5gH6iJ7kL8m",
  "correo": "brayan@correo.com",
  "nombre": "Usuario Lecto",
  "nombre_usuario": "brayan",
  "foto_url": null,
  "fecha_creacion": "2026-09-26T15:00:00Z"
}
```

(`nombre` sale como `"Usuario Lecto"` cuando el token de Firebase no trae nombre, que es lo normal con
registro por correo y contraseña. Se puede cambiar con `PUT /usuarios/me`.)

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 404 | `Usuario no encontrado` | El usuario no está en la base. En la práctica no debería ocurrir. |

---

### PUT /usuarios/me

Actualiza el perfil del usuario. Se reemplazan `nombre`, `nombre_usuario` y `foto_url`.
El `correo` y el `id` vienen de Firebase y no se pueden cambiar.

- **Requiere token:** Sí
- **Parámetros:** ninguno
- **Cuerpo:**

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `nombre` | string | Sí | No vacío, máx. 80 caracteres. |
| `nombre_usuario` | string | Sí | Entre 3 y 40 caracteres, no puede estar en uso por otro usuario. |
| `foto_url` | string | No | Máx. 500 caracteres. Si se omite o se manda `null`, **se borra la foto actual**. |

Mandar el mismo `nombre_usuario` que ya se tiene no da error.

```json
{
  "nombre": "Brayan Fonseca",
  "nombre_usuario": "brayitan",
  "foto_url": "https://ejemplo.com/fotos/brayan.jpg"
}
```

**Respuesta `200 OK`:**

```json
{
  "id": "Xk3pQ9rTz1aBcD4eF5gH6iJ7kL8m",
  "correo": "brayan@correo.com",
  "nombre": "Brayan Fonseca",
  "nombre_usuario": "brayitan",
  "foto_url": "https://ejemplo.com/fotos/brayan.jpg",
  "fecha_creacion": "2026-09-20T13:55:00Z"
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `Datos inválidos` (+ `campos`) | Algún campo no cumple las reglas (nombre vacío, `nombre_usuario` muy corto o muy largo, etc.). |
| 400 | `El cuerpo de la petición JSON está mal formado o es inválido` | JSON mal escrito. |
| 404 | `Usuario no encontrado` | El usuario no está en la base. |
| 409 | `El nombre de usuario ya está en uso` | Otro usuario ya tiene ese `nombre_usuario`. |
| 409 | `Error de integridad en la base de datos` | La base rechazó el cambio (por ejemplo, otro usuario tomó el mismo `nombre_usuario` al mismo tiempo). |

---

### DELETE /usuarios/me

Elimina la cuenta **por completo** (borrado físico, no lógico):

1. Borra de la base, en una sola transacción, todas las notas del usuario, luego sus materias y luego el usuario.
   Si algo falla en este paso, no se borra nada.
2. Cuando esa transacción ya terminó, borra el usuario en Firebase Authentication.

Esta operación no se puede deshacer.

- **Requiere token:** Sí
- **Parámetros:** ninguno
- **Cuerpo:** ninguno

**Respuesta `204 No Content`** (sin cuerpo).

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 404 | `Usuario no encontrado` | El usuario no está en la base. |
| 500 | `Los datos de la cuenta ya se borraron, pero no se pudo eliminar el usuario en Firebase` | Los datos de la base se borraron, pero falló el borrado en Firebase. La cuenta de Firebase sigue existiendo: si el usuario vuelve a entrar, se le crea un registro nuevo y vacío. |

---

## Sincronización

### GET /sync

Devuelve los cambios del usuario para que la app mantenga alineada su base local con el servidor.

- **Requiere token:** Sí
- **Parámetros de consulta (query):**

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `desde` | fecha ISO-8601 | No | Fecha de la última sincronización (el `sincronizado_en` de la respuesta anterior). |

- **Cuerpo:** ninguno

Comportamiento:

- **Sin `desde`** (primera sincronización): devuelve todas las materias y notas **activas** del usuario.
  Las materias vienen ordenadas por nombre y las notas de la más reciente a la más antigua.
- **Con `desde`**: devuelve las materias y notas cuya `fecha_modificacion` sea **estrictamente posterior** a `desde`,
  **incluidas las eliminadas** (con `estado: 0`), para que la app las borre de su base local.
  En este caso no hay un orden garantizado.
- `sincronizado_en` es la hora del servidor tomada **antes** de consultar. La app debe guardarla y enviarla
  como `desde` en la siguiente sincronización.
- En esta respuesta, `cantidad_notas` de las materias **siempre es `0`**: la app lo calcula con su base local.

Si `desde` lleva zona horaria con `+`, hay que codificarlo en la URL (`%2B`); lo más simple es usar siempre UTC con `Z`.

**Ejemplos:**

```
GET /sync
GET /sync?desde=2026-09-26T15:00:00Z
```

**Respuesta `200 OK`:**

```json
{
  "materias": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "nombre": "Cálculo Integral",
      "color": "#E94E77",
      "icono": "calculadora",
      "fecha_creacion": "2026-09-26T15:30:00Z",
      "fecha_modificacion": "2026-09-26T16:10:00Z",
      "estado": 1,
      "cantidad_notas": 0
    }
  ],
  "notas": [
    {
      "id": "3f6c2a1e-8b7d-4c1a-9e2f-5a0b1c2d3e4f",
      "materia_id": "a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
      "titulo": "Estructuras de control",
      "texto": "",
      "imagen_url": null,
      "fecha_creacion": "2026-09-22T10:15:00Z",
      "fecha_modificacion": "2026-09-26T16:40:12.345678Z",
      "estado": 0
    }
  ],
  "sincronizado_en": "2026-09-26T16:45:00.123456Z"
}
```

**Errores** (además de los comunes):

| Código | Mensaje | Significado |
|---|---|---|
| 400 | `El valor de 'desde' no es válido` | `desde` no es una fecha ISO-8601 válida. |

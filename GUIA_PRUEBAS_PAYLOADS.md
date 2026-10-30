# Guía de Payloads para Pruebas (Postman, cURL y Swagger)

Colección estructurada de solicitudes y cuerpos JSON para validar el comportamiento funcional, los DTOs de petición y las reglas de negocio de la API refactorizada con arquitectura REST profesional, **sin `@PathVariable` ni `@RequestParam`** y con envoltura unificada `GenericResponse<T>`.

**Formato Estándar de Respuesta (`GenericResponse<T>`):**

```json
{
  "codigo": 0,
  "mensaje": "Operación exitosa",
  "datos": { ... },
  "errores": []
}
```

_(En caso de error, `codigo: 1`, `mensaje: "Descripción del error"` y `errores` contiene el detalle de validación o causa)._

---

## 1. Autenticación y Generación de Token JWT

### 1.1 Inicio de Sesión

- **Método:** `POST`
- **URL:** `/api/v1/auth/login`
- **Cabeceras:** `Content-Type: application/json`

```json
{
  "correo": "carlos.mendoza@bancoprueba.com",
  "password": "Password123!"
}
```

_Respuesta esperada (HTTP 200 OK):_

```json
{
  "codigo": 0,
  "mensaje": "Autenticación exitosa",
  "datos": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "tipo": "Bearer",
    "usuarioId": 1,
    "clienteId": 1,
    "correo": "carlos.mendoza@bancoprueba.com",
    "expiracionMs": 86400000
  },
  "errores": []
}
```

> **Nota:** Para los endpoints protegidos, incluir la cabecera:
> `Authorization: Bearer <TOKEN>`

---

## 2. Clientes (`/clientes`)

### 2.1 Registro Integral de Cliente

- **Método:** `POST`
- **URL:** `/api/v1/clientes`
- **Cabeceras:** `Content-Type: application/json`
- **Código Esperado:** HTTP 201 Created

```json
w
```

### 2.2 Consulta Dinámica con Filtros (CURP, RFC, etc.) y Paginación

- **Método:** `POST`
- **URL:** `/api/v1/clientes/consultar`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

**Ejemplo 1: Filtrar por CURP (completa o prefijo parcial)**

```json
{
  "curp": "MEGC940820HDFRRN01",
  "soloActivos": true,
  "paginacion": {
    "pagina": 0,
    "tamano": 10
  }
}
```

**Ejemplo 2: Filtrar por RFC (completo o prefijo parcial)**

```json
{
  "rfc": "MEGC940820AB1",
  "soloActivos": true,
  "paginacion": {
    "pagina": 0,
    "tamano": 10
  }
}
```

**Ejemplo 3: Combinar CURP, RFC, Nombre y Paginación avanzada**

```json
{
  "curp": "MEGC",
  "rfc": "MEGC",
  "nombre": "Carlos",
  "soloActivos": true,
  "paginacion": {
    "pagina": 0,
    "tamano": 10,
    "ordenarPor": "fechaCreacion",
    "direccion": "DESC"
  }
}
```

### 2.3 Detalle de Cliente por ID (Sin `@PathVariable`)

- **Método:** `POST`
- **URL:** `/api/v1/clientes/detalle`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "clienteId": 1
}
```

### 2.4 Actualización Parcial de Cliente

- **Método:** `PATCH`
- **URL:** `/api/v1/clientes`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "clienteId": 1,
  "telefonoMovil": "5588990011",
  "ocupacion": "Socio Consultor",
  "ingresoMensual": 55000.0,
  "domicilio": {
    "calle": "Av. Insurgentes Sur",
    "numeroExterior": "1500",
    "colonia": "Crédito Constructor",
    "municipio": "Benito Juárez",
    "estado": "Ciudad de México",
    "codigoPostal": "03940",
    "pais": "México"
  }
}
```

### 2.5 Baja Lógica de Cliente

- **Método:** `DELETE`
- **URL:** `/api/v1/clientes`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "clienteId": 1
}
```

---

## 3. Cuentas Bancarias (`/cuentas`)

### 3.1 Apertura de Cuenta Adicional

- **Método:** `POST`
- **URL:** `/api/v1/cuentas`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "clienteId": 1,
  "saldoInicial": 2500.0
}
```

### 3.2 Consulta Dinámica de Cuentas (Filtros y Paginación)

- **Método:** `POST`
- **URL:** `/api/v1/cuentas/consultar`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "clienteId": 1,
  "estatus": "ACTIVA",
  "paginacion": {
    "pagina": 0,
    "tamano": 10
  }
}
```

### 3.3 Detalle de Cuenta por Número

- **Método:** `POST`
- **URL:** `/api/v1/cuentas/detalle`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "numeroCuenta": "0128048804464"
}
```

### 3.4 Consulta de Saldo por Número de Cuenta

- **Método:** `POST`
- **URL:** `/api/v1/cuentas/saldo`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "numeroCuenta": "0128048804464"
}
```

### 3.5 Actualización de Estatus Operativo (Bloqueo / Activación)

- **Método:** `PATCH`
- **URL:** `/api/v1/cuentas/estatus`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "numeroCuenta": "0128048804464",
  "estatus": "BLOQUEADA"
}
```

---

## 4. Usuarios (`/usuarios`)

### 4.1 Registrar Usuario de Acceso para Cliente

- **Método:** `POST`
- **URL:** `/api/v1/usuarios`
- **Cabeceras:** `Content-Type: application/json`

```json
{
  "clienteId": 1,
  "username": "carlos.mendoza",
  "password": "Password123!"
}
```

### 4.2 Consultar Usuarios (Filtros y Paginación)

- **Método:** `POST`
- **URL:** `/api/v1/usuarios/consultar`
- **Cabeceras:** `Authorization: Bearer <TOKEN>`, `Content-Type: application/json`

```json
{
  "username": "carlos",
  "activo": true,
  "paginacion": {
    "pagina": 0,
    "tamano": 10
  }
}
```

---

## 5. Módulo Personas (`/personas`)

### 5.1 Crear Persona

- **Método:** `POST`
- **URL:** `/api/v1/personas`
- **Cabeceras:** `Content-Type: application/json`

```json
{
  "nombre": "Alejandro",
  "paterno": "González",
  "materno": "Morales",
  "fechaNacimiento": "1990-11-15",
  "activo": true
}
```

### 5.2 Consultar Personas con Filtros

- **Método:** `POST`
- **URL:** `/api/v1/personas/consultar`
- **Cabeceras:** `Content-Type: application/json`

```json
{
  "nombre": "Alejandro",
  "activo": true,
  "paginacion": {
    "pagina": 0,
    "tamano": 10
  }
}
```

### 5.3 Actualizar Persona

- **Método:** `PUT`
- **URL:** `/api/v1/personas`
- **Cabeceras:** `Content-Type: application/json`

```json
{
  "id": 1,
  "nombre": "Alejandro",
  "paterno": "González",
  "materno": "Morales",
  "fechaNacimiento": "1990-11-15",
  "activo": true
}
```

### 5.4 Eliminar Persona (Identificador en Body)

- **Método:** `DELETE`
- **URL:** `/api/v1/personas`
- **Cabeceras:** `Content-Type: application/json`

```json
{
  "id": 1
}
```

---

## 6. Módulo GestoPago (`/gestopago`)

### 6.1 Obtener Catálogo de Servicios y Recargas

- **Método:** `GET`
- **URL:** `/api/v1/gestopago/productos`
- **Respuesta:** `GenericResponse<GestoPagoProductListResponse>`

---

## 7. Módulo Transacciones y Pagos (`/transacciones`)

### 7.1 Pago de Servicio o Recarga Electrónica (`sendTx`)
- **Método:** `POST`
- **URL:** `/api/v1/transacciones/pago-servicio`
- **Cabeceras:**
  - `Authorization: Bearer <TOKEN_JWT>`
  - `Idempotency-Key: <UUID_ALEATORIO>` (Ej. `550e8400-e29b-41d4-a716-446655440000`)
  - `Content-Type: application/json`

#### Ejemplo 1: Recarga Telefónica (tipoFront = 1)
> El monto debe coincidir exactamente con el valor del paquete en catálogo. Comisión al cliente: $0.00.
```json
{
  "numeroCuenta": "01200000000000000016",
  "idProducto": 582,
  "idServicio": 133,
  "telefonoReferencia": "5555555551",
  "monto": 10.00,
  "pin": "1234"
}
```

#### Ejemplo 2: Pago de Servicio (tipoFront = 2)
> El cliente especifica el monto a liquidar del recibo. El débito total incluirá la comisión configurada en catálogo.
```json
{
  "numeroCuenta": "01200000000000000016",
  "idProducto": 205,
  "idServicio": 76,
  "telefonoReferencia": "257203810099",
  "monto": 150.00,
  "pin": "1234"
}
```

#### Códigos de Respuesta HTTP Documentados:
- `200 OK`: Transacción procesada y confirmada exitosamente con el proveedor.
- `202 ACCEPTED`: Transacción en estado incierto (timeout/código 82). Se envía a conciliación automática (`confirmTx`).
- `400 BAD REQUEST`: Error de validación en los datos del body o cabeceras faltantes.
- `401 UNAUTHORIZED`: Token JWT inválido o no suministrado.
- `403 FORBIDDEN`: PIN transaccional incorrecto.
- `404 NOT FOUND`: Cuenta ajena/inexistente o producto no encontrado en catálogo.
- `409 CONFLICT`: Conflicto por clave de idempotencia duplicada con parámetros distintos o en proceso.
- `422 UNPROCESSABLE ENTITY`: Saldo insuficiente en cuenta o monto fuera de reglas de catálogo.
- `423 LOCKED`: Cuenta bloqueada temporalmente (15 min) por rebasar 3 intentos fallidos de PIN.
- `502 BAD GATEWAY`: Rechazo definitivo por la pasarela de pagos (el saldo es automáticamente reembolsado).
- `503 SERVICE UNAVAILABLE`: Pasarela externa o catálogo temporalmente no disponibles.

---

### 7.2 Consultar Estado de una Transacción
- **Método:** `GET`
- **URL:** `/api/v1/transacciones/{id}`
- **Cabeceras:** `Authorization: Bearer <TOKEN_JWT>`
- **Respuesta:** `GenericResponse<PagoServicioResponse>` (Estado `EXITOSA`, `EN_PROCESO`, `FALLIDA`, `REVERSADA`)


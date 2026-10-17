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
*(En caso de error, `codigo: 1`, `mensaje: "Descripción del error"` y `errores` contiene el detalle de validación o causa).*

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

*Respuesta esperada (HTTP 200 OK):*
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
{
  "nombre": "Carlos",
  "segundoNombre": "Eduardo",
  "apellidoPaterno": "Mendoza",
  "apellidoMaterno": "Gómez",
  "fechaNacimiento": "1994-08-20",
  "curp": "MEGC940820HDFRRN01",
  "rfc": "MEGC940820AB1",
  "sexo": "H",
  "nacionalidad": "Mexicana",
  "estadoCivil": "SOLTERO",
  "correo": "carlos.mendoza@bancoprueba.com",
  "telefonoMovil": "5544332211",
  "telefonoAlternativo": "5599887766",
  "domicilio": {
    "calle": "Av. Paseo de la Reforma",
    "numeroExterior": "222",
    "numeroInterior": "Piso 8",
    "colonia": "Juárez",
    "municipio": "Cuauhtémoc",
    "estado": "Ciudad de México",
    "codigoPostal": "06600",
    "pais": "México"
  },
  "ocupacion": "Contador Público",
  "empresa": "Despacho Financiero SC",
  "ingresoMensual": 38500.00,
  "password": "Password123!"
}
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
  "ingresoMensual": 55000.00,
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
  "saldoInicial": 2500.00
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

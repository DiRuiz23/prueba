# Documentación Técnica: API de Onboarding Bancario y Gestión de Cuentas

Sistema backend empresarial para el proceso integral de alta (onboarding), autenticación segura mediante JWT, administración de cuentas bancarias y consultas de clientes personas físicas.

Desplegado en producción en Render Cloud con base de datos gestionada PostgreSQL y migraciones automáticas mediante Flyway.

---

## 1. Enlaces de Acceso y Entorno de Producción

- **URL Base de la API (Render):** `https://banco-onboarding-api.onrender.com/api/v1`
- **Documentación Interactiva Swagger UI:** https://banco-onboarding-api.onrender.com/api/v1/swagger-ui/index.html
- **Especificación OpenAPI (JSON):** https://banco-onboarding-api.onrender.com/api/v1/v3/api-docs

---

## 2. Stack Tecnológico

| Componente | Tecnología y Versión | Justificación Técnica |
| :--- | :--- | :--- |
| **Lenguaje** | Java 17 (LTS) | Soporte a largo plazo, compatibilidad con records y alto rendimiento. |
| **Framework** | Spring Boot 3.3.6 | Módulos WebMVC, Data JPA, Security 6, Validation y Actuator. |
| **Base de Datos** | PostgreSQL 16 | Motor relacional transaccional robusto en Render Managed Database. |
| **Migraciones** | Flyway 10.x | Versionado automatizado de scripts de base de datos (V1, V2 y V3). |
| **Seguridad** | Spring Security + JJWT 0.12.6 | Tokens Bearer firmados con HMAC-SHA512 y contraseñas cifradas en BCrypt. |
| **Documentación** | Springdoc OpenAPI 2.2.0 | Generación dinámica de la interfaz Swagger UI y especificación OAS3. |
| **Contenedor** | Docker (Alpine Linux) | Construcción multi-etapa con Temurin JRE 17, optimizada para 512 MB RAM. |
| **Plataforma** | Render Cloud | Despliegue continuo (CD) integrado directamente con repositorio GitHub. |

---

## 3. Arquitectura del Sistema

El proyecto implementa una arquitectura en capas desacoplada y orientada al dominio:

- **Capa de Controladores (Controller):** Exposición de endpoints REST, recepción de solicitudes HTTP y mapeo de respuestas estándar.
- **Capa de Negocio (Service):** Implementación de lógica bancaria, validaciones de mayoría de edad, catálogos geográficos y transacciones atómicas (`@Transactional`).
- **Capa de Persistencia (Repository):** Interfaces Spring Data JPA con consultas JPQL para búsquedas por CURP, RFC, cuenta y rangos de fechas.
- **Capa de Datos (Entity / Model):** Mapeo objeto-relacional (ORM) y objetos de transferencia de datos (DTO) con validaciones declarativas Bean Validation.
- **Capa Transversal (Security & Exception Handling):** Filtro de autenticación JWT y manejador global de excepciones (`GlobalExceptionHandler`).

---

## 4. Catálogo de Endpoints

Todos los servicios se encuentran expuestos bajo el prefijo común: `/api/v1`.

### 4.1 Autenticación (`/auth`)

| Método | Endpoint | Nivel de Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Público | Autentica credenciales (correo y contraseña). Retorna token JWT Bearer. |

#### Ejemplo de Petición (`POST /api/v1/auth/login`):
```json
{
  "correo": "juan.perez@ejemplo.com",
  "password": "Password123!"
}
```

#### Ejemplo de Respuesta (HTTP 200 OK):
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tipo": "Bearer",
  "usuarioId": 1,
  "clienteId": 1,
  "correo": "juan.perez@ejemplo.com",
  "expiracionMs": 86400000
}
```

---

### 4.2 Clientes (`/clientes`)

| Método | Endpoint | Nivel de Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/clientes` | Público | Registro integral: crea Cliente, Domicilio, Cuenta automática ($1,000.00), Usuario con password cifrada y Biometría. |
| `GET` | `/clientes` | Protegido (JWT) | Listado general de clientes con soporte para filtros por query param: `nombre`, `apellidoPaterno`, `apellidoMaterno`, `curp`, `rfc`, `numeroCuenta`, `soloActivos`, `fechaInicio`, `fechaFin`. |
| `GET` | `/clientes/{id}` | Protegido (JWT) | Consulta individual de cliente por identificador único. |
| `PATCH` | `/clientes/{id}` | Protegido (JWT) | Actualización selectiva de datos de contacto, ocupación o domicilio. CURP y RFC se mantienen inmutables. |
| `DELETE` | `/clientes/{id}` | Protegido (JWT) | Baja lógica: marca cliente como inactivo, congela cuentas a INACTIVA y bloquea acceso a usuarios. |

#### Ejemplo de Registro Integral (`POST /api/v1/clientes`):
```json
{
  "nombre": "Juan",
  "segundoNombre": "Carlos",
  "apellidoPaterno": "Pérez",
  "apellidoMaterno": "López",
  "fechaNacimiento": "1990-05-15",
  "curp": "PELJ900515HDFRRN09",
  "rfc": "PELJ900515AB1",
  "sexo": "H",
  "nacionalidad": "Mexicana",
  "estadoCivil": "SOLTERO",
  "correo": "juan.perez@ejemplo.com",
  "telefonoMovil": "5512345678",
  "telefonoAlternativo": "5587654321",
  "domicilio": {
    "calle": "Av. Insurgentes Sur",
    "numeroExterior": "1602",
    "numeroInterior": "Piso 4",
    "colonia": "Crédito Constructor",
    "municipio": "Benito Juárez",
    "estado": "Ciudad de México",
    "codigoPostal": "03940",
    "pais": "México"
  },
  "ocupacion": "Ingeniero de Software",
  "empresa": "Tech Solutions",
  "ingresoMensual": 45000.00,
  "password": "Password123!"
}
```

---

### 4.3 Cuentas Bancarias (`/cuentas`)

| Método | Endpoint | Nivel de Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/cuentas` | Protegido (JWT) | Creación de cuenta bancaria adicional para cliente activo. |
| `GET` | `/cuentas/{numeroCuenta}` | Protegido (JWT) | Consulta de cuenta por número bancario (13 dígitos). |
| `GET` | `/cuentas/{numeroCuenta}/saldo` | Protegido (JWT) | Consulta inmediata de saldo disponible. |
| `GET` | `/cuentas?clienteId={id}` | Protegido (JWT) | Consulta de todas las cuentas asociadas a un cliente. |
| `GET` | `/cuentas?estatus={estatus}` | Protegido (JWT) | Filtro de cuentas por estatus (ACTIVA, BLOQUEADA, INACTIVA). |
| `PATCH` | `/cuentas/{numeroCuenta}` | Protegido (JWT) | Modificación de estatus operativo de la cuenta. |

#### Ejemplo de Apertura de Cuenta Adicional (`POST /api/v1/cuentas`):
```json
{
  "clienteId": 1,
  "saldoInicial": 5000.00
}
```

---

### 4.4 Usuarios (`/usuarios`)

| Método | Endpoint | Nivel de Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/usuarios/filtro` | Protegido (JWT) | Consulta de usuarios con filtros por clienteId y estatus activo. No expone contraseñas cifradas. |
| `PUT` | `/usuarios/agregar` | Protegido (JWT) | Asignación de credenciales de usuario a clientes que no cuenten con usuario asociado. |

---

## 5. Reglas de Negocio y Validaciones

1. **Mayoría de Edad:** El cliente debe tener al menos 18 años cumplidos al momento del registro (retorna HTTP 422 si es menor).
2. **Unicidad de Datos:** No se permite duplicidad de CURP, RFC ni Correo Electrónico (retorna HTTP 409).
3. **Formatos Oficiales Obligatorios:**
   - **CURP:** Exactamente 18 caracteres alfanuméricos con estructura oficial RENAPO.
   - **RFC:** 12 o 13 caracteres alfanuméricos con homoclave SAT.
   - **Sexo:** Valores permitidos: H, M, Indefinido.
   - **Nacionalidad:** Exclusivamente Mexicana.
   - **Teléfonos:** Longitud estricta de 10 dígitos numéricos.
   - **Contraseña:** Mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número y 1 carácter especial.
4. **Validación Geográfica contra Catálogos (México):**
   - El país debe existir en el catálogo (México).
   - El estado debe ser una de las 32 entidades federativas cargadas en base de datos.
   - El código postal y municipio deben corresponder formalmente con el estado registrado.
5. **Generación Automática de Cuenta:** Al dar de alta un cliente, se genera automáticamente una cuenta bancaria con prefijo 012 + 10 dígitos aleatorios únicos y saldo inicial de $1,000.00 MXN.
6. **Inmutabilidad Fiscal en Actualizaciones:** El servicio ignora intentos de modificar CURP y RFC en peticiones de tipo PATCH.
7. **Baja Lógica y Cascada Operativa:** Al ejecutar DELETE en un cliente, este pasa a estatus inactivo, todas sus cuentas bancarias se congelan automáticamente a INACTIVA y su usuario de acceso queda bloqueado para login.

---

## 6. Códigos de Estado HTTP y Manejo de Errores

El sistema utiliza respuestas estructuradas mediante un manejador centralizado de excepciones:

| Código HTTP | Estado | Causa en el Sistema |
| :--- | :--- | :--- |
| **200** | OK | Petición procesada exitosamente con datos de retorno. |
| **201** | Created | Recurso creado exitosamente (registro de cliente o cuenta). |
| **204** | No Content | Operación completada exitosamente sin contenido en el cuerpo (baja lógica). |
| **400** | Bad Request | Error en formato de datos (sintaxis JSON, regex no cumplida, parámetros inválidos). |
| **401** | Unauthorized | Credenciales inválidas durante el inicio de sesión. |
| **403** | Forbidden | Petición sin token JWT, token no válido o usuario inactivo. |
| **404** | Not Found | Cliente, cuenta bancaria o usuario no localizado. |
| **409** | Conflict | Recurso duplicado (CURP, RFC, correo existente o usuario ya asignado). |
| **422** | Unprocessable Entity | Regla de negocio no cumplida (menor de edad, catálogos no coincidentes, cliente inactivo). |

#### Formato Estándar de Respuesta de Error:
```json
{
  "mensaje": "El cliente debe ser mayor de edad (18 años cumplidos).",
  "status": 422,
  "timestamp": "2026-10-09T17:30:00"
}
```

---

## 7. Resultados de Pruebas Automatizadas

Se ejecutó una batería completa de pruebas automatizadas mediante validación de payloads que cubrió tanto el flujo principal como todos los escenarios límite:

```text
RESUMEN DE PRUEBAS
Pruebas Aprobadas: 63
Pruebas Fallidas:   0
Total Ejecutadas:  63 (100% de Aprobación)
```

### Detalle de Módulos Validados:
1. **Registro de Clientes (3/3):** Registro estándar, creación automática de cuenta y usuario, caracteres especiales (acentos y letra eñe).
2. **Validaciones y Reglas de Negocio (19/19):** CURP, RFC y correo duplicados, menor de edad, fecha futura, teléfono inválido, contraseña débil, validación contra catálogos geográficos de las 32 entidades federativas.
3. **Seguridad y JWT (5/5):** Endpoints protegidos, generación de JWT, contraseñas erróneas, usuario inexistente y firmas inválidas.
4. **Consultas de Clientes (11/11):** Búsqueda por ID, CURP, RFC, nombres, número de cuenta, rango de fechas y filtro de activos.
5. **Actualizaciones Parciales (4/4):** Modificación de domicilio, inmutabilidad de datos fiscales, validación de teléfono y correo en uso.
6. **Gestión de Cuentas (10/10):** Consulta de cuentas, saldo, cuentas por cliente, estatus, apertura de cuenta adicional, bloqueo y reactivación.
7. **Gestión de Usuarios (5/5):** Consultas con omisión de hashes de contraseña y prevención de duplicidad de accesos.
8. **Baja Lógica (6/6):** Desactivación de cliente, congelamiento automático de cuentas a INACTIVA, denegación de autenticación y bloqueo de nuevas operaciones.

---

## 8. Guía de Ejecución Local

### Prerrequisitos:
- Java JDK 17 instalado
- PostgreSQL 14 o superior
- Git

### Procedimiento:
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/DiRuiz23/prueba.git
   cd prueba
   ```
2. Configurar la conexión en `src/main/resources/application.properties` o mediante variables de entorno:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/gestopago_db
   spring.datasource.username=postgres
   spring.datasource.password=TuPassword
   ```
3. Ejecutar la aplicación:
   ```bash
   ./gradlew bootRun
   ```
4. Consultar en el navegador:
   - Base API: `http://localhost:8000/api/v1`
   - Swagger UI: `http://localhost:8000/api/v1/swagger-ui/index.html`

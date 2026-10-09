-- =====================================================================
-- MIGRACIÓN V2: ONBOARDING INTEGRAL DE CLIENTES, CUENTAS Y USUARIOS
-- (Incluye Catálogos e Historial)
-- =====================================================================

-- 0. CATÁLOGOS (PAÍSES, ESTADOS, CÓDIGOS POSTALES)
CREATE TABLE IF NOT EXISTS cat_paises (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo_iso VARCHAR(3) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cat_estados (
    id SERIAL PRIMARY KEY,
    pais_id INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    CONSTRAINT fk_estado_pais FOREIGN KEY (pais_id) REFERENCES cat_paises(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS cat_codigos_postales (
    id SERIAL PRIMARY KEY,
    estado_id INT NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    CONSTRAINT fk_cp_estado FOREIGN KEY (estado_id) REFERENCES cat_estados(id) ON DELETE RESTRICT
);

-- INSERCIONES INICIALES DE CATÁLOGOS BÁSICOS
INSERT INTO cat_paises (nombre, codigo_iso) VALUES ('México', 'MEX') ON CONFLICT DO NOTHING;
INSERT INTO cat_estados (pais_id, nombre) VALUES (1, 'Ciudad de México'), (1, 'Jalisco'), (1, 'Nuevo León') ON CONFLICT DO NOTHING;

-- 1. TABLA CLIENTES
CREATE TABLE IF NOT EXISTS clientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    curp VARCHAR(18) NOT NULL,
    rfc VARCHAR(13) NOT NULL,
    sexo VARCHAR(20) NOT NULL,
    nacionalidad VARCHAR(50) NOT NULL,
    estado_civil VARCHAR(50) NOT NULL,
    correo VARCHAR(100) NOT NULL,
    telefono_movil VARCHAR(10) NOT NULL,
    telefono_alternativo VARCHAR(10),
    ocupacion VARCHAR(100) NOT NULL,
    empresa VARCHAR(100) NOT NULL,
    ingreso_mensual NUMERIC(15, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Restricciones de Integridad
    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo),
    CONSTRAINT ck_clientes_ingreso_positivo CHECK (ingreso_mensual > 0)
);

-- Índices de Rendimiento para Búsquedas Frecuentes
CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes(correo);
CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre);
CREATE INDEX IF NOT EXISTS idx_clientes_apellidos ON clientes(apellido_paterno, apellido_materno);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);
CREATE INDEX IF NOT EXISTS idx_clientes_fecha_creacion ON clientes(fecha_creacion);

-- 2. TABLA DOMICILIOS
CREATE TABLE IF NOT EXISTS domicilios (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    calle VARCHAR(100) NOT NULL,
    numero_exterior VARCHAR(20) NOT NULL,
    numero_interior VARCHAR(20),
    colonia VARCHAR(100) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    estado VARCHAR(100) NOT NULL,
    codigo_postal VARCHAR(5) NOT NULL,
    pais VARCHAR(50) NOT NULL,

    CONSTRAINT fk_domicilio_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_domicilio_cliente UNIQUE (cliente_id)
);

CREATE INDEX IF NOT EXISTS idx_domicilios_cliente_id ON domicilios(cliente_id);

-- 3. TABLA CUENTAS
CREATE TABLE IF NOT EXISTS cuentas (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    numero_cuenta VARCHAR(20) NOT NULL,
    saldo NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    estatus VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cuenta_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT ck_cuentas_saldo_no_negativo CHECK (saldo >= 0.00),
    CONSTRAINT ck_cuentas_estatus_valido CHECK (estatus IN ('ACTIVA', 'BLOQUEADA', 'INACTIVA'))
);

CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero_cuenta ON cuentas(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_estatus ON cuentas(estatus);

-- 4. TABLA USUARIOS (ACCESO AL SISTEMA)
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    correo VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT uq_usuarios_cliente UNIQUE (cliente_id)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON usuarios(correo);

-- 5. TABLA DATOS BIOMÉTRICOS Y RECONOCIMIENTO FACIAL
CREATE TABLE IF NOT EXISTS datos_biometricos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    foto_facial TEXT NOT NULL,                  -- Imagen en Base64 o Template Binario
    facial_hash VARCHAR(64) NOT NULL,           -- Hash SHA-256 para verificación de integridad
    facial_features TEXT,                       -- JSON de landmarks / scores de liveness
    proveedor_reconocimiento VARCHAR(50),      -- Ej: 'AWS_REKOGNITION', 'FACE_API', 'INTERNO'
    confianza_coincidencia NUMERIC(5, 2),       -- Porcentaje de confianza del reconocimiento (ej. 99.85%)
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_captura TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_biometria_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT uq_biometria_cliente UNIQUE (cliente_id)
);

CREATE INDEX IF NOT EXISTS idx_biometria_cliente_id ON datos_biometricos(cliente_id);

-- 6. TABLA HISTORIAL DE BLOQUEO DE CUENTAS
CREATE TABLE IF NOT EXISTS historial_bloqueo_cuenta (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    estatus_anterior VARCHAR(20) NOT NULL,
    estatus_nuevo VARCHAR(20) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_historial_cuenta FOREIGN KEY (cuenta_id) 
        REFERENCES cuentas(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_historial_cuenta_id ON historial_bloqueo_cuenta(cuenta_id);
-- =====================================================================
-- MIGRACIÓN V4: TABLA DE TRANSACCIONES DE SERVICIOS Y PIN EN CUENTAS
-- =====================================================================

-- 1. Agregar columna opcional de PIN para autorización transaccional en cuentas
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS pin VARCHAR(255);

-- 2. Tabla de Transacciones y Pagos de Servicios (Auditoría e Idempotencia)
CREATE TABLE IF NOT EXISTS transacciones_servicios (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    upc VARCHAR(50) NOT NULL,
    id_producto INT NOT NULL,
    id_servicio INT NOT NULL,
    tipo_front INT NOT NULL DEFAULT 1,
    telefono_referencia VARCHAR(100) NOT NULL,
    monto NUMERIC(15, 2) NOT NULL,
    saldo_anterior NUMERIC(15, 2) NOT NULL,
    saldo_posterior NUMERIC(15, 2),
    id_tx_gestopago VARCHAR(60),
    numero_autorizacion VARCHAR(60),
    codigo_respuesta VARCHAR(20),
    mensaje_respuesta VARCHAR(255),
    estatus VARCHAR(30) NOT NULL,
    fecha_transaccion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaccion_cuenta FOREIGN KEY (cuenta_id)
        REFERENCES cuentas(id) ON DELETE RESTRICT,
    CONSTRAINT uq_transacciones_upc UNIQUE (upc)
);

CREATE INDEX IF NOT EXISTS idx_transacciones_cuenta_id ON transacciones_servicios(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_transacciones_upc ON transacciones_servicios(upc);
CREATE INDEX IF NOT EXISTS idx_transacciones_fecha ON transacciones_servicios(fecha_transaccion);

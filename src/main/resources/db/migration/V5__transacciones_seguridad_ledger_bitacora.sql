-- =====================================================================
-- MIGRACIÓN V5: SEGURIDAD DE PIN, EXTENSIÓN TRANSACCIONAL, LEDGER Y BITÁCORA
-- =====================================================================

-- 1. SEGURIDAD DE PIN EN CUENTAS
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'cuentas' AND column_name = 'pin'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'cuentas' AND column_name = 'pin_hash'
    ) THEN
        ALTER TABLE cuentas RENAME COLUMN pin TO pin_hash;
    ELSIF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'cuentas' AND column_name = 'pin_hash'
    ) THEN
        ALTER TABLE cuentas ADD COLUMN pin_hash VARCHAR(255);
    END IF;
END $$;

ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS intentos_fallidos_pin INT NOT NULL DEFAULT 0;
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS bloqueo_pin_hasta TIMESTAMPTZ;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_cuentas_intentos_pin'
    ) THEN
        ALTER TABLE cuentas ADD CONSTRAINT chk_cuentas_intentos_pin CHECK (intentos_fallidos_pin >= 0);
    END IF;
END $$;


-- 2. EXTENSIÓN DE TRANSACCIONES DE SERVICIOS
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS hora_local VARCHAR(14);
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(64);
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS usuario_id BIGINT;
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS comision NUMERIC(15, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS monto_debitado NUMERIC(15, 2);
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS intentos_confirmacion INT NOT NULL DEFAULT 0;
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS proxima_confirmacion_at TIMESTAMPTZ;
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS fecha_limite_conciliacion TIMESTAMPTZ;
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE transacciones_servicios ADD COLUMN IF NOT EXISTS fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Llave foránea a usuarios
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_transaccion_usuario'
    ) THEN
        ALTER TABLE transacciones_servicios
            ADD CONSTRAINT fk_transaccion_usuario
            FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT;
    END IF;
END $$;

-- Validaciones de estado y valores
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_transaccion_estatus'
    ) THEN
        ALTER TABLE transacciones_servicios
            ADD CONSTRAINT chk_transaccion_estatus
            CHECK (estatus IN ('PENDIENTE', 'EXITOSA', 'FALLIDA', 'EN_CONCILIACION', 'REVERSADA', 'REVISION_MANUAL'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_transaccion_monto'
    ) THEN
        ALTER TABLE transacciones_servicios
            ADD CONSTRAINT chk_transaccion_monto CHECK (monto > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_transaccion_comision'
    ) THEN
        ALTER TABLE transacciones_servicios
            ADD CONSTRAINT chk_transaccion_comision CHECK (comision >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_transaccion_intentos'
    ) THEN
        ALTER TABLE transacciones_servicios
            ADD CONSTRAINT chk_transaccion_intentos CHECK (intentos_confirmacion >= 0);
    END IF;
END $$;

-- Índices de idempotencia y conciliación
CREATE UNIQUE INDEX IF NOT EXISTS uq_idx_transaccion_cuenta_idempotency
    ON transacciones_servicios(cuenta_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transacciones_idempotency
    ON transacciones_servicios(idempotency_key);

CREATE INDEX IF NOT EXISTS idx_transacciones_conciliacion
    ON transacciones_servicios(proxima_confirmacion_at)
    WHERE estatus = 'EN_CONCILIACION';

CREATE INDEX IF NOT EXISTS idx_transacciones_estatus
    ON transacciones_servicios(estatus);


-- 3. LEDGER INMUTABLE: MOVIMIENTOS DE CUENTA
CREATE TABLE IF NOT EXISTS movimientos_cuenta (
    id BIGSERIAL PRIMARY KEY,
    cuenta_id BIGINT NOT NULL,
    transaccion_servicio_id BIGINT,
    tipo VARCHAR(30) NOT NULL,
    monto NUMERIC(15, 2) NOT NULL,
    saldo_anterior NUMERIC(15, 2) NOT NULL,
    saldo_posterior NUMERIC(15, 2) NOT NULL,
    fecha_movimiento TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movimiento_cuenta FOREIGN KEY (cuenta_id)
        REFERENCES cuentas(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimiento_transaccion FOREIGN KEY (transaccion_servicio_id)
        REFERENCES transacciones_servicios(id) ON DELETE RESTRICT,
    CONSTRAINT chk_movimiento_tipo CHECK (tipo IN ('DEBITO_PAGO', 'REVERSO_PAGO')),
    CONSTRAINT chk_movimiento_monto CHECK (monto > 0),
    CONSTRAINT chk_movimiento_saldos CHECK (saldo_anterior >= 0 AND saldo_posterior >= 0)
);

CREATE INDEX IF NOT EXISTS idx_movimientos_cuenta_id ON movimientos_cuenta(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_tx_id ON movimientos_cuenta(transaccion_servicio_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_cuenta(fecha_movimiento);


-- 4. BITÁCORA TÉCNICA GESTOPAGO (REQ. AUDITORÍA DEL PROVEEDOR)
CREATE TABLE IF NOT EXISTS gestopago_bitacora (
    id BIGSERIAL PRIMARY KEY,
    transaccion_servicio_id BIGINT,
    upc VARCHAR(50),
    tipo_operacion VARCHAR(30) NOT NULL,
    request_enmascarado TEXT,
    response_enmascarado TEXT,
    codigo_http INT,
    codigo_respuesta_proveedor VARCHAR(20),
    duracion_ms BIGINT,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bitacora_transaccion FOREIGN KEY (transaccion_servicio_id)
        REFERENCES transacciones_servicios(id) ON DELETE SET NULL,
    CONSTRAINT chk_bitacora_tipo_operacion CHECK (tipo_operacion IN ('SEND_TX', 'CONFIRM_TX', 'GET_PRODUCT_LIST', 'AUTH'))
);

CREATE INDEX IF NOT EXISTS idx_bitacora_tx_id ON gestopago_bitacora(transaccion_servicio_id);
CREATE INDEX IF NOT EXISTS idx_bitacora_upc ON gestopago_bitacora(upc);
CREATE INDEX IF NOT EXISTS idx_bitacora_fecha ON gestopago_bitacora(fecha_registro);

-- =====================================================================
-- MIGRACIÓN V7: AUTENTICACIÓN MULTIFACTOR (2FA / OTP SEGURO)
-- =====================================================================

CREATE TABLE IF NOT EXISTS otp_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    proposito VARCHAR(30) NOT NULL DEFAULT 'LOGIN_2FA',
    intentos INT NOT NULL DEFAULT 0,
    max_intentos INT NOT NULL DEFAULT 3,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_otp_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT chk_otp_intentos CHECK (intentos >= 0),
    CONSTRAINT chk_otp_proposito CHECK (proposito IN ('LOGIN_2FA', 'RECUPERACION_ACCESO'))
);

CREATE INDEX IF NOT EXISTS idx_otp_usuario_id ON otp_tokens(usuario_id);
CREATE INDEX IF NOT EXISTS idx_otp_fecha_expiracion ON otp_tokens(fecha_expiracion);
CREATE INDEX IF NOT EXISTS idx_otp_usado ON otp_tokens(usado);


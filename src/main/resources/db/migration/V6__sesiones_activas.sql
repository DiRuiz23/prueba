-- =====================================================================
-- MIGRACIÓN V6: GESTIÓN DE SESIONES ACTIVAS (SESIÓN ÚNICA POR USUARIO)
-- =====================================================================

CREATE TABLE IF NOT EXISTS sesiones_activas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    jti VARCHAR(64) NOT NULL UNIQUE,
    dispositivo VARCHAR(50) NOT NULL DEFAULT 'DESCONOCIDO',
    ip_origen VARCHAR(45),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    fecha_ultimo_acceso TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT chk_sesion_dispositivo CHECK (dispositivo IN ('WEB', 'MOVIL', 'DESCONOCIDO'))
);

CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_id ON sesiones_activas(usuario_id);
CREATE INDEX IF NOT EXISTS idx_sesiones_jti ON sesiones_activas(jti);
CREATE INDEX IF NOT EXISTS idx_sesiones_activa ON sesiones_activas(activa);
CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_activa ON sesiones_activas(usuario_id, activa);


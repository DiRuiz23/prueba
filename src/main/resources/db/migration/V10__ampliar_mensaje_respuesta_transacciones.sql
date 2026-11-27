-- =====================================================================
-- MIGRACIÓN V10: AMPLIAR MENSAJE_RESPUESTA EN TRANSACCIONES_SERVICIOS
-- =====================================================================
-- Amplía mensaje_respuesta para evitar truncamiento o errores cuando GestoPago
-- retorna tickets o mensajes de rechazo mayores a 255 caracteres.
ALTER TABLE transacciones_servicios
    ALTER COLUMN mensaje_respuesta TYPE TEXT;

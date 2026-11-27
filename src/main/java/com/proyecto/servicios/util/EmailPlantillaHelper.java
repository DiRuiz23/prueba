package com.proyecto.servicios.util;

/**
 * Genera correos HTML con la identidad visual de AppHasani.
 */
public final class EmailPlantillaHelper {

    private EmailPlantillaHelper() {
    }

    public static String construirHtmlCodigo(
            String titulo,
            String subtitulo,
            String codigo,
            int minutosVigencia
    ) {
        String tituloEsc = escaparHtml(titulo);
        String subtituloEsc = escaparHtml(subtitulo);
        String codigoEsc = escaparHtml(codigo);

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#F6F7F9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0F172A;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#F6F7F9;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:480px;background-color:#FFFFFF;border:1px solid #E6E8EC;border-radius:18px;overflow:hidden;">
                          <tr>
                            <td style="padding:24px 28px 16px 28px;background:linear-gradient(135deg,#E8F5EF 0%%,#FFFFFF 65%%);border-bottom:1px solid #E6E8EC;">
                              <table role="presentation" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td style="width:34px;height:34px;border-radius:10px;background-color:#0B7A5A;color:#FFFFFF;font-weight:700;font-size:17px;text-align:center;line-height:34px;">A</td>
                                  <td style="padding-left:10px;font-size:17px;font-weight:700;color:#0F172A;letter-spacing:-0.01em;">AppHasani</td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px;">
                              <h1 style="margin:0 0 8px 0;font-size:21px;line-height:1.3;font-weight:700;color:#0F172A;">%s</h1>
                              <p style="margin:0 0 22px 0;font-size:14.5px;line-height:1.55;color:#475569;">%s</p>
                              <div style="background-color:#E8F5EF;border:1px solid #BCE3D3;border-radius:14px;padding:20px 16px;text-align:center;margin-bottom:20px;">
                                <div style="font-size:11px;font-weight:700;letter-spacing:1.2px;text-transform:uppercase;color:#0B7A5A;margin-bottom:8px;">Código de verificación</div>
                                <div style="font-family:'Courier New',Courier,monospace;font-size:32px;font-weight:700;letter-spacing:8px;color:#0B7A5A;line-height:1.2;">%s</div>
                              </div>
                              <p style="margin:0 0 18px 0;text-align:center;">
                                <span style="display:inline-block;padding:5px 12px;background-color:#F1F5F9;border-radius:999px;font-size:12.5px;font-weight:600;color:#475569;">Válido por %d minutos</span>
                              </p>
                              <p style="margin:0;padding-top:16px;border-top:1px solid #E6E8EC;font-size:12.5px;line-height:1.5;color:#64748B;">
                                Por tu seguridad, nunca compartas este código con nadie. Si no realizaste esta solicitud, puedes ignorar este correo.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(tituloEsc, tituloEsc, subtituloEsc, codigoEsc, minutosVigencia);
    }

    public static String construirHtmlResultadoTransaccion(
            boolean exitosa,
            String nombreServicio,
            String montoFormateado,
            String saldoRestanteFormateado,
            String upc,
            String cuentaEnmascarada,
            String referencia,
            String numeroAutorizacion,
            String folioProveedor,
            String motivo,
            String fechaFormateada
    ) {
        String badgeBg = exitosa ? "#E8F5EF" : "#FDECEC";
        String badgeFg = exitosa ? "#0B7A5A" : "#C0392B";
        String badgeTexto = exitosa ? "Pago realizado" : "Pago rechazado";
        String estatusTexto = exitosa ? "EXITOSA" : "RECHAZADA";

        StringBuilder filas = new StringBuilder();
        agregarFilaHtml(filas, "Servicio", nombreServicio);
        agregarFilaHtml(filas, "Estatus", estatusTexto, badgeFg);
        agregarFilaHtml(filas, "Fecha", fechaFormateada);
        agregarFilaHtml(filas, "UPC", upc);
        agregarFilaHtml(filas, "Cuenta", cuentaEnmascarada);
        agregarFilaHtml(filas, "Referencia", referencia);
        if (exitosa && numeroAutorizacion != null && !numeroAutorizacion.isBlank()) {
            agregarFilaHtml(filas, "Autorización", numeroAutorizacion);
        }
        if (folioProveedor != null && !folioProveedor.isBlank()) {
            agregarFilaHtml(filas, "Folio proveedor", folioProveedor);
        }
        if (saldoRestanteFormateado != null && !saldoRestanteFormateado.isBlank()) {
            agregarFilaHtml(filas, "Saldo restante", saldoRestanteFormateado, "#0B7A5A");
        }

        String bloqueMotivo = "";
        if (!exitosa && motivo != null && !motivo.isBlank()) {
            bloqueMotivo = """
                    <div style="background-color:#FDECEC;border:1px solid #F5C2C0;border-radius:12px;padding:14px 16px;margin-bottom:18px;">
                      <div style="font-size:11.5px;font-weight:700;color:#C0392B;margin-bottom:4px;">Motivo del rechazo</div>
                      <div style="font-size:13.5px;line-height:1.45;color:#7F1D1D;">%s</div>
                      <div style="font-size:12px;line-height:1.45;color:#991B1B;margin-top:8px;">No se aplicó ningún cargo definitivo a tu cuenta; cualquier retención previa fue restituida automáticamente.</div>
                    </div>
                    """.formatted(escaparHtml(motivo));
        }

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s — AppHasani</title>
                </head>
                <body style="margin:0;padding:0;background-color:#F6F7F9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0F172A;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#F6F7F9;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:480px;background-color:#FFFFFF;border:1px solid #E6E8EC;border-radius:18px;overflow:hidden;">
                          <tr>
                            <td style="padding:22px 28px 16px 28px;background:linear-gradient(135deg,#E8F5EF 0%%,#FFFFFF 65%%);border-bottom:1px solid #E6E8EC;">
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                  <td>
                                    <table role="presentation" cellspacing="0" cellpadding="0">
                                      <tr>
                                        <td style="width:34px;height:34px;border-radius:10px;background-color:#0B7A5A;color:#FFFFFF;font-weight:700;font-size:17px;text-align:center;line-height:34px;">A</td>
                                        <td style="padding-left:10px;font-size:17px;font-weight:700;color:#0F172A;">AppHasani</td>
                                      </tr>
                                    </table>
                                  </td>
                                  <td align="right">
                                    <span style="display:inline-block;padding:5px 12px;border-radius:999px;background-color:%s;color:%s;font-size:12px;font-weight:700;">%s</span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:26px 28px 28px 28px;">
                              <div style="font-size:12px;font-weight:600;color:#64748B;text-transform:uppercase;letter-spacing:0.8px;">Importe</div>
                              <div style="font-size:30px;font-weight:700;color:#0F172A;margin:4px 0 18px 0;">%s</div>
                              %s
                              <div style="font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#64748B;margin-bottom:8px;">Ticket de transacción</div>
                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#F8FAFC;border:1px solid #E6E8EC;border-radius:12px;padding:4px 14px;">
                                %s
                              </table>
                              <p style="margin:18px 0 0 0;padding-top:14px;border-top:1px solid #E6E8EC;font-size:12px;line-height:1.5;color:#64748B;">
                                Este correo es un comprobante automático de tu operación en AppHasani. Si no reconoces este movimiento, comunícate con soporte de inmediato.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escaparHtml(badgeTexto),
                badgeBg,
                badgeFg,
                escaparHtml(badgeTexto),
                escaparHtml(montoFormateado),
                bloqueMotivo,
                filas.toString()
        );
    }

    private static void agregarFilaHtml(StringBuilder sb, String etiqueta, String valor) {
        agregarFilaHtml(sb, etiqueta, valor, "#0F172A");
    }

    private static void agregarFilaHtml(StringBuilder sb, String etiqueta, String valor, String colorValor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        sb.append("""
                <tr>
                  <td style="padding:9px 0;font-size:13px;color:#64748B;border-bottom:1px solid #E6E8EC;">%s</td>
                  <td align="right" style="padding:9px 0;font-size:13px;font-weight:700;color:%s;border-bottom:1px solid #E6E8EC;">%s</td>
                </tr>
                """.formatted(escaparHtml(etiqueta), colorValor, escaparHtml(valor)));
    }

    private static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

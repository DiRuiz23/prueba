package com.proyecto.servicios.dto.response.banco;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del comprobante de pago de servicio o recarga")
public class PagoServicioResponse {

    @Schema(description = "Identificador interno de la transacción", example = "1")
    private Long idTransaccion;

    @Schema(description = "Código único de transacción (UPC / Folio interno)", example = "GP000000000001")
    private String upc;

    @Schema(description = "Identificador de transacción otorgado por GestoPago", example = "291437188")
    private String idTxGestoPago;

    @Schema(description = "Número de autorización emitido por el proveedor", example = "100024988")
    private String numeroAutorizacion;

    @Schema(description = "Número de cuenta bancaria debitada", example = "01200000000000000016")
    private String numeroCuenta;

    @Schema(description = "Monto debitado de la cuenta", example = "10.00")
    private BigDecimal montoDebitado;

    @Schema(description = "Saldo disponible restante en la cuenta bancaria", example = "990.00")
    private BigDecimal saldoRestante;

    @Schema(description = "Teléfono o referencia del servicio pagado", example = "5555555551")
    private String telefonoReferencia;

    @Schema(description = "Estatus de la transacción (EXITOSA, EN_PROCESO, FALLIDA, REVERSADA)", example = "EXITOSA")
    private String estatus;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Operación realizada con éxito")
    private String mensaje;

    @Schema(description = "Fecha y hora del movimiento")
    private OffsetDateTime fechaTransaccion;
}

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/transaccion_entity.dart';

class ComprobanteDialog extends StatelessWidget {
  final TransaccionEntity transaccion;
  final String? productoNombre;

  const ComprobanteDialog({
    super.key,
    required this.transaccion,
    this.productoNombre,
  });

  static Future<void> mostrar(
    BuildContext context, {
    required TransaccionEntity transaccion,
    String? productoNombre,
  }) {
    return showDialog<void>(
      context: context,
      builder: (_) => ComprobanteDialog(
        transaccion: transaccion,
        productoNombre: productoNombre,
      ),
    );
  }

  String _enmascararCuenta(String cuenta) {
    final limpio = cuenta.trim();
    if (limpio.length >= 4) {
      return '•••• ${limpio.substring(limpio.length - 4)}';
    }
    return limpio;
  }

  @override
  Widget build(BuildContext context) {
    final fechaFmt = transaccion.fechaTransaccion != null
        ? DateFormat('dd/MM/yyyy HH:mm').format(transaccion.fechaTransaccion!.toLocal())
        : 'Reciente';
    final montoFmt = NumberFormat('#,##0.00').format(transaccion.montoDebitado);

    final esExitosa = transaccion.esExitosa;
    final esEnProceso = transaccion.estaEnProceso;
    final badgeLabel = esExitosa
        ? 'Exitosa'
        : (esEnProceso ? 'En proceso' : (transaccion.estatus.isNotEmpty ? transaccion.estatus : 'Rechazada'));
    final badgeBg = esExitosa
        ? AppColors.brandSoft
        : (esEnProceso ? AppColors.warnSoft : AppColors.dangerSoft);
    final badgeFg = esExitosa
        ? AppColors.brand
        : (esEnProceso ? AppColors.warn : AppColors.danger);
    final upcDisplay = transaccion.upc.trim().isNotEmpty
        ? transaccion.upc.trim()
        : 'No generado';

    final filas = <Widget>[
      if (productoNombre != null && productoNombre!.trim().isNotEmpty)
        _FilaDetalle('Servicio', productoNombre!.trim()),
      _FilaDetalle('Estatus', badgeLabel),
      _FilaDetalle('Fecha', fechaFmt),
      if (transaccion.idTransaccion > 0)
        _FilaDetalle('Transacción', '#${transaccion.idTransaccion}'),
      _FilaDetalle('UPC', upcDisplay),
      _FilaDetalle('Cuenta', _enmascararCuenta(transaccion.numeroCuenta)),
      _FilaDetalle('Referencia', transaccion.telefonoReferencia),
      if (esExitosa &&
          transaccion.numeroAutorizacion != null &&
          transaccion.numeroAutorizacion!.trim().isNotEmpty &&
          transaccion.numeroAutorizacion!.trim() != '-1' &&
          transaccion.numeroAutorizacion!.trim() != '0')
        _FilaDetalle('Autorización', transaccion.numeroAutorizacion!.trim()),
      if (transaccion.idTxGestoPago != null &&
          transaccion.idTxGestoPago!.trim().isNotEmpty &&
          transaccion.idTxGestoPago!.trim() != '-1' &&
          transaccion.idTxGestoPago!.trim() != '0')
        _FilaDetalle('Folio', transaccion.idTxGestoPago!.trim()),
      if (transaccion.saldoRestante != null)
        _FilaDetalle(
          'Saldo restante',
          '\$${NumberFormat('#,##0.00').format(transaccion.saldoRestante!)} MXN',
          resaltar: true,
        ),
    ];

    final ticketTexto = [
      'AppHasani — Ticket de transacción',
      'Estatus: $badgeLabel',
      if (productoNombre != null && productoNombre!.trim().isNotEmpty)
        'Servicio: ${productoNombre!.trim()}',
      if (transaccion.idTransaccion > 0)
        'Transacción: #${transaccion.idTransaccion}',
      'UPC: $upcDisplay',
      'Cuenta: ${_enmascararCuenta(transaccion.numeroCuenta)}',
      'Referencia: ${transaccion.telefonoReferencia}',
      'Importe: \$$montoFmt MXN',
      'Fecha: $fechaFmt',
      if (transaccion.mensaje.trim().isNotEmpty)
        'Detalle: ${transaccion.mensaje.trim()}',
    ].join('\n');

    return Dialog(
      backgroundColor: AppColors.surface,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: esExitosa ? AppColors.line : badgeFg.withValues(alpha: 0.35)),
      ),
      insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: badgeBg,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (!esExitosa) ...[
                          Icon(
                            esEnProceso ? Icons.schedule_rounded : Icons.error_outline_rounded,
                            size: 15,
                            color: badgeFg,
                          ),
                          const SizedBox(width: 5),
                        ],
                        Text(
                          esExitosa
                              ? 'Pago realizado'
                              : (esEnProceso ? 'Pago en proceso' : 'Pago rechazado'),
                          style: TextStyle(
                            color: badgeFg,
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    tooltip: 'Cerrar',
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close_rounded, size: 20, color: AppColors.ink2),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Text(
                '\$$montoFmt MXN',
                style: const TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w700,
                  color: AppColors.ink,
                  fontFeatures: [FontFeature.tabularFigures()],
                ),
              ),
              const SizedBox(height: 12),
              Flexible(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      if (!esExitosa && transaccion.mensaje.trim().isNotEmpty) ...[
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: badgeBg,
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(color: badgeFg.withValues(alpha: 0.25)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Motivo del rechazo',
                                style: TextStyle(
                                  color: badgeFg,
                                  fontSize: 11.5,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                transaccion.mensaje.trim(),
                                style: TextStyle(color: badgeFg, fontSize: 13),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 12),
                      ],
                      const Text(
                        'TICKET DE TRANSACCIÓN',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.9,
                          color: AppColors.ink3,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.bg,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: AppColors.line),
                        ),
                        child: Column(
                          children: [
                            for (var i = 0; i < filas.length; i++) ...[
                              if (i > 0) const Divider(height: 1),
                              filas[i],
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      style: OutlinedButton.styleFrom(
                        minimumSize: const Size(0, 46),
                      ),
                      icon: const Icon(Icons.copy_rounded, size: 17),
                      label: const Text('Copiar ticket'),
                      onPressed: () {
                        Clipboard.setData(ClipboardData(text: ticketTexto));
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Ticket copiado al portapapeles')),
                        );
                      },
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      style: FilledButton.styleFrom(
                        minimumSize: const Size(0, 46),
                      ),
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('Cerrar'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _FilaDetalle extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final bool resaltar;

  const _FilaDetalle(this.etiqueta, this.valor, {this.resaltar = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            etiqueta,
            style: const TextStyle(color: AppColors.ink2, fontSize: 13.5),
          ),
          const SizedBox(width: 16),
          Flexible(
            child: Text(
              valor,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                fontSize: 13.5,
                color: resaltar ? AppColors.brand : AppColors.ink,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

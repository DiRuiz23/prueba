import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../domain/entities/transaccion_entity.dart';

class ComprobanteDialog extends StatelessWidget {
  final TransaccionEntity transaccion;

  const ComprobanteDialog({super.key, required this.transaccion});

  static Future<void> mostrar(
    BuildContext context, {
    required TransaccionEntity transaccion,
  }) {
    return showDialog<void>(
      context: context,
      builder: (_) => ComprobanteDialog(transaccion: transaccion),
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
        : (esEnProceso ? 'En proceso' : transaccion.estatus);
    final badgeBg = esExitosa
        ? AppColors.brandSoft
        : (esEnProceso ? AppColors.warnSoft : AppColors.dangerSoft);
    final badgeFg = esExitosa
        ? AppColors.brand
        : (esEnProceso ? AppColors.warn : AppColors.danger);

    final filas = <Widget>[
      _FilaDetalle('Fecha', fechaFmt),
      _FilaDetalle('Transacción', '#${transaccion.idTransaccion}'),
      _FilaDetalle('Cuenta', _enmascararCuenta(transaccion.numeroCuenta)),
      _FilaDetalle('Referencia', transaccion.telefonoReferencia),
      if (transaccion.numeroAutorizacion != null &&
          transaccion.numeroAutorizacion!.trim().isNotEmpty)
        _FilaDetalle('Autorización', transaccion.numeroAutorizacion!.trim()),
      if (transaccion.idTxGestoPago != null &&
          transaccion.idTxGestoPago!.trim().isNotEmpty)
        _FilaDetalle('Folio', transaccion.idTxGestoPago!.trim()),
      if (transaccion.upc.trim().isNotEmpty)
        _FilaDetalle('UPC', transaccion.upc.trim()),
      if (transaccion.saldoRestante != null)
        _FilaDetalle(
          'Saldo restante',
          '\$${NumberFormat('#,##0.00').format(transaccion.saldoRestante!)} MXN',
          resaltar: true,
        ),
    ];

    return Dialog(
      backgroundColor: AppColors.surface,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: AppColors.line),
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
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: badgeBg,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Text(
                      badgeLabel,
                      style: TextStyle(
                        color: badgeFg,
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
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
              const SizedBox(height: 16),
              Flexible(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.bg,
                          borderRadius: BorderRadius.circular(10),
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
                      if (!esExitosa && transaccion.mensaje.trim().isNotEmpty) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: badgeBg,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            transaccion.mensaje.trim(),
                            style: TextStyle(color: badgeFg, fontSize: 13),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: () => Navigator.of(context).pop(),
                child: const Text('Cerrar'),
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

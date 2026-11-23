import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../features/compra/domain/entities/cuenta_entity.dart';
import '../theme/app_theme.dart';

class BalanceCard extends StatefulWidget {
  final List<CuentaEntity> cuentas;
  final CuentaEntity? cuentaActiva;
  final ValueChanged<String>? onCambiarCuenta;
  final VoidCallback? onRefrescar;

  const BalanceCard({
    super.key,
    required this.cuentas,
    this.cuentaActiva,
    this.onCambiarCuenta,
    this.onRefrescar,
  });

  @override
  State<BalanceCard> createState() => _BalanceCardState();
}

class _BalanceCardState extends State<BalanceCard> {
  bool _oculto = false;

  String _ultimos4(String n) => n.length >= 4 ? n.substring(n.length - 4) : n;

  @override
  Widget build(BuildContext context) {
    final cuenta = widget.cuentaActiva ??
        (widget.cuentas.isNotEmpty ? widget.cuentas.first : null);
    final fmt = NumberFormat('#,##0.00');

    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 8, 14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Text(
                  'Saldo disponible',
                  style: TextStyle(color: AppColors.ink2, fontSize: 13),
                ),
                const Spacer(),
                IconButton(
                  visualDensity: VisualDensity.compact,
                  tooltip: _oculto ? 'Mostrar saldo' : 'Ocultar saldo',
                  icon: Icon(
                    _oculto ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                    size: 20,
                    color: AppColors.ink2,
                  ),
                  onPressed: () => setState(() => _oculto = !_oculto),
                ),
                if (widget.onRefrescar != null)
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    tooltip: 'Actualizar',
                    icon: const Icon(Icons.refresh_rounded, size: 20, color: AppColors.ink2),
                    onPressed: widget.onRefrescar,
                  ),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.baseline,
                textBaseline: TextBaseline.alphabetic,
                children: [
                  Text(
                    cuenta == null
                        ? '—'
                        : (_oculto ? '••••••' : '\$${fmt.format(cuenta.saldo)}'),
                    style: const TextStyle(
                      fontSize: 30,
                      fontWeight: FontWeight.w700,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(width: 6),
                  const Text('MXN', style: TextStyle(color: AppColors.ink3, fontSize: 13)),
                ],
              ),
            ),
            if (cuenta != null) ...[
              const SizedBox(height: 8),
              if (widget.cuentas.length > 1 && widget.onCambiarCuenta != null)
                DropdownButtonHideUnderline(
                  child: DropdownButton<String>(
                    value: cuenta.numeroCuenta,
                    isDense: true,
                    style: const TextStyle(color: AppColors.ink2, fontSize: 13),
                    items: widget.cuentas
                        .map((c) => DropdownMenuItem(
                              value: c.numeroCuenta,
                              child: Text('Cuenta •••• ${_ultimos4(c.numeroCuenta)}'),
                            ))
                        .toList(),
                    onChanged: (v) {
                      if (v != null) widget.onCambiarCuenta!(v);
                    },
                  ),
                )
              else
                Text(
                  'Cuenta •••• ${_ultimos4(cuenta.numeroCuenta)}',
                  style: const TextStyle(color: AppColors.ink2, fontSize: 13),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

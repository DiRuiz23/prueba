import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../features/compra/domain/entities/cuenta_entity.dart';
import '../theme/app_theme.dart';

class BalanceCard extends StatefulWidget {
  final List<CuentaEntity> cuentas;
  final CuentaEntity? cuentaActiva;
  final bool cargando;
  final String? error;
  final ValueChanged<String>? onCambiarCuenta;
  final VoidCallback? onRefrescar;

  const BalanceCard({
    super.key,
    required this.cuentas,
    this.cuentaActiva,
    this.cargando = false,
    this.error,
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

    return Container(
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppColors.line),
        gradient: LinearGradient(
          begin: Alignment.topRight,
          end: Alignment.bottomLeft,
          colors: [
            AppColors.brandSoft.withValues(alpha: 0.72),
            AppColors.surface,
            AppColors.surface,
          ],
          stops: const [0.0, 0.52, 1.0],
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 12, 10, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: AppColors.brand,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 8),
                const Text(
                  'Saldo disponible',
                  style: TextStyle(
                    color: AppColors.ink2,
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const Spacer(),
                IconButton(
                  visualDensity: VisualDensity.compact,
                  tooltip: _oculto ? 'Mostrar saldo' : 'Ocultar saldo',
                  icon: Icon(
                    _oculto
                        ? Icons.visibility_off_outlined
                        : Icons.visibility_outlined,
                    size: 20,
                    color: AppColors.ink2,
                  ),
                  onPressed: () => setState(() => _oculto = !_oculto),
                ),
                if (widget.onRefrescar != null)
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    tooltip: 'Actualizar',
                    icon: widget.cargando
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: AppColors.brand,
                            ),
                          )
                        : const Icon(
                            Icons.refresh_rounded,
                            size: 20,
                            color: AppColors.ink2,
                          ),
                    onPressed: widget.cargando ? null : widget.onRefrescar,
                  ),
              ],
            ),
            const SizedBox(height: 2),
            if (widget.cargando && cuenta == null)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 8),
                child: Text(
                  'Consultando saldo...',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: AppColors.ink2,
                  ),
                ),
              )
            else if (cuenta == null)
              Padding(
                padding: const EdgeInsets.only(right: 8, top: 4),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '—',
                      style: TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                        color: AppColors.ink,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            widget.error ?? 'No se pudo cargar tu cuenta',
                            style: const TextStyle(
                              fontSize: 12.5,
                              color: AppColors.danger,
                            ),
                          ),
                        ),
                        if (widget.onRefrescar != null)
                          TextButton(
                            style: TextButton.styleFrom(
                              visualDensity: VisualDensity.compact,
                              padding: const EdgeInsets.symmetric(horizontal: 10),
                            ),
                            onPressed: widget.onRefrescar,
                            child: const Text('Reintentar'),
                          ),
                      ],
                    ),
                  ],
                ),
              )
            else
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text(
                      _oculto ? '••••••' : '\$${fmt.format(cuenta.saldo)}',
                      style: const TextStyle(
                        fontSize: 31,
                        fontWeight: FontWeight.w800,
                        letterSpacing: -0.5,
                        color: AppColors.ink,
                        fontFeatures: [FontFeature.tabularFigures()],
                      ),
                    ),
                    const SizedBox(width: 6),
                    const Text(
                      'MXN',
                      style: TextStyle(
                        color: AppColors.ink3,
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            if (cuenta != null) ...[
              const SizedBox(height: 10),
              if (widget.cuentas.length > 1 && widget.onCambiarCuenta != null)
                DropdownButtonHideUnderline(
                  child: DropdownButton<String>(
                    value: cuenta.numeroCuenta,
                    isDense: true,
                    style: const TextStyle(
                      color: AppColors.brand,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                    items: widget.cuentas
                        .map(
                          (c) => DropdownMenuItem(
                            value: c.numeroCuenta,
                            child: Text('Cuenta •••• ${_ultimos4(c.numeroCuenta)}'),
                          ),
                        )
                        .toList(),
                    onChanged: (v) {
                      if (v != null) widget.onCambiarCuenta!(v);
                    },
                  ),
                )
              else
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.brandSoft,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    'Cuenta •••• ${_ultimos4(cuenta.numeroCuenta)}',
                    style: const TextStyle(
                      color: AppColors.brand,
                      fontSize: 12.5,
                      fontWeight: FontWeight.w600,
                      fontFeatures: [FontFeature.tabularFigures()],
                    ),
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../features/compra/domain/entities/cuenta_entity.dart';

class VirtualBankCard extends StatefulWidget {
  final String titular;
  final List<CuentaEntity> cuentas;
  final CuentaEntity? cuentaActiva;
  final ValueChanged<String>? onCambiarCuenta;
  final VoidCallback? onRefrescar;

  const VirtualBankCard({
    super.key,
    required this.titular,
    required this.cuentas,
    this.cuentaActiva,
    this.onCambiarCuenta,
    this.onRefrescar,
  });

  @override
  State<VirtualBankCard> createState() => _VirtualBankCardState();
}

class _VirtualBankCardState extends State<VirtualBankCard> {
  bool _mostrarNumero = false;

  @override
  Widget build(BuildContext context) {
    final cuenta = widget.cuentaActiva ??
        (widget.cuentas.isNotEmpty ? widget.cuentas.first : null);
    final fmt = NumberFormat('#,##0.00');

    final numeroCuenta = cuenta?.numeroCuenta ?? '';
    final numeroDisplay = numeroCuenta.isEmpty
        ? '•••• •••• ••••'
        : (_mostrarNumero
            ? numeroCuenta
            : '•••• •••• ${numeroCuenta.length >= 4 ? numeroCuenta.substring(numeroCuenta.length - 4) : numeroCuenta}');

    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF0B1120), Color(0xFF172554), Color(0xFF064E3B)],
        ),
        border: Border.all(color: const Color(0xFF10B981).withValues(alpha: 0.45)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF0B1120).withValues(alpha: 0.24),
            blurRadius: 16,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Container(
                    width: 34,
                    height: 24,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(5),
                      gradient: const LinearGradient(
                        colors: [Color(0xFFFDE68A), Color(0xFFD97706)],
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  const Icon(Icons.wifi_rounded, color: Colors.white54, size: 18),
                ],
              ),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                    decoration: BoxDecoration(
                      color: const Color(0xFF10B981).withValues(alpha: 0.18),
                      borderRadius: BorderRadius.circular(99),
                      border: Border.all(
                        color: const Color(0xFF10B981).withValues(alpha: 0.5),
                      ),
                    ),
                    child: Text(
                      cuenta?.estatus ?? 'ACTIVA',
                      style: const TextStyle(
                        color: Color(0xFF6EE7B7),
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    icon: Icon(
                      _mostrarNumero ? Icons.visibility_off : Icons.visibility,
                      color: Colors.white70,
                      size: 18,
                    ),
                    onPressed: () => setState(() => _mostrarNumero = !_mostrarNumero),
                  ),
                  if (widget.onRefrescar != null)
                    IconButton(
                      visualDensity: VisualDensity.compact,
                      icon: const Icon(Icons.refresh, color: Colors.white70, size: 18),
                      onPressed: widget.onRefrescar,
                    ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 14),
          const Text(
            'SALDO DISPONIBLE',
            style: TextStyle(
              color: Color(0xFF94A3B8),
              fontSize: 10,
              letterSpacing: 1.2,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            cuenta != null ? '\$${fmt.format(cuenta.saldo)} MXN' : 'Consultando saldo...',
            style: const TextStyle(
              color: Color(0xFF34D399),
              fontSize: 24,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'TITULAR',
                      style: TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 10,
                        letterSpacing: 1.1,
                      ),
                    ),
                    Text(
                      widget.titular,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const Text(
                    'CUENTA',
                    style: TextStyle(
                      color: Color(0xFF94A3B8),
                      fontSize: 10,
                      letterSpacing: 1.1,
                    ),
                  ),
                  Text(
                    numeroDisplay,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w600,
                      fontSize: 13,
                      letterSpacing: 0.8,
                    ),
                  ),
                ],
              ),
            ],
          ),
          if (widget.cuentas.length > 1 && widget.onCambiarCuenta != null) ...[
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: Colors.black26,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.white24),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: cuenta?.numeroCuenta,
                  isExpanded: true,
                  dropdownColor: const Color(0xFF0F172A),
                  style: const TextStyle(color: Colors.white, fontSize: 12),
                  items: widget.cuentas
                      .map((c) => DropdownMenuItem(
                            value: c.numeroCuenta,
                            child: Text(
                              '${c.numeroCuenta} — \$${fmt.format(c.saldo)} MXN',
                            ),
                          ))
                      .toList(),
                  onChanged: (val) {
                    if (val != null) widget.onCambiarCuenta!(val);
                  },
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}


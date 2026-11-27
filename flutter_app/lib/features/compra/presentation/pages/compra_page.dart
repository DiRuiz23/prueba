import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/utils/input_formatters.dart';
import '../../../../core/widgets/product_avatar.dart';
import '../../../auth/presentation/cubit/auth_cubit.dart';
import '../../../auth/presentation/cubit/auth_state.dart';
import '../../../catalogo/domain/entities/producto_entity.dart';
import '../../../historial/presentation/cubit/historial_cubit.dart';
import '../../domain/entities/cuenta_entity.dart';
import '../cubit/compra_cubit.dart';
import '../cubit/compra_state.dart';
import '../widgets/comprobante_dialog.dart';

class CompraPage extends StatefulWidget {
  final ProductoEntity producto;

  const CompraPage({super.key, required this.producto});

  @override
  State<CompraPage> createState() => _CompraPageState();
}

class _CompraPageState extends State<CompraPage> {
  final _formKey = GlobalKey<FormState>();
  final _cuentaCtrl = TextEditingController();
  final _referenciaCtrl = TextEditingController();
  final _montoCtrl = TextEditingController();
  final _pinCtrl = TextEditingController();
  bool _ocultarPin = true;

  @override
  void initState() {
    super.initState();
    if (widget.producto.tienePrecioFijo) {
      _montoCtrl.text = widget.producto.precio;
    }
    _montoCtrl.addListener(() {
      if (mounted) setState(() {});
    });

    final cubit = context.read<CompraCubit>();
    cubit.prepararNuevaOperacion();

    final authState = context.read<AuthCubit>().state;
    if (authState is AuthAuthenticated &&
        cubit.state.cuentas.isEmpty &&
        !cubit.state.cargandoCuentas) {
      cubit.cargarCuentas(authState.correo);
    } else if (cubit.state.cuentaActiva != null) {
      _cuentaCtrl.text = cubit.state.cuentaActiva!.numeroCuenta;
    } else if (cubit.state.cuentas.isNotEmpty) {
      _cuentaCtrl.text = cubit.state.cuentas.first.numeroCuenta;
    }
  }

  @override
  void dispose() {
    _cuentaCtrl.dispose();
    _referenciaCtrl.dispose();
    _montoCtrl.dispose();
    _pinCtrl.dispose();
    super.dispose();
  }

  String _enmascararCuenta(String cuenta) {
    final limpio = cuenta.trim();
    if (limpio.length >= 4) {
      return '•••• ${limpio.substring(limpio.length - 4)}';
    }
    return limpio;
  }

  String _ultimos4(String n) => n.length >= 4 ? n.substring(n.length - 4) : n;

  Future<void> _confirmarCompra(BuildContext context) async {
    final cubit = context.read<CompraCubit>();
    if (_cuentaCtrl.text.trim().isEmpty && cubit.state.cuentaActiva != null) {
      _cuentaCtrl.text = cubit.state.cuentaActiva!.numeroCuenta;
    }
    if (!_formKey.currentState!.validate()) return;
    if (cubit.state is CompraLoading) return;

    final monto = double.tryParse(_montoCtrl.text.trim()) ?? 0.0;
    final fmt = NumberFormat('#,##0.00');
    final cuentaEnmascarada = _enmascararCuenta(_cuentaCtrl.text);

    final confirmar = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Confirmar pago',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _ConfirmRow('Servicio', widget.producto.producto),
            const Divider(height: 1),
            _ConfirmRow('Referencia', _referenciaCtrl.text.trim()),
            const Divider(height: 1),
            _ConfirmRow('Cuenta', cuentaEnmascarada),
            const Divider(height: 1),
            _ConfirmRow('Total', '\$${fmt.format(monto)} MXN', destacado: true),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              minimumSize: const Size(0, 44),
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Pagar'),
          ),
        ],
      ),
    );

    if (confirmar == true && context.mounted) {
      await cubit.ejecutarCompra(
        numeroCuenta: _cuentaCtrl.text.trim(),
        idProducto: widget.producto.idProducto,
        idServicio: widget.producto.idServicio,
        telefonoReferencia: _referenciaCtrl.text.trim(),
        monto: monto,
        pin: _pinCtrl.text,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = context.watch<AuthCubit>().state;
    final fmt = NumberFormat('#,##0.00');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Pagar'),
        actions: [
          if (authState is AuthAuthenticated)
            IconButton(
              tooltip: 'Actualizar saldo',
              icon: const Icon(Icons.refresh_rounded),
              onPressed: () =>
                  context.read<CompraCubit>().cargarCuentas(authState.correo),
            ),
        ],
      ),
      body: BlocConsumer<CompraCubit, CompraState>(
        listener: (context, state) {
          if (state.cuentaActiva != null) {
            _cuentaCtrl.text = state.cuentaActiva!.numeroCuenta;
          } else if (state.cuentas.isNotEmpty && _cuentaCtrl.text.isEmpty) {
            _cuentaCtrl.text = state.cuentas.first.numeroCuenta;
          }
          if (state is CompraExitosa) {
            ComprobanteDialog.mostrar(
              context,
              transaccion: state.transaccion,
              productoNombre: widget.producto.producto,
            );
          } else if (state is CompraRechazada && state.transaccion != null) {
            if (state.transaccion!.idTransaccion == 0) {
              context.read<HistorialCubit>().registrarTicketLocal(state.transaccion!);
            }
            ComprobanteDialog.mostrar(
              context,
              transaccion: state.transaccion!,
              productoNombre: widget.producto.producto,
            );
          }
        },
        builder: (context, state) {
          final isLoading = state is CompraLoading;
          final esExitosa = state is CompraExitosa;
          final cuentaActiva = state.cuentaActiva ??
              (state.cuentas.isNotEmpty ? state.cuentas.first : null);
          final montoIngresado = double.tryParse(_montoCtrl.text.trim()) ?? 0.0;
          final saldoActual = cuentaActiva?.saldo ?? 0.0;
          final saldoProyectado = saldoActual - montoIngresado;

          return SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          ProductAvatar(producto: widget.producto, size: 44),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  widget.producto.producto,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w700,
                                    color: AppColors.ink,
                                  ),
                                ),
                                Text(
                                  widget.producto.servicio,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    color: AppColors.ink2,
                                    fontSize: 13,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            widget.producto.tienePrecioFijo
                                ? '\$${widget.producto.precio}'
                                : 'Variable',
                            style: const TextStyle(
                              fontWeight: FontWeight.w700,
                              color: AppColors.ink,
                              fontSize: 15,
                              fontFeatures: [FontFeature.tabularFigures()],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (widget.producto.legend != null &&
                      widget.producto.legend!.trim().isNotEmpty) ...[
                    const SizedBox(height: 12),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFE8EFFF),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        widget.producto.legend!.trim(),
                        style: const TextStyle(
                          color: Color(0xFF1E3A8A),
                          fontSize: 13,
                        ),
                      ),
                    ),
                  ],
                  if (state is CompraExitosa ||
                      state is CompraPendienteIncierta ||
                      state is CompraRechazada ||
                      state is CompraErrorComunicacion) ...[
                    const SizedBox(height: 12),
                    _EstadoBanner(
                      state: state,
                      onVerificarEstado: (id) =>
                          context.read<CompraCubit>().verificarEstadoReal(id),
                      onVerHistorial: () => context.push('/historial'),
                      onNuevoPago: () {
                        context.read<CompraCubit>().prepararNuevaOperacion();
                        context.go('/catalogo');
                      },
                      onIntentarNuevo: () =>
                          context.read<CompraCubit>().prepararNuevaOperacion(),
                      onVerTicket: (state is CompraRechazada && state.transaccion != null)
                          ? () => ComprobanteDialog.mostrar(
                                context,
                                transaccion: state.transaccion!,
                                productoNombre: widget.producto.producto,
                              )
                          : null,
                      onReintentarComunicacion: () =>
                          _confirmarCompra(context),
                    ),
                  ],
                  const SizedBox(height: 12),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Form(
                        key: _formKey,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            if (state.cargandoCuentas && state.cuentas.isEmpty) ...[
                              const Padding(
                                padding: EdgeInsets.symmetric(vertical: 8),
                                child: Row(
                                  children: [
                                    SizedBox(
                                      width: 16,
                                      height: 16,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        color: AppColors.brand,
                                      ),
                                    ),
                                    SizedBox(width: 10),
                                    Text(
                                      'Cargando cuenta bancaria...',
                                      style: TextStyle(color: AppColors.ink2),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(height: 10),
                            ] else if (state.cuentas.length > 1 &&
                                cuentaActiva != null) ...[
                              _SelectorCuenta(
                                cuentas: state.cuentas,
                                cuentaActiva: cuentaActiva,
                                enabled: !isLoading && !esExitosa,
                                labelCuenta: (c) =>
                                    '•••• ${_ultimos4(c.numeroCuenta)} · \$${fmt.format(c.saldo)}',
                                onChanged: (numCta) {
                                  _cuentaCtrl.text = numCta;
                                  context
                                      .read<CompraCubit>()
                                      .seleccionarCuenta(numCta);
                                },
                              ),
                              const SizedBox(height: 14),
                            ] else if (state.cuentas.isEmpty) ...[
                              TextFormField(
                                controller: _cuentaCtrl,
                                enabled: !isLoading && !esExitosa,
                                keyboardType: TextInputType.number,
                                inputFormatters: AppInputFormatters.soloDigitos(13),
                                decoration: InputDecoration(
                                  labelText: 'Número de cuenta',
                                  suffixIcon: authState is AuthAuthenticated
                                      ? IconButton(
                                          tooltip: 'Cargar mi cuenta',
                                          icon: const Icon(Icons.refresh_rounded, size: 20),
                                          onPressed: () => context
                                              .read<CompraCubit>()
                                              .cargarCuentas(authState.correo),
                                        )
                                      : null,
                                ),
                                validator: (v) =>
                                    (v == null || v.trim().length < 10)
                                        ? 'Ingresa una cuenta válida'
                                        : null,
                              ),
                              const SizedBox(height: 14),
                            ],
                            TextFormField(
                              controller: _referenciaCtrl,
                              enabled: !isLoading && !esExitosa,
                              keyboardType: widget.producto.esRecarga
                                  ? TextInputType.phone
                                  : TextInputType.text,
                              inputFormatters: widget.producto.esRecarga
                                  ? AppInputFormatters.soloDigitos(10)
                                  : AppInputFormatters.referenciaServicio,
                              decoration: InputDecoration(
                                labelText: widget.producto.esRecarga
                                    ? 'Teléfono'
                                    : 'Referencia',
                              ),
                              validator: (v) {
                                final val = v?.trim() ?? '';
                                if (widget.producto.esRecarga) {
                                  if (!RegExp(r'^\d{10}$').hasMatch(val)) {
                                    return 'Ingresa un teléfono de 10 dígitos';
                                  }
                                } else {
                                  if (!RegExp(r'^[A-Za-z0-9\-_]{4,30}$')
                                      .hasMatch(val)) {
                                    return 'Referencia inválida (4 a 30 caracteres)';
                                  }
                                }
                                return null;
                              },
                            ),
                            const SizedBox(height: 14),
                            TextFormField(
                              controller: _montoCtrl,
                              readOnly: widget.producto.tienePrecioFijo,
                              enabled: !isLoading && !esExitosa,
                              keyboardType:
                                  const TextInputType.numberWithOptions(
                                      decimal: true),
                              inputFormatters: AppInputFormatters.decimal(maxEnteros: 7),
                              decoration: const InputDecoration(
                                labelText: 'Importe (MXN)',
                              ),
                              validator: (v) {
                                final num = double.tryParse(v ?? '');
                                if (num == null || num <= 0) {
                                  return 'Ingresa un importe mayor a cero';
                                }
                                return null;
                              },
                            ),
                            const SizedBox(height: 14),
                            TextFormField(
                              controller: _pinCtrl,
                              obscureText: _ocultarPin,
                              enabled: !isLoading && !esExitosa,
                              inputFormatters: AppInputFormatters.password,
                              decoration: InputDecoration(
                                labelText: 'PIN o contraseña',
                                suffixIcon: IconButton(
                                  icon: Icon(
                                    _ocultarPin
                                        ? Icons.visibility_outlined
                                        : Icons.visibility_off_outlined,
                                    size: 20,
                                  ),
                                  onPressed: () => setState(
                                      () => _ocultarPin = !_ocultarPin),
                                ),
                              ),
                              validator: (v) => (v == null || v.length < 4)
                                  ? 'Mínimo 4 caracteres'
                                  : null,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Resumen',
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              color: AppColors.ink,
                            ),
                          ),
                          const SizedBox(height: 10),
                          _ConfirmRow(
                            'Importe',
                            '\$${fmt.format(montoIngresado)} MXN',
                          ),
                          if (cuentaActiva != null) ...[
                            const Divider(height: 1),
                            _ConfirmRow(
                              'Saldo actual',
                              '\$${fmt.format(saldoActual)} MXN',
                            ),
                            const Divider(height: 1),
                            _ConfirmRow(
                              'Saldo después',
                              '\$${fmt.format(saldoProyectado)} MXN',
                              colorValor: saldoProyectado < 0
                                  ? AppColors.danger
                                  : AppColors.ink,
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  FilledButton(
                    onPressed: (isLoading || esExitosa)
                        ? null
                        : () => _confirmarCompra(context),
                    child: isLoading
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Text('Continuar'),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _SelectorCuenta extends StatelessWidget {
  final List<CuentaEntity> cuentas;
  final CuentaEntity cuentaActiva;
  final bool enabled;
  final String Function(CuentaEntity) labelCuenta;
  final ValueChanged<String> onChanged;

  const _SelectorCuenta({
    required this.cuentas,
    required this.cuentaActiva,
    required this.enabled,
    required this.labelCuenta,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: const InputDecoration(labelText: 'Cuenta de cargo'),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: cuentaActiva.numeroCuenta,
          isExpanded: true,
          isDense: true,
          style: const TextStyle(
            color: AppColors.ink,
            fontSize: 15,
            fontFeatures: [FontFeature.tabularFigures()],
          ),
          items: cuentas
              .map(
                (c) => DropdownMenuItem(
                  value: c.numeroCuenta,
                  child: Text(labelCuenta(c)),
                ),
              )
              .toList(),
          onChanged: enabled
              ? (v) {
                  if (v != null) onChanged(v);
                }
              : null,
        ),
      ),
    );
  }
}

class _ConfirmRow extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final bool destacado;
  final Color? colorValor;

  const _ConfirmRow(
    this.etiqueta,
    this.valor, {
    this.destacado = false,
    this.colorValor,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            etiqueta,
            style: const TextStyle(color: AppColors.ink2, fontSize: 13.5),
          ),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              valor,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontWeight: destacado ? FontWeight.w700 : FontWeight.w600,
                fontSize: destacado ? 15 : 13.5,
                color: colorValor ?? AppColors.ink,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _EstadoBanner extends StatelessWidget {
  final CompraState state;
  final void Function(int idTx) onVerificarEstado;
  final VoidCallback onVerHistorial;
  final VoidCallback onNuevoPago;
  final VoidCallback onIntentarNuevo;
  final VoidCallback? onVerTicket;
  final VoidCallback onReintentarComunicacion;

  const _EstadoBanner({
    required this.state,
    required this.onVerificarEstado,
    required this.onVerHistorial,
    required this.onNuevoPago,
    required this.onIntentarNuevo,
    this.onVerTicket,
    required this.onReintentarComunicacion,
  });

  @override
  Widget build(BuildContext context) {
    final s = state;
    if (s is CompraExitosa) {
      return Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.brandSoft,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.brand.withValues(alpha: 0.25)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Pago realizado',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.brand,
                fontSize: 14.5,
              ),
            ),
            if (s.transaccion.mensaje.trim().isNotEmpty) ...[
              const SizedBox(height: 4),
              Text(
                s.transaccion.mensaje.trim(),
                style: const TextStyle(color: AppColors.ink, fontSize: 13.5),
              ),
            ],
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: onVerHistorial,
                    child: const Text('Historial'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FilledButton(
                    style: FilledButton.styleFrom(
                      minimumSize: const Size(0, 42),
                    ),
                    onPressed: onNuevoPago,
                    child: const Text('Nuevo pago'),
                  ),
                ),
              ],
            ),
          ],
        ),
      );
    }

    if (s is CompraPendienteIncierta) {
      return Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.warnSoft,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.warn.withValues(alpha: 0.25)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'En proceso',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.warn,
                fontSize: 14.5,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              s.mensaje,
              maxLines: 4,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: AppColors.ink, fontSize: 13.5),
            ),
            if (s.transaccion != null) ...[
              const SizedBox(height: 12),
              FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.warn,
                  minimumSize: const Size.fromHeight(44),
                ),
                onPressed: () =>
                    onVerificarEstado(s.transaccion!.idTransaccion),
                child: const Text('Verificar estado'),
              ),
            ],
          ],
        ),
      );
    }

    if (s is CompraRechazada) {
      return Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.dangerSoft,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.danger.withValues(alpha: 0.25)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Pago rechazado',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.danger,
                fontSize: 14.5,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              s.mensaje,
              maxLines: 4,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: AppColors.ink, fontSize: 13.5),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                if (onVerTicket != null) ...[
                  Expanded(
                    child: OutlinedButton(
                      onPressed: onVerTicket,
                      child: const Text('Ver ticket'),
                    ),
                  ),
                  const SizedBox(width: 10),
                ],
                Expanded(
                  child: FilledButton(
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.danger,
                      minimumSize: const Size(0, 44),
                    ),
                    onPressed: onIntentarNuevo,
                    child: const Text('Intentar de nuevo'),
                  ),
                ),
              ],
            ),
          ],
        ),
      );
    }

    if (s is CompraErrorComunicacion) {
      return Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.dangerSoft,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.danger.withValues(alpha: 0.25)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Sin respuesta',
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: AppColors.danger,
                fontSize: 14.5,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              '${s.mensaje} Revisa el historial antes de reintentar.',
              style: const TextStyle(color: AppColors.ink, fontSize: 13.5),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: onVerHistorial,
                    child: const Text('Historial'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FilledButton(
                    style: FilledButton.styleFrom(
                      minimumSize: const Size(0, 42),
                    ),
                    onPressed: onReintentarComunicacion,
                    child: const Text('Reintentar'),
                  ),
                ),
              ],
            ),
          ],
        ),
      );
    }

    return const SizedBox.shrink();
  }
}

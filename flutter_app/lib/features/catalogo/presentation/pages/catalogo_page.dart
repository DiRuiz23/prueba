import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/error/exceptions.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/utils/input_formatters.dart';
import '../../../../core/widgets/balance_card.dart';
import '../../../../core/widgets/fintech_bottom_nav.dart';
import '../../../../core/widgets/product_avatar.dart';
import '../../../auth/data/models/registro_cliente_model.dart';
import '../../../auth/presentation/cubit/auth_cubit.dart';
import '../../../auth/presentation/cubit/auth_state.dart';
import '../../../auth/presentation/widgets/totp_setup_panel.dart';
import '../../../compra/presentation/cubit/compra_cubit.dart';
import '../../../compra/presentation/cubit/compra_state.dart';
import '../../domain/entities/producto_entity.dart';
import '../cubit/catalogo_cubit.dart';
import '../cubit/catalogo_state.dart';

class CatalogoPage extends StatefulWidget {
  const CatalogoPage({super.key});

  @override
  State<CatalogoPage> createState() => _CatalogoPageState();
}

class _CatalogoPageState extends State<CatalogoPage> {
  @override
  void initState() {
    super.initState();
    final catCubit = context.read<CatalogoCubit>();
    if (catCubit.state is CatalogoInitial) catCubit.cargarProductos();

    final authState = context.read<AuthCubit>().state;
    final compraCubit = context.read<CompraCubit>();
    if (authState is AuthAuthenticated &&
        compraCubit.state.cuentas.isEmpty &&
        !compraCubit.state.cargandoCuentas) {
      compraCubit.cargarCuentas(authState.correo);
    }
  }

  void _abrirDialogoSeguridad(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (_) => BlocProvider.value(
        value: context.read<AuthCubit>(),
        child: const _DialogoSeguridadTotp(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final authState = context.watch<AuthCubit>().state;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Servicios'),
        actions: [
          IconButton(
            tooltip: 'Seguridad 2FA (Google Authenticator)',
            icon: const Icon(Icons.shield_outlined),
            onPressed: () => _abrirDialogoSeguridad(context),
          ),
          IconButton(
            tooltip: 'Actualizar',
            icon: const Icon(Icons.refresh_rounded),
            onPressed: () {
              context.read<CatalogoCubit>().cargarProductos(forzarRecarga: true);
              if (authState is AuthAuthenticated) {
                context.read<CompraCubit>().cargarCuentas(authState.correo);
              }
            },
          ),
          IconButton(
            tooltip: 'Cerrar sesión',
            icon: const Icon(Icons.logout_rounded),
            onPressed: () => context.read<AuthCubit>().cerrarSesion(),
          ),
        ],
      ),
      bottomNavigationBar: const FintechBottomNav(currentIndex: 0),
      body: BlocBuilder<CatalogoCubit, CatalogoState>(
        builder: (context, state) {
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              BlocBuilder<CompraCubit, CompraState>(
                builder: (context, compra) => BalanceCard(
                  cuentas: compra.cuentas,
                  cuentaActiva: compra.cuentaActiva,
                  cargando: compra.cargandoCuentas,
                  error: compra.errorCuentas,
                  onCambiarCuenta: (n) => context.read<CompraCubit>().seleccionarCuenta(n),
                  onRefrescar: authState is AuthAuthenticated
                      ? () => context.read<CompraCubit>().cargarCuentas(authState.correo)
                      : null,
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                inputFormatters: [LengthLimitingTextInputFormatter(60)],
                decoration: AppTheme.pillInputDecoration(
                  hintText: 'Buscar servicio o recarga',
                  prefixIcon: Icons.search_rounded,
                ),
                onChanged: (v) =>
                    context.read<CatalogoCubit>().aplicarFiltros(busqueda: v),
              ),
              const SizedBox(height: 12),
              _Segmentos(
                actual: state is CatalogoLoaded ? state.categoria : CategoriaFiltro.todos,
                onChanged: (c) =>
                    context.read<CatalogoCubit>().aplicarFiltros(categoria: c),
              ),
              const SizedBox(height: 12),
              ..._contenido(context, state),
            ],
          );
        },
      ),
    );
  }

  List<Widget> _contenido(BuildContext context, CatalogoState state) {
    if (state is CatalogoLoading) {
      return const [
        Padding(
          padding: EdgeInsets.symmetric(vertical: 48),
          child: Center(child: CircularProgressIndicator()),
        ),
      ];
    }
    if (state is CatalogoError) {
      return [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 32),
          child: Column(
            children: [
              Text(state.mensaje, textAlign: TextAlign.center),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () =>
                    context.read<CatalogoCubit>().cargarProductos(forzarRecarga: true),
                child: const Text('Reintentar'),
              ),
            ],
          ),
        ),
      ];
    }
    if (state is CatalogoLoaded) {
      if (state.filtrados.isEmpty) {
        return const [
          Padding(
            padding: EdgeInsets.symmetric(vertical: 40),
            child: Center(child: Text('Sin resultados', style: TextStyle(color: AppColors.ink2))),
          ),
        ];
      }
      return [
        for (var i = 0; i < state.filtrados.length; i++) ...[
          if (i > 0) const SizedBox(height: 12),
          _ProductoCard(producto: state.filtrados[i]),
        ],
      ];
    }
    return const [];
  }
}

class _Segmentos extends StatelessWidget {
  final CategoriaFiltro actual;
  final ValueChanged<CategoriaFiltro> onChanged;

  const _Segmentos({required this.actual, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return SegmentedButton<CategoriaFiltro>(
      showSelectedIcon: false,
      style: SegmentedButton.styleFrom(
        visualDensity: VisualDensity.compact,
        selectedBackgroundColor: AppColors.brandSoft,
        selectedForegroundColor: AppColors.brand,
      ),
      segments: const [
        ButtonSegment(value: CategoriaFiltro.todos, label: Text('Todos')),
        ButtonSegment(value: CategoriaFiltro.recargas, label: Text('Recargas')),
        ButtonSegment(value: CategoriaFiltro.servicios, label: Text('Servicios')),
      ],
      selected: {actual},
      onSelectionChanged: (s) => onChanged(s.first),
    );
  }
}

class _ProductoCard extends StatelessWidget {
  final ProductoEntity producto;

  const _ProductoCard({required this.producto});

  @override
  Widget build(BuildContext context) {
    final esRecarga = producto.esRecarga;

    return Material(
      color: AppColors.surface,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: () => context.push('/producto-detalle', extra: producto),
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: AppColors.line),
            boxShadow: [
              BoxShadow(
                color: AppColors.ink.withValues(alpha: 0.04),
                blurRadius: 12,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  ProductAvatar(producto: producto, size: 48, circular: true),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: esRecarga ? AppColors.brandSoft : AppColors.bg,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          esRecarga ? Icons.bolt_rounded : Icons.receipt_long_rounded,
                          size: 14,
                          color: esRecarga ? AppColors.brand : AppColors.ink2,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          esRecarga ? 'Recarga' : 'Servicio',
                          style: TextStyle(
                            color: esRecarga ? AppColors.brand : AppColors.ink2,
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Flexible(
                    child: Text(
                      producto.servicio,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 13.5,
                        color: AppColors.ink,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Text(
                    'Pago inmediato',
                    style: TextStyle(color: AppColors.ink3, fontSize: 12),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                producto.producto,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontWeight: FontWeight.w700,
                  fontSize: 17.5,
                  height: 1.25,
                  color: AppColors.ink,
                ),
              ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: [
                  _CardChip(esRecarga ? 'Tiempo aire' : 'Pago de servicio'),
                  _CardChip(producto.tienePrecioFijo ? 'Monto fijo' : 'Monto variable'),
                ],
              ),
              const SizedBox(height: 16),
              const Divider(height: 1, color: AppColors.line),
              const SizedBox(height: 14),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        producto.tienePrecioFijo ? '\$${producto.precio} MXN' : 'Variable',
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 16.5,
                          color: AppColors.ink,
                          fontFeatures: [FontFeature.tabularFigures()],
                        ),
                      ),
                      const SizedBox(height: 2),
                      const Text(
                        'Cargo a tu cuenta',
                        style: TextStyle(color: AppColors.ink3, fontSize: 12),
                      ),
                    ],
                  ),
                  FilledButton(
                    style: FilledButton.styleFrom(
                      minimumSize: const Size(0, 42),
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    onPressed: () => context.push('/compra', extra: producto),
                    child: const Text('Pagar ahora'),
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

class _CardChip extends StatelessWidget {
  final String texto;

  const _CardChip(this.texto);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: const Color(0xFFF1F5F9),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        texto,
        style: const TextStyle(
          color: AppColors.ink2,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class _DialogoSeguridadTotp extends StatefulWidget {
  const _DialogoSeguridadTotp();

  @override
  State<_DialogoSeguridadTotp> createState() => _DialogoSeguridadTotpState();
}

class _DialogoSeguridadTotpState extends State<_DialogoSeguridadTotp> {
  final _codigoCtrl = TextEditingController();
  bool _cargando = true;
  bool _habilitado = false;
  TotpConfigResult? _config;
  String? _error;
  String? _mensajeOk;

  @override
  void initState() {
    super.initState();
    _cargarEstado();
  }

  @override
  void dispose() {
    _codigoCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarEstado() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final activo = await context.read<AuthCubit>().obtenerEstadoTotp();
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _habilitado = activo;
      });
    } on ServerException catch (e) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = e.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = 'No se pudo consultar el estado 2FA.';
      });
    }
  }

  Future<void> _configurar() async {
    setState(() {
      _cargando = true;
      _error = null;
      _mensajeOk = null;
    });
    try {
      final cfg = await context.read<AuthCubit>().configurarTotp();
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _config = cfg;
      });
    } on ServerException catch (e) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = e.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = 'No se pudo generar la clave.';
      });
    }
  }

  Future<void> _activar() async {
    if (_codigoCtrl.text.trim().length != 6) {
      setState(() => _error = 'Ingresa los 6 dígitos de Google Authenticator.');
      return;
    }
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      await context.read<AuthCubit>().activarTotp(_codigoCtrl.text.trim());
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _habilitado = true;
        _config = null;
        _codigoCtrl.clear();
        _mensajeOk = 'Google Authenticator activado correctamente.';
      });
    } on ServerException catch (e) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = e.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error = 'No se pudo activar Google Authenticator.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.surface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: const Text('Seguridad 2FA'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Método: Google Authenticator (${_habilitado ? 'Activo' : 'Pendiente'})',
              style: const TextStyle(fontSize: 13.5, color: AppColors.ink2),
            ),
            const SizedBox(height: 12),
            if (_error != null)
              Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.dangerSoft,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  _error!,
                  style: const TextStyle(color: AppColors.danger, fontSize: 13),
                ),
              ),
            if (_mensajeOk != null)
              Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.brandSoft,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  _mensajeOk!,
                  style: const TextStyle(color: AppColors.brand, fontSize: 13),
                ),
              ),
            if (_cargando && _config == null)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_config != null) ...[
              TotpSetupPanel(
                correo: _config!.correo,
                secreto: _config!.secreto,
                qrBase64: _config!.qrBase64,
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _codigoCtrl,
                keyboardType: TextInputType.number,
                textAlign: TextAlign.center,
                inputFormatters: AppInputFormatters.soloDigitos(6),
                decoration: AppTheme.pillInputDecoration(
                  hintText: 'Código de 6 dígitos',
                ),
              ),
            ] else
              Text(
                _habilitado
                    ? 'Tu cuenta utiliza Google Authenticator al iniciar sesión. Si cambiaste de dispositivo, puedes generar un nuevo código QR o clave secreta.'
                    : 'Vincula Google Authenticator escaneando el código QR o ingresando la clave secreta.',
                style: const TextStyle(fontSize: 13, color: AppColors.ink2),
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _cargando ? null : () => Navigator.of(context).pop(),
          child: const Text('Cerrar'),
        ),
        if (_config != null)
          FilledButton(
            style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
            onPressed: _cargando ? null : _activar,
            child: const Text('Activar'),
          )
        else if (!_cargando)
          FilledButton(
            style: FilledButton.styleFrom(minimumSize: const Size(0, 44)),
            onPressed: _configurar,
            child: Text(_habilitado ? 'Reconfigurar' : 'Configurar'),
          ),
      ],
    );
  }
}

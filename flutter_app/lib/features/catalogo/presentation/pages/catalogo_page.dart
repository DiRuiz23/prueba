import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/widgets/balance_card.dart';
import '../../../../core/widgets/fintech_bottom_nav.dart';
import '../../../../core/widgets/product_avatar.dart';
import '../../../auth/presentation/cubit/auth_cubit.dart';
import '../../../auth/presentation/cubit/auth_state.dart';
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
    if (authState is AuthAuthenticated && compraCubit.state.cuentas.isEmpty) {
      compraCubit.cargarCuentas(authState.correo);
    }
  }

  @override
  Widget build(BuildContext context) {
    final authState = context.watch<AuthCubit>().state;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Servicios'),
        actions: [
          IconButton(
            tooltip: 'Actualizar',
            icon: const Icon(Icons.refresh_rounded),
            onPressed: () =>
                context.read<CatalogoCubit>().cargarProductos(forzarRecarga: true),
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
                  onCambiarCuenta: (n) => context.read<CompraCubit>().seleccionarCuenta(n),
                  onRefrescar: authState is AuthAuthenticated
                      ? () => context.read<CompraCubit>().cargarCuentas(authState.correo)
                      : null,
                ),
              ),
              const SizedBox(height: 16),
              TextField(
                decoration: const InputDecoration(
                  hintText: 'Buscar',
                  prefixIcon: Icon(Icons.search_rounded),
                  isDense: true,
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
        Card(
          clipBehavior: Clip.antiAlias,
          child: Column(
            children: [
              for (var i = 0; i < state.filtrados.length; i++) ...[
                if (i > 0) const Divider(),
                _ProductoTile(producto: state.filtrados[i]),
              ],
            ],
          ),
        ),
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

class _ProductoTile extends StatelessWidget {
  final ProductoEntity producto;

  const _ProductoTile({required this.producto});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => context.push('/producto-detalle', extra: producto),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        child: Row(
          children: [
            ProductAvatar(producto: producto),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    producto.producto,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                  ),
                  Text(
                    producto.servicio,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: AppColors.ink2, fontSize: 13),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Text(
              producto.tienePrecioFijo ? '\$${producto.precio}' : 'Variable',
              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5),
            ),
          ],
        ),
      ),
    );
  }
}

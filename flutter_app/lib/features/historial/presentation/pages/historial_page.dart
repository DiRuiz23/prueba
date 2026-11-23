import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/widgets/fintech_bottom_nav.dart';
import '../../../compra/domain/entities/transaccion_entity.dart';
import '../../../compra/presentation/widgets/comprobante_dialog.dart';
import '../cubit/historial_cubit.dart';
import '../cubit/historial_state.dart';

class HistorialPage extends StatefulWidget {
  const HistorialPage({super.key});

  @override
  State<HistorialPage> createState() => _HistorialPageState();
}

class _HistorialPageState extends State<HistorialPage> {
  final _scrollCtrl = ScrollController();

  @override
  void initState() {
    super.initState();
    context.read<HistorialCubit>().cargarInicial();
    _scrollCtrl.addListener(_onScroll);
  }

  void _onScroll() {
    if (_scrollCtrl.position.pixels >= _scrollCtrl.position.maxScrollExtent - 180) {
      context.read<HistorialCubit>().cargarSiguientePagina();
    }
  }

  @override
  void dispose() {
    _scrollCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final numFmt = NumberFormat('#,##0.00');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Historial'),
        actions: [
          IconButton(
            tooltip: 'Actualizar',
            icon: const Icon(Icons.refresh_rounded),
            onPressed: () => context.read<HistorialCubit>().cargarInicial(),
          ),
        ],
      ),
      bottomNavigationBar: const FintechBottomNav(currentIndex: 1),
      body: BlocBuilder<HistorialCubit, HistorialState>(
        builder: (context, state) {
          if (state is HistorialLoading) {
            return const Center(
              child: CircularProgressIndicator(color: AppColors.brand),
            );
          }

          if (state is HistorialError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      state.mensaje,
                      textAlign: TextAlign.center,
                      style: const TextStyle(color: AppColors.ink2),
                    ),
                    const SizedBox(height: 12),
                    OutlinedButton(
                      onPressed: () => context.read<HistorialCubit>().cargarInicial(),
                      child: const Text('Reintentar'),
                    ),
                  ],
                ),
              ),
            );
          }

          if (state is HistorialLoaded) {
            if (state.items.isEmpty) {
              return RefreshIndicator(
                color: AppColors.brand,
                onRefresh: () => context.read<HistorialCubit>().cargarInicial(),
                child: ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: const [
                    SizedBox(height: 120),
                    Center(
                      child: Text(
                        'Sin movimientos registrados',
                        style: TextStyle(color: AppColors.ink2),
                      ),
                    ),
                  ],
                ),
              );
            }

            return RefreshIndicator(
              color: AppColors.brand,
              onRefresh: () => context.read<HistorialCubit>().cargarInicial(),
              child: ListView(
                controller: _scrollCtrl,
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                children: [
                  Card(
                    clipBehavior: Clip.antiAlias,
                    child: Column(
                      children: [
                        for (var i = 0; i < state.items.length; i++) ...[
                          if (i > 0) const Divider(),
                          _TransaccionTile(
                            tx: state.items[i],
                            numFmt: numFmt,
                            onTap: () => ComprobanteDialog.mostrar(
                              context,
                              transaccion: state.items[i],
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  if (state.cargandoMas)
                    const Padding(
                      padding: EdgeInsets.all(16),
                      child: Center(
                        child: CircularProgressIndicator(color: AppColors.brand),
                      ),
                    ),
                ],
              ),
            );
          }

          return const SizedBox.shrink();
        },
      ),
    );
  }
}

class _TransaccionTile extends StatelessWidget {
  final TransaccionEntity tx;
  final NumberFormat numFmt;
  final VoidCallback onTap;

  const _TransaccionTile({
    required this.tx,
    required this.numFmt,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final fecha = tx.fechaTransaccion != null
        ? DateFormat('dd/MM/yyyy HH:mm').format(tx.fechaTransaccion!.toLocal())
        : '';
    final esExitosa = tx.esExitosa;
    final badgeLabel = esExitosa
        ? 'Exitosa'
        : (tx.estaEnProceso ? 'En proceso' : tx.estatus);
    final badgeBg = esExitosa
        ? AppColors.brandSoft
        : (tx.estaEnProceso ? AppColors.warnSoft : AppColors.dangerSoft);
    final badgeFg = esExitosa
        ? AppColors.brand
        : (tx.estaEnProceso ? AppColors.warn : AppColors.danger);

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Producto #${tx.idProducto ?? '-'} · ${tx.telefonoReferencia}',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                      color: AppColors.ink,
                    ),
                  ),
                  if (fecha.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      fecha,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.ink2,
                        fontFeatures: [FontFeature.tabularFigures()],
                      ),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '\$${numFmt.format(tx.montoDebitado)}',
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14.5,
                    color: AppColors.ink,
                    fontFeatures: [FontFeature.tabularFigures()],
                  ),
                ),
                const SizedBox(height: 4),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: badgeBg,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Text(
                    badgeLabel,
                    style: TextStyle(
                      color: badgeFg,
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

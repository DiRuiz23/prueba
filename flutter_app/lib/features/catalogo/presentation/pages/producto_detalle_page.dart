import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/widgets/product_avatar.dart';
import '../../domain/entities/producto_entity.dart';

class ProductoDetallePage extends StatelessWidget {
  final ProductoEntity producto;

  const ProductoDetallePage({super.key, required this.producto});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Servicio')),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton(
            onPressed: () => context.push('/compra', extra: producto),
            child: const Text('Pagar'),
          ),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            children: [
              ProductAvatar(producto: producto, size: 52),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      producto.producto,
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
                    ),
                    Text(
                      producto.servicio,
                      style: const TextStyle(color: AppColors.ink2),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Card(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Column(
                children: [
                  _Fila(
                    'Importe',
                    producto.tienePrecioFijo ? '\$${producto.precio} MXN' : 'Variable',
                    destacado: true,
                  ),
                  const Divider(),
                  _Fila('Tipo', producto.esRecarga ? 'Recarga' : 'Pago de servicio'),
                  const Divider(),
                  _Fila('Producto', '#${producto.idProducto}'),
                ],
              ),
            ),
          ),
          if (producto.legend != null && producto.legend!.isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFE8EFFF),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                producto.legend!,
                style: const TextStyle(color: Color(0xFF1E3A8A), fontSize: 13.5),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _Fila extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final bool destacado;

  const _Fila(this.etiqueta, this.valor, {this.destacado = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(etiqueta, style: const TextStyle(color: AppColors.ink2)),
          Text(
            valor,
            style: TextStyle(
              fontWeight: FontWeight.w700,
              fontSize: destacado ? 17 : 14.5,
            ),
          ),
        ],
      ),
    );
  }
}

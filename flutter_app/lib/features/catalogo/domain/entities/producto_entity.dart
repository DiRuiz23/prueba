import 'package:equatable/equatable.dart';

class ProductoEntity extends Equatable {
  final String producto;
  final String servicio;
  final int idServicio;
  final int idProducto;
  final int idCatTipoServicio;
  final int tipoFront;
  final String precio;
  final String? legend;
  final bool showAyuda;

  const ProductoEntity({
    required this.producto,
    required this.servicio,
    required this.idServicio,
    required this.idProducto,
    required this.idCatTipoServicio,
    required this.tipoFront,
    required this.precio,
    this.legend,
    this.showAyuda = false,
  });

  bool get esRecarga => tipoFront == 1;

  bool get tienePrecioFijo {
    final p = double.tryParse(precio);
    return p != null && p > 0;
  }

  @override
  List<Object?> get props => [
        producto,
        servicio,
        idServicio,
        idProducto,
        idCatTipoServicio,
        tipoFront,
        precio,
        legend,
        showAyuda,
      ];
}


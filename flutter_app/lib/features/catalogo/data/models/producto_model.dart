import '../../domain/entities/producto_entity.dart';

class ProductoModel extends ProductoEntity {
  const ProductoModel({
    required super.producto,
    required super.servicio,
    required super.idServicio,
    required super.idProducto,
    required super.idCatTipoServicio,
    required super.tipoFront,
    required super.precio,
    super.legend,
    super.showAyuda,
  });

  factory ProductoModel.fromJson(Map<String, dynamic> json) {
    return ProductoModel(
      producto: (json['producto'] as String?) ?? '',
      servicio: (json['servicio'] as String?) ?? '',
      idServicio: (json['idServicio'] as num?)?.toInt() ?? 0,
      idProducto: (json['idProducto'] as num?)?.toInt() ?? 0,
      idCatTipoServicio: (json['idCatTipoServicio'] as num?)?.toInt() ?? 0,
      tipoFront: (json['tipoFront'] as num?)?.toInt() ?? 1,
      precio: (json['precio']?.toString()) ?? '0',
      legend: json['legend'] as String?,
      showAyuda: (json['showAyuda'] as bool?) ?? false,
    );
  }
}


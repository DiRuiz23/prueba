import '../entities/producto_entity.dart';

abstract class CatalogoRepository {
  Future<List<ProductoEntity>> obtenerProductos({bool forzarRecarga = false});
  void limpiarCache();
}


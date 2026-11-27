import '../../domain/entities/producto_entity.dart';
import '../../domain/repositories/catalogo_repository.dart';
import '../datasources/catalogo_remote_datasource.dart';

class CatalogoRepositoryImpl implements CatalogoRepository {
  final CatalogoRemoteDataSource remoteDataSource;
  List<ProductoEntity>? _cache;

  CatalogoRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<ProductoEntity>> obtenerProductos({bool forzarRecarga = false}) async {
    if (_cache != null && !forzarRecarga) {
      return _cache!;
    }
    final lista = await remoteDataSource.obtenerProductos();
    _cache = lista;
    return lista;
  }

  @override
  void limpiarCache() {
    _cache = null;
  }
}


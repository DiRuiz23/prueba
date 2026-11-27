import '../../domain/entities/historial_page_entity.dart';
import '../../domain/repositories/historial_repository.dart';
import '../datasources/historial_remote_datasource.dart';

class HistorialRepositoryImpl implements HistorialRepository {
  final HistorialRemoteDataSource remoteDataSource;

  HistorialRepositoryImpl({required this.remoteDataSource});

  @override
  Future<HistorialPageEntity> obtenerHistorialExitoso({
    int pagina = 0,
    int tamanio = 10,
  }) {
    return remoteDataSource.obtenerHistorialExitoso(
      pagina: pagina,
      tamanio: tamanio,
    );
  }
}


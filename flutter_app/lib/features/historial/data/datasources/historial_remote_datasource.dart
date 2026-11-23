import '../../../../core/network/dio_client.dart';
import '../../../compra/data/models/compra_models.dart';
import '../../domain/entities/historial_page_entity.dart';

abstract class HistorialRemoteDataSource {
  Future<HistorialPageEntity> obtenerHistorialExitoso({
    int pagina = 0,
    int tamanio = 10,
  });
}

class HistorialRemoteDataSourceImpl implements HistorialRemoteDataSource {
  final DioClient dioClient;

  HistorialRemoteDataSourceImpl({required this.dioClient});

  @override
  Future<HistorialPageEntity> obtenerHistorialExitoso({
    int pagina = 0,
    int tamanio = 10,
  }) async {
    final response = await dioClient.get(
      '/transacciones/historial',
      queryParameters: {
        'page': pagina,
        'size': tamanio,
      },
    );

    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>? ?? {};
    final content = datos['content'] as List<dynamic>? ?? [];
    final items = content
        .whereType<Map<String, dynamic>>()
        .map(TransaccionModel.fromJson)
        .toList();

    return HistorialPageEntity(
      items: items,
      paginaActual: (datos['number'] as num?)?.toInt() ?? 0,
      totalPaginas: (datos['totalPages'] as num?)?.toInt() ?? 1,
      totalElementos: (datos['totalElements'] as num?)?.toInt() ?? items.length,
      esUltima: (datos['last'] as bool?) ?? true,
    );
  }
}


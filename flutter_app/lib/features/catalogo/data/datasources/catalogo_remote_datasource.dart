import '../../../../core/network/dio_client.dart';
import '../models/producto_model.dart';

abstract class CatalogoRemoteDataSource {
  Future<List<ProductoModel>> obtenerProductos();
}

class CatalogoRemoteDataSourceImpl implements CatalogoRemoteDataSource {
  final DioClient dioClient;

  CatalogoRemoteDataSourceImpl({required this.dioClient});

  @override
  Future<List<ProductoModel>> obtenerProductos() async {
    final response = await dioClient.get('/gestopago/productos');
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    final products = datos?['products'] as List<dynamic>? ?? [];
    return products
        .whereType<Map<String, dynamic>>()
        .map(ProductoModel.fromJson)
        .toList();
  }
}


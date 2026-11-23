import 'package:dio/dio.dart';
import '../../../../core/error/exceptions.dart';
import '../../../../core/network/dio_client.dart';
import '../models/compra_models.dart';

abstract class CompraRemoteDataSource {
  Future<List<CuentaModel>> obtenerCuentasCliente(String correo);

  Future<TransaccionModel> ejecutarPago({
    required String numeroCuenta,
    required int idProducto,
    required int idServicio,
    required String telefonoReferencia,
    required double monto,
    required String pin,
    required String idempotencyKey,
  });

  Future<TransaccionModel> consultarEstadoTransaccion(int idTransaccion);
}

class CompraRemoteDataSourceImpl implements CompraRemoteDataSource {
  final DioClient dioClient;

  CompraRemoteDataSourceImpl({required this.dioClient});

  @override
  Future<List<CuentaModel>> obtenerCuentasCliente(String correo) async {
    final cliResp = await dioClient.post(
      '/clientes/consultar',
      data: {
        'correo': correo,
        'paginacion': {'pagina': 0, 'tamanio': 1},
      },
    );

    final cliBody = cliResp.data as Map<String, dynamic>;
    final cliDatos = cliBody['datos'] as Map<String, dynamic>?;
    final clientes = cliDatos?['contenido'] as List<dynamic>? ?? [];
    if (clientes.isEmpty) return [];

    final clienteId = (clientes.first as Map<String, dynamic>)['id'];
    final ctaResp = await dioClient.post(
      '/cuentas/consultar',
      data: {
        'clienteId': clienteId,
        'paginacion': {'pagina': 0, 'tamanio': 10},
      },
    );

    final ctaBody = ctaResp.data as Map<String, dynamic>;
    final ctaDatos = ctaBody['datos'] as Map<String, dynamic>?;
    final lista = ctaDatos?['contenido'] as List<dynamic>? ?? [];
    return lista.whereType<Map<String, dynamic>>().map(CuentaModel.fromJson).toList();
  }

  @override
  Future<TransaccionModel> ejecutarPago({
    required String numeroCuenta,
    required int idProducto,
    required int idServicio,
    required String telefonoReferencia,
    required double monto,
    required String pin,
    required String idempotencyKey,
  }) async {
    final response = await dioClient.post(
      '/transacciones/pago-servicio',
      data: {
        'numeroCuenta': numeroCuenta,
        'idProducto': idProducto,
        'idServicio': idServicio,
        'telefonoReferencia': telefonoReferencia,
        'monto': monto,
        'pin': pin,
      },
      options: Options(headers: {'Idempotency-Key': idempotencyKey}),
    );

    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'Respuesta sin comprobante de transacción',
        statusCode: response.statusCode,
      );
    }
    return TransaccionModel.fromJson(datos);
  }

  @override
  Future<TransaccionModel> consultarEstadoTransaccion(int idTransaccion) async {
    final response = await dioClient.get('/transacciones/$idTransaccion');
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'No se encontró la transacción',
        statusCode: response.statusCode,
      );
    }
    return TransaccionModel.fromJson(datos);
  }
}


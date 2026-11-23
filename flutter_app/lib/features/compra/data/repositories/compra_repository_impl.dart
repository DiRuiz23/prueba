import '../../domain/entities/cuenta_entity.dart';
import '../../domain/entities/transaccion_entity.dart';
import '../../domain/repositories/compra_repository.dart';
import '../datasources/compra_remote_datasource.dart';

class CompraRepositoryImpl implements CompraRepository {
  final CompraRemoteDataSource remoteDataSource;

  CompraRepositoryImpl({required this.remoteDataSource});

  @override
  Future<List<CuentaEntity>> obtenerCuentasCliente(String correo) {
    return remoteDataSource.obtenerCuentasCliente(correo);
  }

  @override
  Future<TransaccionEntity> ejecutarPago({
    required String numeroCuenta,
    required int idProducto,
    required int idServicio,
    required String telefonoReferencia,
    required double monto,
    required String pin,
    required String idempotencyKey,
  }) {
    return remoteDataSource.ejecutarPago(
      numeroCuenta: numeroCuenta,
      idProducto: idProducto,
      idServicio: idServicio,
      telefonoReferencia: telefonoReferencia,
      monto: monto,
      pin: pin,
      idempotencyKey: idempotencyKey,
    );
  }

  @override
  Future<TransaccionEntity> consultarEstadoTransaccion(int idTransaccion) {
    return remoteDataSource.consultarEstadoTransaccion(idTransaccion);
  }
}


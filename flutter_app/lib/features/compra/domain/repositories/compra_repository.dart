import '../entities/cuenta_entity.dart';
import '../entities/transaccion_entity.dart';

abstract class CompraRepository {
  Future<List<CuentaEntity>> obtenerCuentasCliente(String correo);

  Future<TransaccionEntity> ejecutarPago({
    required String numeroCuenta,
    required int idProducto,
    required int idServicio,
    required String telefonoReferencia,
    required double monto,
    required String pin,
    required String idempotencyKey,
  });

  Future<TransaccionEntity> consultarEstadoTransaccion(int idTransaccion);
}


import '../../domain/entities/cuenta_entity.dart';
import '../../domain/entities/transaccion_entity.dart';

class TransaccionModel extends TransaccionEntity {
  const TransaccionModel({
    required super.idTransaccion,
    required super.upc,
    super.idTxGestoPago,
    super.numeroAutorizacion,
    required super.numeroCuenta,
    required super.montoDebitado,
    super.saldoRestante,
    required super.telefonoReferencia,
    super.idProducto,
    super.idServicio,
    required super.estatus,
    required super.mensaje,
    super.fechaTransaccion,
  });

  factory TransaccionModel.fromJson(Map<String, dynamic> json) {
    final fechaStr = json['fechaTransaccion'] as String?;
    return TransaccionModel(
      idTransaccion: (json['idTransaccion'] as num?)?.toInt() ?? 0,
      upc: (json['upc'] as String?) ?? '',
      idTxGestoPago: json['idTxGestoPago'] as String?,
      numeroAutorizacion: json['numeroAutorizacion'] as String?,
      numeroCuenta: (json['numeroCuenta'] as String?) ?? '',
      montoDebitado: (json['montoDebitado'] as num?)?.toDouble() ?? 0.0,
      saldoRestante: (json['saldoRestante'] as num?)?.toDouble(),
      telefonoReferencia: (json['telefonoReferencia'] as String?) ?? '',
      idProducto: (json['idProducto'] as num?)?.toInt(),
      idServicio: (json['idServicio'] as num?)?.toInt(),
      estatus: (json['estatus'] as String?) ?? 'DESCONOCIDO',
      mensaje: (json['mensaje'] as String?) ?? '',
      fechaTransaccion: fechaStr != null ? DateTime.tryParse(fechaStr) : null,
    );
  }
}

class CuentaModel extends CuentaEntity {
  const CuentaModel({
    required super.id,
    required super.numeroCuenta,
    required super.saldo,
    required super.estatus,
  });

  factory CuentaModel.fromJson(Map<String, dynamic> json) {
    return CuentaModel(
      id: (json['id'] as num?)?.toInt() ?? 0,
      numeroCuenta: (json['numeroCuenta'] as String?) ?? '',
      saldo: (json['saldo'] as num?)?.toDouble() ?? 0.0,
      estatus: (json['estatus'] as String?) ?? 'ACTIVA',
    );
  }
}


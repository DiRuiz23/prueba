import '../../domain/entities/cuenta_entity.dart';
import '../../domain/entities/transaccion_entity.dart';

int? _asInt(dynamic v) =>
    v is int ? v : (v is num ? v.toInt() : int.tryParse('${v ?? ''}'));

double? _asDouble(dynamic v) {
  if (v == null) return null;
  if (v is num) return v.toDouble();
  final cleaned = v.toString().trim().replaceAll(',', '');
  return double.tryParse(cleaned);
}

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
    final rawFecha = json['fechaTransaccion'];
    final fechaStr = rawFecha is String ? rawFecha : rawFecha?.toString();
    return TransaccionModel(
      idTransaccion: _asInt(json['idTransaccion']) ?? 0,
      upc: json['upc']?.toString() ?? '',
      idTxGestoPago: json['idTxGestoPago']?.toString(),
      numeroAutorizacion: json['numeroAutorizacion']?.toString(),
      numeroCuenta: json['numeroCuenta']?.toString() ?? '',
      montoDebitado: _asDouble(json['montoDebitado']) ?? 0.0,
      saldoRestante: _asDouble(json['saldoRestante']),
      telefonoReferencia: json['telefonoReferencia']?.toString() ?? '',
      idProducto: _asInt(json['idProducto']),
      idServicio: _asInt(json['idServicio']),
      estatus: json['estatus']?.toString() ?? 'DESCONOCIDO',
      mensaje: json['mensaje']?.toString() ?? '',
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
      id: _asInt(json['id']) ?? 0,
      numeroCuenta: (json['numeroCuenta'] ?? json['numero_cuenta'])?.toString().trim() ?? '',
      saldo: _asDouble(
            json['saldo'] ?? json['saldoDisponible'] ?? json['saldo_disponible'],
          ) ??
          0.0,
      estatus: (json['estatus'] ?? json['estado'])?.toString().trim() ?? 'ACTIVA',
    );
  }
}


import 'package:equatable/equatable.dart';

class TransaccionEntity extends Equatable {
  final int idTransaccion;
  final String upc;
  final String? idTxGestoPago;
  final String? numeroAutorizacion;
  final String numeroCuenta;
  final double montoDebitado;
  final double? saldoRestante;
  final String telefonoReferencia;
  final int? idProducto;
  final int? idServicio;
  final String estatus;
  final String mensaje;
  final DateTime? fechaTransaccion;

  const TransaccionEntity({
    required this.idTransaccion,
    required this.upc,
    this.idTxGestoPago,
    this.numeroAutorizacion,
    required this.numeroCuenta,
    required this.montoDebitado,
    this.saldoRestante,
    required this.telefonoReferencia,
    this.idProducto,
    this.idServicio,
    required this.estatus,
    required this.mensaje,
    this.fechaTransaccion,
  });

  bool get esExitosa => estatus == 'EXITOSA';
  bool get estaEnProceso => estatus == 'EN_PROCESO' || estatus == 'PENDIENTE';

  @override
  List<Object?> get props => [
        idTransaccion,
        upc,
        idTxGestoPago,
        numeroAutorizacion,
        numeroCuenta,
        montoDebitado,
        saldoRestante,
        telefonoReferencia,
        idProducto,
        idServicio,
        estatus,
        mensaje,
        fechaTransaccion,
      ];
}


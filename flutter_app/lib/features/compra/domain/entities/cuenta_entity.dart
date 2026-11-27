import 'package:equatable/equatable.dart';

class CuentaEntity extends Equatable {
  final int id;
  final String numeroCuenta;
  final double saldo;
  final String estatus;

  const CuentaEntity({
    required this.id,
    required this.numeroCuenta,
    required this.saldo,
    required this.estatus,
  });

  @override
  List<Object?> get props => [id, numeroCuenta, saldo, estatus];
}


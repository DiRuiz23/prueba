import 'package:equatable/equatable.dart';
import '../../domain/entities/cuenta_entity.dart';
import '../../domain/entities/transaccion_entity.dart';

abstract class CompraState extends Equatable {
  final String idempotencyKey;
  final List<CuentaEntity> cuentas;
  final CuentaEntity? cuentaActiva;

  const CompraState({
    required this.idempotencyKey,
    this.cuentas = const [],
    this.cuentaActiva,
  });

  @override
  List<Object?> get props => [idempotencyKey, cuentas, cuentaActiva];
}

class CompraInitial extends CompraState {
  const CompraInitial({
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
  });
}

class CompraLoading extends CompraState {
  const CompraLoading({
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
  });
}

class CompraExitosa extends CompraState {
  final TransaccionEntity transaccion;

  const CompraExitosa({
    required this.transaccion,
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
  });

  @override
  List<Object?> get props => [transaccion, idempotencyKey, cuentas, cuentaActiva];
}

class CompraPendienteIncierta extends CompraState {
  final TransaccionEntity? transaccion;
  final String mensaje;

  const CompraPendienteIncierta({
    this.transaccion,
    required this.mensaje,
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
  });

  @override
  List<Object?> get props => [transaccion, mensaje, idempotencyKey, cuentas, cuentaActiva];
}

class CompraRechazada extends CompraState {
  final String mensaje;

  const CompraRechazada({
    required this.mensaje,
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
  });

  @override
  List<Object?> get props => [mensaje, idempotencyKey, cuentas, cuentaActiva];
}

class CompraErrorComunicacion extends CompraState {
  final String mensaje;

  const CompraErrorComunicacion({
    required this.mensaje,
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
  });

  @override
  List<Object?> get props => [mensaje, idempotencyKey, cuentas, cuentaActiva];
}

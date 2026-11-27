import 'package:equatable/equatable.dart';
import '../../domain/entities/cuenta_entity.dart';
import '../../domain/entities/transaccion_entity.dart';

abstract class CompraState extends Equatable {
  final String idempotencyKey;
  final List<CuentaEntity> cuentas;
  final CuentaEntity? cuentaActiva;
  final bool cargandoCuentas;
  final String? errorCuentas;

  const CompraState({
    required this.idempotencyKey,
    this.cuentas = const [],
    this.cuentaActiva,
    this.cargandoCuentas = false,
    this.errorCuentas,
  });

  @override
  List<Object?> get props => [
        idempotencyKey,
        cuentas,
        cuentaActiva,
        cargandoCuentas,
        errorCuentas,
      ];
}

class CompraInitial extends CompraState {
  const CompraInitial({
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
    super.cargandoCuentas,
    super.errorCuentas,
  });
}

class CompraLoading extends CompraState {
  const CompraLoading({
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
    super.cargandoCuentas,
    super.errorCuentas,
  });
}

class CompraExitosa extends CompraState {
  final TransaccionEntity transaccion;

  const CompraExitosa({
    required this.transaccion,
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
    super.cargandoCuentas,
    super.errorCuentas,
  });

  @override
  List<Object?> get props => [
        transaccion,
        idempotencyKey,
        cuentas,
        cuentaActiva,
        cargandoCuentas,
        errorCuentas,
      ];
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
    super.cargandoCuentas,
    super.errorCuentas,
  });

  @override
  List<Object?> get props => [
        transaccion,
        mensaje,
        idempotencyKey,
        cuentas,
        cuentaActiva,
        cargandoCuentas,
        errorCuentas,
      ];
}

class CompraRechazada extends CompraState {
  final String mensaje;
  final TransaccionEntity? transaccion;

  const CompraRechazada({
    required this.mensaje,
    this.transaccion,
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
    super.cargandoCuentas,
    super.errorCuentas,
  });

  @override
  List<Object?> get props => [
        mensaje,
        transaccion,
        idempotencyKey,
        cuentas,
        cuentaActiva,
        cargandoCuentas,
        errorCuentas,
      ];
}

class CompraErrorComunicacion extends CompraState {
  final String mensaje;

  const CompraErrorComunicacion({
    required this.mensaje,
    required super.idempotencyKey,
    super.cuentas,
    super.cuentaActiva,
    super.cargandoCuentas,
    super.errorCuentas,
  });

  @override
  List<Object?> get props => [
        mensaje,
        idempotencyKey,
        cuentas,
        cuentaActiva,
        cargandoCuentas,
        errorCuentas,
      ];
}

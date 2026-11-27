import 'dart:developer' as dev;
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:uuid/uuid.dart';
import '../../../../core/error/exceptions.dart';
import '../../data/models/compra_models.dart';
import '../../domain/entities/cuenta_entity.dart';
import '../../domain/entities/transaccion_entity.dart';
import '../../domain/repositories/compra_repository.dart';
import 'compra_state.dart';

class CompraCubit extends Cubit<CompraState> {
  final CompraRepository repository;
  final Uuid _uuid;

  CompraCubit({
    required this.repository,
    Uuid? uuid,
  })  : _uuid = uuid ?? const Uuid(),
        super(CompraInitial(idempotencyKey: (uuid ?? const Uuid()).v4()));

  Future<void> cargarCuentas(String correo) async {
    if (state.cargandoCuentas || correo.trim().isEmpty) return;
    emit(CompraInitial(
      idempotencyKey: state.idempotencyKey,
      cuentas: state.cuentas,
      cuentaActiva: state.cuentaActiva,
      cargandoCuentas: true,
    ));
    try {
      final lista = await repository.obtenerCuentasCliente(correo);
      final cuentas = List<CuentaEntity>.from(lista);
      CuentaEntity? activa = state.cuentaActiva;
      if (activa != null && cuentas.isNotEmpty) {
        final numeroPrevio = activa.numeroCuenta;
        activa = cuentas.firstWhere(
          (c) => c.numeroCuenta == numeroPrevio,
          orElse: () => cuentas.first,
        );
      } else if (cuentas.isNotEmpty) {
        activa = cuentas.firstWhere(
          (c) => c.estatus == 'ACTIVA',
          orElse: () => cuentas.first,
        );
      } else {
        activa = null;
      }
      emit(CompraInitial(
        idempotencyKey: state.idempotencyKey,
        cuentas: cuentas,
        cuentaActiva: activa,
        cargandoCuentas: false,
        errorCuentas: cuentas.isEmpty ? 'No se encontraron cuentas asociadas.' : null,
      ));
    } on UnauthorizedException catch (e) {
      emit(CompraInitial(
        idempotencyKey: state.idempotencyKey,
        cuentas: state.cuentas,
        cuentaActiva: state.cuentaActiva,
        cargandoCuentas: false,
        errorCuentas: e.message,
      ));
    } on ServerException catch (e) {
      emit(CompraInitial(
        idempotencyKey: state.idempotencyKey,
        cuentas: state.cuentas,
        cuentaActiva: state.cuentaActiva,
        cargandoCuentas: false,
        errorCuentas: e.message,
      ));
    } on NetworkTimeoutException catch (e) {
      emit(CompraInitial(
        idempotencyKey: state.idempotencyKey,
        cuentas: state.cuentas,
        cuentaActiva: state.cuentaActiva,
        cargandoCuentas: false,
        errorCuentas: e.message,
      ));
    } on NetworkConnectionException catch (e) {
      emit(CompraInitial(
        idempotencyKey: state.idempotencyKey,
        cuentas: state.cuentas,
        cuentaActiva: state.cuentaActiva,
        cargandoCuentas: false,
        errorCuentas: e.message,
      ));
    } catch (e, st) {
      dev.log('Error cargando cuentas: $e', name: 'CompraCubit', stackTrace: st);
      debugPrint('Error cargando cuentas: $e\n$st');
      final detalleRaw = e.toString().replaceAll('\n', ' ').trim();
      final detalle = detalleRaw.length > 140
          ? '${detalleRaw.substring(0, 140)}...'
          : detalleRaw;
      emit(CompraInitial(
        idempotencyKey: state.idempotencyKey,
        cuentas: state.cuentas,
        cuentaActiva: state.cuentaActiva,
        cargandoCuentas: false,
        errorCuentas: 'No se pudo consultar tu cuenta y saldo: $detalle',
      ));
    }
  }

  void limpiarCuentas() {
    emit(CompraInitial(idempotencyKey: _uuid.v4()));
  }

  void seleccionarCuenta(String numeroCuenta) {
    final encontrada = state.cuentas.where((c) => c.numeroCuenta == numeroCuenta).firstOrNull;
    if (encontrada != null) {
      emit(CompraInitial(
        idempotencyKey: state.idempotencyKey,
        cuentas: state.cuentas,
        cuentaActiva: encontrada,
      ));
    }
  }

  Future<void> ejecutarCompra({
    required String numeroCuenta,
    required int idProducto,
    required int idServicio,
    required String telefonoReferencia,
    required double monto,
    required String pin,
  }) async {
    if (state is CompraLoading) return;

    final key = state.idempotencyKey;
    final cuentasActuales = List<CuentaEntity>.from(state.cuentas);
    final activaActual = state.cuentaActiva;
    emit(CompraLoading(
      idempotencyKey: key,
      cuentas: cuentasActuales,
      cuentaActiva: activaActual,
    ));

    try {
      final tx = await repository.ejecutarPago(
        numeroCuenta: numeroCuenta,
        idProducto: idProducto,
        idServicio: idServicio,
        telefonoReferencia: telefonoReferencia,
        monto: monto,
        pin: pin,
        idempotencyKey: key,
      );

      if (tx.estaEnProceso) {
        emit(CompraPendienteIncierta(
          transaccion: tx,
          mensaje: tx.mensaje,
          idempotencyKey: key,
          cuentas: cuentasActuales,
          cuentaActiva: activaActual,
        ));
      } else if (tx.esExitosa) {
        CuentaEntity? activaActualizada = activaActual;
        List<CuentaEntity> cuentasActualizadas = cuentasActuales;
        if (tx.saldoRestante != null && activaActual != null) {
          activaActualizada = CuentaEntity(
            id: activaActual.id,
            numeroCuenta: activaActual.numeroCuenta,
            saldo: tx.saldoRestante!,
            estatus: activaActual.estatus,
          );
          cuentasActualizadas = cuentasActuales
              .map((c) => c.numeroCuenta == activaActual.numeroCuenta ? activaActualizada! : c)
              .toList();
        }
        emit(CompraExitosa(
          transaccion: tx,
          idempotencyKey: key,
          cuentas: cuentasActualizadas,
          cuentaActiva: activaActualizada,
        ));
      } else {
        emit(CompraRechazada(
          mensaje: tx.mensaje,
          transaccion: tx,
          idempotencyKey: key,
          cuentas: cuentasActuales,
          cuentaActiva: activaActual,
        ));
      }
    } on NetworkTimeoutException catch (e) {
      emit(CompraErrorComunicacion(
        mensaje: '${e.message} Verifica tu historial antes de reintentar.',
        idempotencyKey: key,
        cuentas: cuentasActuales,
        cuentaActiva: activaActual,
      ));
    } on NetworkConnectionException catch (e) {
      emit(CompraErrorComunicacion(
        mensaje: '${e.message} Verifica tu historial antes de reintentar.',
        idempotencyKey: key,
        cuentas: cuentasActuales,
        cuentaActiva: activaActual,
      ));
    } on ServerException catch (e) {
      if (e.statusCode == 503 || e.statusCode == 504) {
        emit(CompraErrorComunicacion(
          mensaje: e.message,
          idempotencyKey: key,
          cuentas: cuentasActuales,
          cuentaActiva: activaActual,
        ));
      } else {
        TransaccionEntity txRechazo;
        final rawDatos = (e.data is Map) ? (e.data as Map)['datos'] : null;
        if (rawDatos is Map) {
          txRechazo = TransaccionModel.fromJson(
            rawDatos.map((k, v) => MapEntry(k.toString(), v)),
          );
        } else {
          txRechazo = TransaccionEntity(
            idTransaccion: 0,
            upc: 'No generado (validación previa)',
            numeroCuenta: numeroCuenta,
            montoDebitado: monto,
            telefonoReferencia: telefonoReferencia,
            idProducto: idProducto,
            idServicio: idServicio,
            estatus: 'RECHAZADA',
            mensaje: e.message,
            fechaTransaccion: DateTime.now(),
          );
        }
        emit(CompraRechazada(
          mensaje: e.message,
          transaccion: txRechazo,
          idempotencyKey: key,
          cuentas: cuentasActuales,
          cuentaActiva: activaActual,
        ));
      }
    } catch (_) {
      emit(CompraErrorComunicacion(
        mensaje: 'Error inesperado durante la operación.',
        idempotencyKey: key,
        cuentas: cuentasActuales,
        cuentaActiva: activaActual,
      ));
    }
  }

  Future<void> verificarEstadoReal(int idTransaccion) async {
    if (state is CompraLoading) return;
    final key = state.idempotencyKey;
    final cuentasActuales = List<CuentaEntity>.from(state.cuentas);
    final activaActual = state.cuentaActiva;
    emit(CompraLoading(
      idempotencyKey: key,
      cuentas: cuentasActuales,
      cuentaActiva: activaActual,
    ));

    try {
      final tx = await repository.consultarEstadoTransaccion(idTransaccion);
      if (tx.esExitosa) {
        emit(CompraExitosa(
          transaccion: tx,
          idempotencyKey: key,
          cuentas: cuentasActuales,
          cuentaActiva: activaActual,
        ));
      } else if (tx.estaEnProceso) {
        emit(CompraPendienteIncierta(
          transaccion: tx,
          mensaje: tx.mensaje,
          idempotencyKey: key,
          cuentas: cuentasActuales,
          cuentaActiva: activaActual,
        ));
      } else {
        emit(CompraRechazada(
          mensaje: tx.mensaje,
          transaccion: tx,
          idempotencyKey: key,
          cuentas: cuentasActuales,
          cuentaActiva: activaActual,
        ));
      }
    } catch (_) {
      emit(CompraPendienteIncierta(
        mensaje: 'La transacción sigue pendiente de confirmación.',
        idempotencyKey: key,
        cuentas: cuentasActuales,
        cuentaActiva: activaActual,
      ));
    }
  }

  void prepararNuevaOperacion() {
    emit(CompraInitial(
      idempotencyKey: _uuid.v4(),
      cuentas: state.cuentas,
      cuentaActiva: state.cuentaActiva,
      cargandoCuentas: state.cargandoCuentas,
      errorCuentas: state.errorCuentas,
    ));
  }
}

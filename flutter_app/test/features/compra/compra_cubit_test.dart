import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:uuid/uuid.dart';

import 'package:apphasani_mobile/core/error/exceptions.dart';
import 'package:apphasani_mobile/features/compra/domain/entities/transaccion_entity.dart';
import 'package:apphasani_mobile/features/compra/domain/repositories/compra_repository.dart';
import 'package:apphasani_mobile/features/compra/presentation/cubit/compra_cubit.dart';
import 'package:apphasani_mobile/features/compra/presentation/cubit/compra_state.dart';

class MockCompraRepository extends Mock implements CompraRepository {}

class MockUuid extends Mock implements Uuid {}

void main() {
  late MockCompraRepository mockRepo;
  late MockUuid mockUuid;

  setUp(() {
    mockRepo = MockCompraRepository();
    mockUuid = MockUuid();
    when(() => mockUuid.v4()).thenReturn('idem-uuid-1234');
  });

  CompraCubit crearCubit() => CompraCubit(repository: mockRepo, uuid: mockUuid);

  group('CompraCubit - Idempotencia, Timeout y Estados de Compra', () {
    const txExitosa = TransaccionEntity(
      idTransaccion: 1001,
      upc: 'GP000001',
      numeroCuenta: '0120000000000001',
      montoDebitado: 50.0,
      saldoRestante: 950.0,
      telefonoReferencia: '5512345678',
      estatus: 'EXITOSA',
      mensaje: 'Operación realizada con éxito',
    );

    const txPendiente = TransaccionEntity(
      idTransaccion: 1002,
      upc: 'GP000002',
      numeroCuenta: '0120000000000001',
      montoDebitado: 100.0,
      telefonoReferencia: '5512345678',
      estatus: 'EN_PROCESO',
      mensaje: 'Operación en conciliación con el proveedor',
    );

    blocTest<CompraCubit, CompraState>(
      'emite CompraExitosa enviando la clave de idempotencia generada',
      build: () {
        when(() => mockRepo.ejecutarPago(
              numeroCuenta: '0120000000000001',
              idProducto: 582,
              idServicio: 133,
              telefonoReferencia: '5512345678',
              monto: 50.0,
              pin: '1234',
              idempotencyKey: 'idem-uuid-1234',
            )).thenAnswer((_) async => txExitosa);
        return crearCubit();
      },
      act: (cubit) => cubit.ejecutarCompra(
        numeroCuenta: '0120000000000001',
        idProducto: 582,
        idServicio: 133,
        telefonoReferencia: '5512345678',
        monto: 50.0,
        pin: '1234',
      ),
      expect: () => const [
        CompraLoading(idempotencyKey: 'idem-uuid-1234'),
        CompraExitosa(
          transaccion: txExitosa,
          idempotencyKey: 'idem-uuid-1234',
        ),
      ],
    );

    blocTest<CompraCubit, CompraState>(
      'emite CompraPendienteIncierta cuando el servidor retorna estatus EN_PROCESO (202)',
      build: () {
        when(() => mockRepo.ejecutarPago(
              numeroCuenta: '0120000000000001',
              idProducto: 582,
              idServicio: 133,
              telefonoReferencia: '5512345678',
              monto: 100.0,
              pin: '1234',
              idempotencyKey: 'idem-uuid-1234',
            )).thenAnswer((_) async => txPendiente);
        return crearCubit();
      },
      act: (cubit) => cubit.ejecutarCompra(
        numeroCuenta: '0120000000000001',
        idProducto: 582,
        idServicio: 133,
        telefonoReferencia: '5512345678',
        monto: 100.0,
        pin: '1234',
      ),
      expect: () => const [
        CompraLoading(idempotencyKey: 'idem-uuid-1234'),
        CompraPendienteIncierta(
          transaccion: txPendiente,
          mensaje: 'Operación en conciliación con el proveedor',
          idempotencyKey: 'idem-uuid-1234',
        ),
      ],
    );

    blocTest<CompraCubit, CompraState>(
      'conserva la misma Idempotency-Key ante timeout y emite CompraErrorComunicacion',
      build: () {
        when(() => mockRepo.ejecutarPago(
              numeroCuenta: any(named: 'numeroCuenta'),
              idProducto: any(named: 'idProducto'),
              idServicio: any(named: 'idServicio'),
              telefonoReferencia: any(named: 'telefonoReferencia'),
              monto: any(named: 'monto'),
              pin: any(named: 'pin'),
              idempotencyKey: 'idem-uuid-1234',
            )).thenThrow(const NetworkTimeoutException());
        return crearCubit();
      },
      act: (cubit) => cubit.ejecutarCompra(
        numeroCuenta: '0120000000000001',
        idProducto: 582,
        idServicio: 133,
        telefonoReferencia: '5512345678',
        monto: 50.0,
        pin: '1234',
      ),
      expect: () => const [
        CompraLoading(idempotencyKey: 'idem-uuid-1234'),
        CompraErrorComunicacion(
          mensaje: 'Tiempo de espera agotado al conectar con el servidor. Verifica tu historial antes de reintentar.',
          idempotencyKey: 'idem-uuid-1234',
        ),
      ],
    );

    blocTest<CompraCubit, CompraState>(
      'verificarEstadoReal consulta GET /transacciones/{id} y actualiza a CompraExitosa',
      build: () {
        when(() => mockRepo.consultarEstadoTransaccion(1002))
            .thenAnswer((_) async => txExitosa);
        return crearCubit();
      },
      act: (cubit) => cubit.verificarEstadoReal(1002),
      expect: () => const [
        CompraLoading(idempotencyKey: 'idem-uuid-1234'),
        CompraExitosa(
          transaccion: txExitosa,
          idempotencyKey: 'idem-uuid-1234',
        ),
      ],
    );
  });
}


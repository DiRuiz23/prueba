import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:apphasani_mobile/core/network/auth_interceptor.dart';
import 'package:apphasani_mobile/core/network/dio_client.dart';
import 'package:apphasani_mobile/features/compra/data/datasources/compra_remote_datasource.dart';
import 'package:apphasani_mobile/features/compra/data/models/compra_models.dart';

class MockFlutterSecureStorage extends Mock implements FlutterSecureStorage {}

class FakeHttpClientAdapter implements HttpClientAdapter {
  final Future<ResponseBody> Function(RequestOptions options) onFetch;

  FakeHttpClientAdapter(this.onFetch);

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    return onFetch(options);
  }

  @override
  void close({bool force = false}) {}
}

ResponseBody _jsonResponse(Object? body, {int statusCode = 200}) {
  final text = body is String ? body : jsonEncode(body);
  return ResponseBody.fromString(
    text,
    statusCode,
    headers: {
      Headers.contentTypeHeader: [Headers.jsonContentType],
    },
  );
}

void main() {
  late MockFlutterSecureStorage mockStorage;

  setUp(() {
    mockStorage = MockFlutterSecureStorage();
    when(() => mockStorage.read(key: any(named: 'key')))
        .thenAnswer((_) async => 'jwt-token-test');
    when(() => mockStorage.delete(key: any(named: 'key')))
        .thenAnswer((_) async {});
  });

  CompraRemoteDataSourceImpl crearDataSource(
    Future<ResponseBody> Function(RequestOptions options) handler,
  ) {
    final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8000/api/v1'));
    dio.httpClientAdapter = FakeHttpClientAdapter(handler);
    final interceptor = AuthInterceptor(secureStorage: mockStorage);
    final client = DioClient(authInterceptor: interceptor, customDio: dio);
    return CompraRemoteDataSourceImpl(dioClient: client);
  }

  group('CompraRemoteDataSourceImpl - Consulta tolerante de cuentas', () {
    test('obtiene cuentas directamente desde GET /cuentas/mias (num y String)', () async {
      final ds = crearDataSource((options) async {
        expect(options.path, '/cuentas/mias');
        return _jsonResponse({
          'codigo': 0,
          'mensaje': 'OK',
          'datos': [
            {
              'id': 10,
              'numeroCuenta': '0120000000001',
              'saldo': 1500.5,
              'estatus': 'ACTIVA',
            },
            {
              'id': '11',
              'numeroCuenta': '0120000000002',
              'saldo': '2,340.75',
              'estatus': 'ACTIVA',
            },
          ],
        });
      });

      final cuentas = await ds.obtenerCuentasCliente('cliente@correo.com');

      expect(cuentas.length, 2);
      expect(cuentas[0].numeroCuenta, '0120000000001');
      expect(cuentas[0].saldo, 1500.5);
      expect(cuentas[1].id, 11);
      expect(cuentas[1].saldo, 2340.75);
    });

    test('hace fallback a /clientes/consultar y /cuentas/consultar si /cuentas/mias falla', () async {
      final ds = crearDataSource((options) async {
        if (options.path == '/cuentas/mias') {
          return _jsonResponse({'codigo': 1, 'mensaje': 'No encontrado'}, statusCode: 404);
        }
        if (options.path == '/clientes/consultar') {
          return _jsonResponse({
            'codigo': 0,
            'datos': {
              'contenido': [
                {'id': 77, 'correo': 'cliente@correo.com'}
              ],
            },
          });
        }
        if (options.path == '/cuentas/consultar') {
          return _jsonResponse({
            'codigo': 0,
            'datos': {
              'contenido': [
                {
                  'id': 99,
                  'numeroCuenta': '0129999999999',
                  'saldo': 800,
                  'estatus': 'ACTIVA',
                }
              ],
            },
          });
        }
        return _jsonResponse({}, statusCode: 500);
      });

      final cuentas = await ds.obtenerCuentasCliente('cliente@correo.com');

      expect(cuentas.length, 1);
      expect(cuentas.first.numeroCuenta, '0129999999999');
      expect(cuentas.first.saldo, 800.0);
    });

    test('hace fallback a numeroCuentas + /cuentas/detalle si /cuentas/consultar regresa vacío', () async {
      final ds = crearDataSource((options) async {
        if (options.path == '/cuentas/mias') {
          return _jsonResponse({'codigo': 0, 'datos': []});
        }
        if (options.path == '/clientes/consultar') {
          return _jsonResponse({
            'codigo': 0,
            'datos': {
              'contenido': [
                {
                  'id': 88,
                  'correo': 'cliente@correo.com',
                  'numeroCuentas': ['0128888888888'],
                }
              ],
            },
          });
        }
        if (options.path == '/cuentas/consultar') {
          return _jsonResponse({'codigo': 0, 'datos': {'contenido': []}});
        }
        if (options.path == '/cuentas/detalle') {
          return _jsonResponse({
            'codigo': 0,
            'datos': {
              'id': 5,
              'numeroCuenta': '0128888888888',
              'saldo': 999.25,
              'estatus': 'ACTIVA',
            },
          });
        }
        return _jsonResponse({}, statusCode: 404);
      });

      final cuentas = await ds.obtenerCuentasCliente('cliente@correo.com');

      expect(cuentas.length, 1);
      expect(cuentas.first.numeroCuenta, '0128888888888');
      expect(cuentas.first.saldo, 999.25);
    });

    test('tolera respuesta como String JSON y elementos malformados sin lanzar _TypeError', () async {
      final ds = crearDataSource((options) async {
        return ResponseBody.fromString(
          jsonEncode({
            'codigo': 0,
            'datos': [
              'elemento-invalido',
              {'id': 1, 'numeroCuenta': '0120000000010', 'saldo': 120.0},
            ],
          }),
          200,
          headers: {
            Headers.contentTypeHeader: ['text/plain'],
          },
        );
      });

      final cuentas = await ds.obtenerCuentasCliente('cliente@correo.com');

      expect(cuentas.length, 1);
      expect(cuentas.first.numeroCuenta, '0120000000010');
      expect(cuentas.first.estatus, 'ACTIVA');
    });

    test('TransaccionModel.fromJson no lanza _TypeError si fechaTransaccion no es String', () {
      final tx = TransaccionModel.fromJson(const {
        'idTransaccion': 10,
        'upc': '1234567890',
        'numeroCuenta': '0120000000001',
        'montoDebitado': '50.00',
        'telefonoReferencia': '5512345678',
        'estatus': 'EXITOSA',
        'mensaje': 'OK',
        'fechaTransaccion': 1700000000,
      });

      expect(tx.upc, '1234567890');
      expect(tx.montoDebitado, 50.0);
    });
  });
}

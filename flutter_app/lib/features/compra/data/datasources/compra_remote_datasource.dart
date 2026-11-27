import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import '../../../../core/error/exceptions.dart';
import '../../../../core/network/dio_client.dart';
import '../models/compra_models.dart';

abstract class CompraRemoteDataSource {
  Future<List<CuentaModel>> obtenerCuentasCliente(String correo);

  Future<TransaccionModel> ejecutarPago({
    required String numeroCuenta,
    required int idProducto,
    required int idServicio,
    required String telefonoReferencia,
    required double monto,
    required String pin,
    required String idempotencyKey,
  });

  Future<TransaccionModel> consultarEstadoTransaccion(int idTransaccion);
}

class CompraRemoteDataSourceImpl implements CompraRemoteDataSource {
  final DioClient dioClient;

  CompraRemoteDataSourceImpl({required this.dioClient});

  Map<String, dynamic>? _asMap(dynamic data) {
    dynamic actual = data;
    if (actual is List<int>) {
      try {
        actual = utf8.decode(actual);
      } catch (_) {
        return null;
      }
    }
    if (actual is String && actual.trim().isNotEmpty) {
      try {
        actual = jsonDecode(actual);
      } catch (_) {
        return null;
      }
    }
    if (actual is Map) {
      final result = <String, dynamic>{};
      for (final entry in actual.entries) {
        result[entry.key.toString()] = entry.value;
      }
      return result;
    }
    return null;
  }

  List<Map<String, dynamic>> _asMapList(dynamic data) {
    dynamic actual = data;
    if (actual is List<int>) {
      try {
        actual = utf8.decode(actual);
      } catch (_) {
        return const [];
      }
    }
    if (actual is String && actual.trim().isNotEmpty) {
      try {
        actual = jsonDecode(actual);
      } catch (_) {
        return const [];
      }
    }
    if (actual is List) {
      final result = <Map<String, dynamic>>[];
      for (final item in actual) {
        final m = _asMap(item);
        if (m != null) result.add(m);
      }
      return result;
    }
    final mapa = _asMap(actual);
    if (mapa != null) {
      final inner = mapa['contenido'] ??
          mapa['content'] ??
          mapa['cuentas'] ??
          mapa['items'];
      if (inner is List) {
        final result = <Map<String, dynamic>>[];
        for (final item in inner) {
          final m = _asMap(item);
          if (m != null) result.add(m);
        }
        return result;
      }
      if (mapa.containsKey('numeroCuenta') || mapa.containsKey('numero_cuenta')) {
        return [mapa];
      }
    }
    return const [];
  }

  List<CuentaModel> _parseCuentas(dynamic raw) {
    final result = <CuentaModel>[];
    for (final item in _asMapList(raw)) {
      try {
        final cuenta = CuentaModel.fromJson(item);
        if (cuenta.numeroCuenta.isNotEmpty) {
          result.add(cuenta);
        }
      } catch (e, st) {
        debugPrint('Error parseando CuentaModel ($item): $e\n$st');
      }
    }
    return result;
  }

  int? _extractInt(dynamic raw) {
    if (raw is int) return raw;
    if (raw is num) return raw.toInt();
    return int.tryParse(raw?.toString() ?? '');
  }

  @override
  Future<List<CuentaModel>> obtenerCuentasCliente(String correo) async {
    Exception? ultimoError;

    try {
      final directResp = await dioClient.get('/cuentas/mias');
      final directBody = _asMap(directResp.data);
      final cuentas = _parseCuentas(directBody?['datos']);
      if (cuentas.isNotEmpty || correo.trim().isEmpty) {
        return cuentas;
      }
    } on UnauthorizedException {
      rethrow;
    } on Exception catch (e, st) {
      ultimoError = e;
      debugPrint('Fallback desde /cuentas/mias: $e\n$st');
    } catch (e, st) {
      debugPrint('Error inesperado en /cuentas/mias: $e\n$st');
    }

    final correoNorm = correo.trim().toLowerCase();
    if (correoNorm.isEmpty) {
      if (ultimoError != null) throw ultimoError;
      return const [];
    }

    Map<String, dynamic>? clienteExacto;
    try {
      final cliResp = await dioClient.post(
        '/clientes/consultar',
        data: {
          'correo': correoNorm,
          'paginacion': {'pagina': 0, 'tamanio': 10},
        },
      );

      final cliBody = _asMap(cliResp.data);
      final clientes = _asMapList(cliBody?['datos']);
      if (clientes.isEmpty) {
        return const [];
      }

      clienteExacto = clientes.first;
      for (final c in clientes) {
        final correoItem = (c['correo']?.toString() ?? '').trim().toLowerCase();
        if (correoItem == correoNorm) {
          clienteExacto = c;
          break;
        }
      }
    } on UnauthorizedException {
      rethrow;
    } on Exception catch (e, st) {
      ultimoError ??= e;
      debugPrint('Error en /clientes/consultar: $e\n$st');
      throw ultimoError;
    } catch (e, st) {
      debugPrint('Error inesperado en /clientes/consultar: $e\n$st');
      if (ultimoError != null) throw ultimoError;
      return const [];
    }

    final clienteId = _extractInt(clienteExacto?['id']);
    if (clienteId != null) {
      try {
        final ctaResp = await dioClient.post(
          '/cuentas/consultar',
          data: {
            'clienteId': clienteId,
            'paginacion': {'pagina': 0, 'tamanio': 10},
          },
        );

        final ctaBody = _asMap(ctaResp.data);
        final lista = _parseCuentas(ctaBody?['datos']);
        if (lista.isNotEmpty) return lista;
      } on UnauthorizedException {
        rethrow;
      } on Exception catch (e, st) {
        ultimoError ??= e;
        debugPrint('Fallback desde /cuentas/consultar: $e\n$st');
      } catch (e, st) {
        debugPrint('Error inesperado en /cuentas/consultar: $e\n$st');
      }
    }

    final rawNumeros = clienteExacto?['numeroCuentas'];
    if (rawNumeros is List && rawNumeros.isNotEmpty) {
      final cuentasDetalle = <CuentaModel>[];
      for (final item in rawNumeros) {
        final numCta = item?.toString().trim() ?? '';
        if (numCta.isEmpty) continue;
        try {
          final detResp = await dioClient.post(
            '/cuentas/detalle',
            data: {'numeroCuenta': numCta},
          );
          final detBody = _asMap(detResp.data);
          final detDatos = _asMap(detBody?['datos']);
          if (detDatos != null) {
            final cuenta = CuentaModel.fromJson(detDatos);
            if (cuenta.numeroCuenta.isNotEmpty) {
              cuentasDetalle.add(cuenta);
            }
          }
        } on UnauthorizedException {
          rethrow;
        } on Exception catch (e, st) {
          ultimoError ??= e;
          debugPrint('Error en /cuentas/detalle ($numCta): $e\n$st');
        } catch (e, st) {
          debugPrint('Error inesperado en /cuentas/detalle ($numCta): $e\n$st');
        }
      }
      if (cuentasDetalle.isNotEmpty) return cuentasDetalle;
    }

    if (ultimoError != null) {
      throw ultimoError;
    }
    return const [];
  }

  @override
  Future<TransaccionModel> ejecutarPago({
    required String numeroCuenta,
    required int idProducto,
    required int idServicio,
    required String telefonoReferencia,
    required double monto,
    required String pin,
    required String idempotencyKey,
  }) async {
    final response = await dioClient.post(
      '/transacciones/pago-servicio',
      data: {
        'numeroCuenta': numeroCuenta,
        'idProducto': idProducto,
        'idServicio': idServicio,
        'telefonoReferencia': telefonoReferencia,
        'monto': monto,
        'pin': pin,
      },
      options: Options(headers: {'Idempotency-Key': idempotencyKey}),
    );

    final body = _asMap(response.data);
    final datos = _asMap(body?['datos']);
    if (datos == null) {
      throw ServerException(
        message: body?['mensaje']?.toString() ?? 'Respuesta sin comprobante de transacción',
        statusCode: response.statusCode,
      );
    }
    return TransaccionModel.fromJson(datos);
  }

  @override
  Future<TransaccionModel> consultarEstadoTransaccion(int idTransaccion) async {
    final response = await dioClient.get('/transacciones/$idTransaccion');
    final body = _asMap(response.data);
    final datos = _asMap(body?['datos']);
    if (datos == null) {
      throw ServerException(
        message: body?['mensaje']?.toString() ?? 'No se encontró la transacción',
        statusCode: response.statusCode,
      );
    }
    return TransaccionModel.fromJson(datos);
  }
}


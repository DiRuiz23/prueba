import 'dart:async';
import 'dart:developer' as dev;
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../config/env_config.dart';

class AuthInterceptor extends Interceptor {
  final FlutterSecureStorage secureStorage;
  String? _tokenMemoria;
  final StreamController<String> _sessionExpiredController =
      StreamController<String>.broadcast();

  Stream<String> get onSessionExpired => _sessionExpiredController.stream;

  AuthInterceptor({required this.secureStorage});

  void actualizarToken(String? token) {
    _tokenMemoria = (token != null && token.isNotEmpty) ? token : null;
  }

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final bool isPublicRoute = options.path.contains('/auth/login') ||
        options.path.contains('/auth/verificar-2fa') ||
        options.path.contains('/auth/forzar-cierre-sesion') ||
        options.path.contains('/clientes/disponibilidad') ||
        options.path.contains('/clientes/verificar-correo') ||
        options.path.contains('/catalogos') ||
        (options.path == '/clientes' && options.method.toUpperCase() == 'POST');

    if (!isPublicRoute) {
      String? token = _tokenMemoria;
      if (token == null || token.isEmpty) {
        try {
          token = await secureStorage.read(key: EnvConfig.tokenStorageKey);
          if (token != null && token.isNotEmpty) {
            _tokenMemoria = token;
          }
        } catch (e) {
          dev.log('No se pudo leer token de almacenamiento seguro: $e', name: 'DioClient');
        }
      }
      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }

    try {
      dev.log(
        '[HTTP REQUEST] ${options.method} ${options.path} '
        '(headers=${_maskHeaders(options.headers)}, body=${_maskBody(options.data)})',
        name: 'DioClient',
      );
    } catch (_) {}

    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    try {
      dev.log(
        '[HTTP RESPONSE] ${response.statusCode} ${response.requestOptions.path}',
        name: 'DioClient',
      );
    } catch (_) {}
    handler.next(response);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final status = err.response?.statusCode;
    final path = err.requestOptions.path;
    final isAuthRoute = path.contains('/auth/');
    final preserveSessionOn403 = status == 403 &&
        (path.contains('/pago-servicio') ||
            path.contains('/cuentas/mias') ||
            path.contains('/seguridad/totp'));

    try {
      dev.log(
        '[HTTP ERROR] status=$status path=$path type=${err.type}',
        name: 'DioClient',
      );
    } catch (_) {}

    if ((status == 401 || (status == 403 && !preserveSessionOn403)) && !isAuthRoute) {
      _tokenMemoria = null;
      try {
        await secureStorage.delete(key: EnvConfig.tokenStorageKey);
        await secureStorage.delete(key: EnvConfig.emailStorageKey);
      } catch (e) {
        dev.log('No se pudo limpiar almacenamiento seguro: $e', name: 'DioClient');
      }
      _sessionExpiredController.add(
        'Tu sesión ha expirado o fue cerrada porque iniciaste sesión en otro dispositivo.',
      );
    }

    handler.next(err);
  }

  Map<String, dynamic> _maskHeaders(Map<dynamic, dynamic> headers) {
    final masked = <String, dynamic>{};
    for (final entry in headers.entries) {
      final key = entry.key.toString();
      masked[key] = key.toLowerCase() == 'authorization'
          ? 'Bearer ***PROTEGIDO***'
          : entry.value;
    }
    return masked;
  }

  dynamic _maskBody(dynamic body) {
    if (body is Map) {
      final masked = <String, dynamic>{};
      for (final entry in body.entries) {
        final key = entry.key.toString();
        masked[key] = (key == 'password' || key == 'pin' || key == 'codigo')
            ? '***PROTEGIDO***'
            : entry.value;
      }
      return masked;
    }
    return body;
  }

  void dispose() {
    _sessionExpiredController.close();
  }
}


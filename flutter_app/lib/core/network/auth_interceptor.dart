import 'dart:async';
import 'dart:developer' as dev;
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../config/env_config.dart';

class AuthInterceptor extends Interceptor {
  final FlutterSecureStorage secureStorage;
  final StreamController<String> _sessionExpiredController =
      StreamController<String>.broadcast();

  Stream<String> get onSessionExpired => _sessionExpiredController.stream;

  AuthInterceptor({required this.secureStorage});

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final bool isPublicAuth = options.path.contains('/auth/login') ||
        options.path.contains('/auth/verificar-2fa') ||
        options.path.contains('/auth/forzar-cierre-sesion');

    if (!isPublicAuth) {
      final token = await secureStorage.read(key: EnvConfig.tokenStorageKey);
      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }

    dev.log(
      '[HTTP REQUEST] ${options.method} ${options.path} '
      '(headers=${_maskHeaders(options.headers)}, body=${_maskBody(options.data)})',
      name: 'DioClient',
    );

    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    dev.log(
      '[HTTP RESPONSE] ${response.statusCode} ${response.requestOptions.path}',
      name: 'DioClient',
    );
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
    final isPaymentPinCheck = path.contains('/pago-servicio') && status == 403;

    dev.log(
      '[HTTP ERROR] status=$status path=$path type=${err.type}',
      name: 'DioClient',
    );

    if ((status == 401 || (status == 403 && !isPaymentPinCheck)) && !isAuthRoute) {
      await secureStorage.delete(key: EnvConfig.tokenStorageKey);
      await secureStorage.delete(key: EnvConfig.emailStorageKey);
      _sessionExpiredController.add(
        'Tu sesión ha expirado o fue cerrada porque iniciaste sesión en otro dispositivo.',
      );
    }

    handler.next(err);
  }

  Map<String, dynamic> _maskHeaders(Map<String, dynamic> headers) {
    final masked = Map<String, dynamic>.from(headers);
    if (masked.containsKey('Authorization')) {
      masked['Authorization'] = 'Bearer ***PROTEGIDO***';
    }
    return masked;
  }

  dynamic _maskBody(dynamic body) {
    if (body is Map) {
      final masked = Map<String, dynamic>.from(body);
      for (final sensitive in ['password', 'pin', 'codigo']) {
        if (masked.containsKey(sensitive)) {
          masked[sensitive] = '***PROTEGIDO***';
        }
      }
      return masked;
    }
    return body;
  }

  void dispose() {
    _sessionExpiredController.close();
  }
}


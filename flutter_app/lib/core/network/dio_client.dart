import 'package:dio/dio.dart';
import '../config/env_config.dart';
import '../error/exceptions.dart';
import 'auth_interceptor.dart';

class DioClient {
  final Dio dio;
  final AuthInterceptor authInterceptor;

  DioClient({required this.authInterceptor, Dio? customDio})
      : dio = customDio ??
            Dio(
              BaseOptions(
                baseUrl: EnvConfig.baseUrl,
                connectTimeout: EnvConfig.connectTimeout,
                receiveTimeout: EnvConfig.receiveTimeout,
                sendTimeout: EnvConfig.sendTimeout,
                headers: {
                  'Content-Type': 'application/json',
                  'Accept': 'application/json',
                },
              ),
            ) {
    dio.interceptors.add(authInterceptor);
  }

  Future<Response<dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
    Options? options,
  }) async {
    try {
      return await dio.get(path, queryParameters: queryParameters, options: options);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Future<Response<dynamic>> post(
    String path, {
    dynamic data,
    Options? options,
  }) async {
    try {
      return await dio.post(path, data: data, options: options);
    } on DioException catch (e) {
      throw _mapDioException(e);
    }
  }

  Exception _mapDioException(DioException e) {
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout) {
      return const NetworkTimeoutException();
    }

    if (e.type == DioExceptionType.connectionError) {
      return const NetworkConnectionException();
    }

    final status = e.response?.statusCode;
    final body = e.response?.data;
    String mensaje = 'Ocurrió un error al procesar la solicitud.';

    if (body is Map<String, dynamic> && body['mensaje'] is String) {
      mensaje = body['mensaje'] as String;
    }

    if (status == 401) {
      return UnauthorizedException(mensaje);
    }

    return ServerException(
      message: mensaje,
      statusCode: status,
      data: body,
    );
  }
}


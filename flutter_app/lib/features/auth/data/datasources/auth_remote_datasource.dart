import '../../../../core/error/exceptions.dart';
import '../../../../core/network/dio_client.dart';
import '../models/auth_models.dart';

abstract class AuthRemoteDataSource {
  Future<LoginResponseModel> login({
    required String correo,
    required String password,
    required bool forzarCierreSesionAnterior,
  });

  Future<LoginResponseModel> verificarOtp({
    required String correo,
    required String codigo,
  });

  Future<void> logout();
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final DioClient dioClient;

  AuthRemoteDataSourceImpl({required this.dioClient});

  @override
  Future<LoginResponseModel> login({
    required String correo,
    required String password,
    required bool forzarCierreSesionAnterior,
  }) async {
    final response = await dioClient.post(
      '/auth/login',
      data: {
        'correo': correo.trim().toLowerCase(),
        'password': password,
        'dispositivo': 'MOVIL',
        'forzarCierreSesionAnterior': forzarCierreSesionAnterior,
      },
    );

    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'Respuesta inválida de autenticación',
        statusCode: response.statusCode,
      );
    }
    return LoginResponseModel.fromJson(datos);
  }

  @override
  Future<LoginResponseModel> verificarOtp({
    required String correo,
    required String codigo,
  }) async {
    final response = await dioClient.post(
      '/auth/verificar-2fa',
      data: {
        'correo': correo.trim().toLowerCase(),
        'codigo': codigo.trim(),
        'dispositivo': 'MOVIL',
      },
    );

    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'Respuesta inválida al validar 2FA',
        statusCode: response.statusCode,
      );
    }
    return LoginResponseModel.fromJson(datos);
  }

  @override
  Future<void> logout() async {
    try {
      await dioClient.post('/auth/logout');
    } catch (_) {
      // Aun si falla la red al cerrar sesión, la sesión local debe limpiarse.
    }
  }
}


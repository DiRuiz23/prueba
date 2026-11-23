import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../../core/config/env_config.dart';
import '../../domain/entities/auth_result_entity.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final FlutterSecureStorage secureStorage;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.secureStorage,
  });

  @override
  Future<AuthResultEntity> login({
    required String correo,
    required String password,
    bool forzarCierreSesionAnterior = false,
  }) {
    return remoteDataSource.login(
      correo: correo,
      password: password,
      forzarCierreSesionAnterior: forzarCierreSesionAnterior,
    );
  }

  @override
  Future<AuthResultEntity> verificarOtp({
    required String correo,
    required String codigo,
  }) async {
    final resultado = await remoteDataSource.verificarOtp(
      correo: correo,
      codigo: codigo,
    );

    if (resultado.token != null && resultado.token!.isNotEmpty) {
      await secureStorage.write(
        key: EnvConfig.tokenStorageKey,
        value: resultado.token,
      );
      await secureStorage.write(
        key: EnvConfig.emailStorageKey,
        value: resultado.correo,
      );
    }

    return resultado;
  }

  @override
  Future<void> logout() async {
    await remoteDataSource.logout();
    await secureStorage.delete(key: EnvConfig.tokenStorageKey);
    await secureStorage.delete(key: EnvConfig.emailStorageKey);
  }

  @override
  Future<String?> obtenerUsuarioAutenticado() {
    return secureStorage.read(key: EnvConfig.emailStorageKey);
  }

  @override
  Future<bool> tieneSesionLocalVigente() async {
    final token = await secureStorage.read(key: EnvConfig.tokenStorageKey);
    if (token == null || token.isEmpty) {
      return false;
    }
    try {
      final partes = token.split('.');
      if (partes.length != 3) return false;
      final normalized = base64Url.normalize(partes[1]);
      final payloadJson = utf8.decode(base64Url.decode(normalized));
      final payload = jsonDecode(payloadJson) as Map<String, dynamic>;
      final exp = payload['exp'] as int?;
      if (exp == null) return true;
      return DateTime.now().millisecondsSinceEpoch < exp * 1000;
    } catch (_) {
      return false;
    }
  }
}


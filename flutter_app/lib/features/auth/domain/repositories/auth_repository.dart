import '../entities/auth_result_entity.dart';

abstract class AuthRepository {
  Future<AuthResultEntity> login({
    required String correo,
    required String password,
    bool forzarCierreSesionAnterior = false,
  });

  Future<AuthResultEntity> verificarOtp({
    required String correo,
    required String codigo,
  });

  Future<void> logout();

  Future<String?> obtenerUsuarioAutenticado();

  Future<bool> tieneSesionLocalVigente();
}


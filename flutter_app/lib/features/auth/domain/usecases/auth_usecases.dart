import '../entities/auth_result_entity.dart';
import '../repositories/auth_repository.dart';

class LoginUseCase {
  final AuthRepository repository;

  LoginUseCase(this.repository);

  Future<AuthResultEntity> call({
    required String correo,
    required String password,
    bool forzarCierreSesionAnterior = false,
  }) {
    return repository.login(
      correo: correo,
      password: password,
      forzarCierreSesionAnterior: forzarCierreSesionAnterior,
    );
  }
}

class VerificarOtpUseCase {
  final AuthRepository repository;

  VerificarOtpUseCase(this.repository);

  Future<AuthResultEntity> call({
    required String correo,
    required String codigo,
  }) {
    return repository.verificarOtp(correo: correo, codigo: codigo);
  }
}

class LogoutUseCase {
  final AuthRepository repository;

  LogoutUseCase(this.repository);

  Future<void> call() => repository.logout();
}


import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import 'package:apphasani_mobile/core/error/exceptions.dart';
import 'package:apphasani_mobile/features/auth/data/models/registro_cliente_model.dart';
import 'package:apphasani_mobile/features/auth/domain/entities/auth_result_entity.dart';
import 'package:apphasani_mobile/features/auth/domain/repositories/auth_repository.dart';
import 'package:apphasani_mobile/features/auth/domain/usecases/auth_usecases.dart';
import 'package:apphasani_mobile/features/auth/presentation/cubit/auth_cubit.dart';
import 'package:apphasani_mobile/features/auth/presentation/cubit/auth_state.dart';

class MockAuthRepository extends Mock implements AuthRepository {}

class FakeRegistroClienteParams extends Fake implements RegistroClienteParams {}

void main() {
  setUpAll(() {
    registerFallbackValue(FakeRegistroClienteParams());
  });
  late MockAuthRepository mockRepo;
  late LoginUseCase loginUseCase;
  late VerificarOtpUseCase verificarOtpUseCase;
  late LogoutUseCase logoutUseCase;

  setUp(() {
    mockRepo = MockAuthRepository();
    loginUseCase = LoginUseCase(mockRepo);
    verificarOtpUseCase = VerificarOtpUseCase(mockRepo);
    logoutUseCase = LogoutUseCase(mockRepo);
  });

  AuthCubit crearCubit() => AuthCubit(
        loginUseCase: loginUseCase,
        verificarOtpUseCase: verificarOtpUseCase,
        logoutUseCase: logoutUseCase,
        authRepository: mockRepo,
      );

  group('AuthCubit - Flujo de autenticación, 2FA y sesión única', () {
    blocTest<AuthCubit, AuthState>(
      'emite AuthOtpRequired con Google Authenticator y datos de vinculación QR/clave cuando requiere 2FA',
      build: () {
        when(() => mockRepo.login(
              correo: 'usuario@banco.com',
              password: 'Password123',
              forzarCierreSesionAnterior: false,
            )).thenAnswer(
          (_) async => const AuthResultEntity(
            correo: 'usuario@banco.com',
            requiere2fa: true,
            metodo2fa: 'GOOGLE_AUTHENTICATOR',
            requiereConfiguracionTotp: true,
            totpSecreto: 'JBSWY3DPEHPK3PXP',
            totpOtpAuthUri: 'otpauth://totp/AppHasani:usuario@banco.com?secret=JBSWY3DPEHPK3PXP',
            totpQrBase64: 'iVBORw0KGgo=',
            tiempoExpiracionOtpSegundos: 300,
            mensaje: 'Vincula Google Authenticator',
          ),
        );
        return crearCubit();
      },
      act: (cubit) => cubit.iniciarSesion(
        correo: 'usuario@banco.com',
        password: 'Password123',
      ),
      expect: () => const [
        AuthLoading('Validando credenciales...'),
        AuthOtpRequired(
          correo: 'usuario@banco.com',
          segundosExpiracion: 300,
          mensaje: 'Vincula Google Authenticator',
          metodo2fa: 'GOOGLE_AUTHENTICATOR',
          requiereConfiguracionTotp: true,
          totpSecreto: 'JBSWY3DPEHPK3PXP',
          totpOtpAuthUri: 'otpauth://totp/AppHasani:usuario@banco.com?secret=JBSWY3DPEHPK3PXP',
          totpQrBase64: 'iVBORw0KGgo=',
        ),
      ],
    );

    blocTest<AuthCubit, AuthState>(
      'emite AuthActiveSessionConflict cuando existe sesión previa en Web',
      build: () {
        when(() => mockRepo.login(
              correo: 'usuario@banco.com',
              password: 'Password123',
              forzarCierreSesionAnterior: false,
            )).thenAnswer(
          (_) async => const AuthResultEntity(
            correo: 'usuario@banco.com',
            sesionActivaDetectada: true,
            dispositivoSesionActiva: 'WEB',
            mensaje: 'Sesión activa detectada',
          ),
        );
        return crearCubit();
      },
      act: (cubit) => cubit.iniciarSesion(
        correo: 'usuario@banco.com',
        password: 'Password123',
      ),
      expect: () => const [
        AuthLoading('Validando credenciales...'),
        AuthActiveSessionConflict(
          correo: 'usuario@banco.com',
          password: 'Password123',
          dispositivoActivo: 'WEB',
          mensaje: 'Sesión activa detectada',
        ),
      ],
    );

    blocTest<AuthCubit, AuthState>(
      'emite AuthAuthenticated cuando el código OTP 2FA es válido',
      build: () {
        when(() => mockRepo.verificarOtp(
              correo: 'usuario@banco.com',
              codigo: '123456',
            )).thenAnswer(
          (_) async => const AuthResultEntity(
            correo: 'usuario@banco.com',
            token: 'jwt-token-valido',
            requiere2fa: false,
          ),
        );
        return crearCubit();
      },
      act: (cubit) => cubit.validarOtp(
        correo: 'usuario@banco.com',
        codigo: '123456',
      ),
      expect: () => const [
        AuthLoading('Verificando segundo factor (2FA)...'),
        AuthAuthenticated(correo: 'usuario@banco.com'),
      ],
    );

    blocTest<AuthCubit, AuthState>(
      'emite AuthError cuando el código OTP es rechazado por servidor',
      build: () {
        when(() => mockRepo.verificarOtp(
              correo: 'usuario@banco.com',
              codigo: '000000',
            )).thenThrow(const ServerException(message: 'Código OTP incorrecto', statusCode: 403));
        return crearCubit();
      },
      act: (cubit) => cubit.validarOtp(
        correo: 'usuario@banco.com',
        codigo: '000000',
      ),
      expect: () => const [
        AuthLoading('Verificando segundo factor (2FA)...'),
        AuthError(
          mensaje: 'Código OTP incorrecto',
          duranteOtp: true,
          correoEnProceso: 'usuario@banco.com',
        ),
      ],
    );

    blocTest<AuthCubit, AuthState>(
      'cerrarSesion limpia sesión y emite AuthUnauthenticated',
      build: () {
        when(() => mockRepo.logout()).thenAnswer((_) async {});
        return crearCubit();
      },
      act: (cubit) => cubit.cerrarSesion('Sesión finalizada'),
      expect: () => const [
        AuthUnauthenticated('Sesión finalizada'),
      ],
    );

    blocTest<AuthCubit, AuthState>(
      'registrarCliente emite AuthRegistroExitoso cuando el alta es válida',
      build: () {
        when(() => mockRepo.registrarCliente(any())).thenAnswer(
          (_) async => const RegistroClienteResult(
            id: 20,
            nombreCompleto: 'Ana López Ruiz',
            correo: 'ana@banco.com',
            numeroCuenta: '0123456789012',
          ),
        );
        return crearCubit();
      },
      act: (cubit) => cubit.registrarCliente(
        const RegistroClienteParams(
          nombre: 'Ana',
          apellidoPaterno: 'López',
          apellidoMaterno: 'Ruiz',
          fechaNacimiento: '1998-05-10',
          curp: 'LORA980510MDFRN09',
          rfc: 'LORA980510ABC',
          sexo: 'M',
          estadoCivil: 'SOLTERO',
          correo: 'ana@banco.com',
          telefonoMovil: '5512345678',
          ocupacion: 'Ingeniera',
          empresa: 'Tech',
          ingresoMensual: 15000,
          calle: 'Reforma',
          numeroExterior: '100',
          colonia: 'Juárez',
          municipio: 'Cuauhtémoc',
          estado: 'Ciudad de México',
          codigoPostal: '06600',
          password: 'Password123!',
        ),
      ),
      expect: () => const [
        AuthLoading('Creando cuenta...'),
        AuthRegistroExitoso(
          RegistroClienteResult(
            id: 20,
            nombreCompleto: 'Ana López Ruiz',
            correo: 'ana@banco.com',
            numeroCuenta: '0123456789012',
          ),
        ),
      ],
    );
  });
}


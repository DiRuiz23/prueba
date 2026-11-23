import '../../domain/entities/auth_result_entity.dart';

class LoginResponseModel extends AuthResultEntity {
  const LoginResponseModel({
    super.token,
    required super.correo,
    super.requiere2fa,
    super.sesionActivaDetectada,
    super.dispositivoSesionActiva,
    super.tiempoExpiracionOtpSegundos,
    super.mensaje,
  });

  factory LoginResponseModel.fromJson(Map<String, dynamic> json) {
    return LoginResponseModel(
      token: json['token'] as String?,
      correo: (json['correo'] as String?) ?? '',
      requiere2fa: (json['requiere2fa'] as bool?) ?? false,
      sesionActivaDetectada: (json['sesionActivaDetectada'] as bool?) ?? false,
      dispositivoSesionActiva: json['dispositivoSesionActiva'] as String?,
      tiempoExpiracionOtpSegundos: json['tiempoExpiracionOtpSegundos'] as int?,
      mensaje: json['mensaje'] as String?,
    );
  }
}


import '../../domain/entities/auth_result_entity.dart';

class LoginResponseModel extends AuthResultEntity {
  const LoginResponseModel({
    super.token,
    required super.correo,
    super.requiere2fa,
    super.metodo2fa,
    super.requiereConfiguracionTotp,
    super.totpSecreto,
    super.totpOtpAuthUri,
    super.totpQrBase64,
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
      metodo2fa: (json['metodo2fa'] as String?) ?? 'GOOGLE_AUTHENTICATOR',
      requiereConfiguracionTotp: (json['requiereConfiguracionTotp'] as bool?) ?? false,
      totpSecreto: json['totpSecreto'] as String?,
      totpOtpAuthUri: json['totpOtpAuthUri'] as String?,
      totpQrBase64: json['totpQrBase64'] as String?,
      sesionActivaDetectada: (json['sesionActivaDetectada'] as bool?) ?? false,
      dispositivoSesionActiva: json['dispositivoSesionActiva'] as String?,
      tiempoExpiracionOtpSegundos: json['tiempoExpiracionOtpSegundos'] as int?,
      mensaje: json['mensaje'] as String?,
    );
  }
}


class EnvConfig {
  EnvConfig._();

  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8000/api/v1',
  );

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 25);
  static const Duration sendTimeout = Duration(seconds: 15);

  static const String tokenStorageKey = 'apphasani_jwt_token';
  static const String emailStorageKey = 'apphasani_user_email';
}


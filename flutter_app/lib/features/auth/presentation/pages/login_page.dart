import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/utils/input_formatters.dart';
import '../../../../core/widgets/auth_aura_background.dart';
import '../cubit/auth_cubit.dart';
import '../cubit/auth_state.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _correoCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _ocultarPassword = true;

  @override
  void dispose() {
    _correoCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  void _enviarLogin(BuildContext context) {
    if (!_formKey.currentState!.validate()) return;
    context.read<AuthCubit>().iniciarSesion(
          correo: _correoCtrl.text.trim(),
          password: _passwordCtrl.text,
        );
  }

  Future<void> _mostrarDialogoConflicto(
    BuildContext context,
    AuthActiveSessionConflict conflicto,
  ) async {
    final cubit = context.read<AuthCubit>();
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text('Sesión activa en ${conflicto.dispositivoActivo}'),
        content: const Text(
          'Solo se permite una sesión a la vez. Si continúas, la sesión anterior se cerrará.',
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              cubit.cancelarConflicto();
            },
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: AppColors.danger,
              minimumSize: const Size(0, 44),
            ),
            onPressed: () {
              Navigator.of(ctx).pop();
              cubit.iniciarSesion(
                correo: conflicto.correo,
                password: conflicto.password,
                forzarCierre: true,
              );
            },
            child: const Text('Cerrar la otra sesión'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AuthAuraBackground(
        child: BlocConsumer<AuthCubit, AuthState>(
          listener: (context, state) {
            if (state is AuthActiveSessionConflict) {
              _mostrarDialogoConflicto(context, state);
            } else if (state is AuthOtpRequired) {
              context.go('/otp', extra: {
                'correo': state.correo,
                'segundos': state.segundosExpiracion,
                'mensaje': state.mensaje,
                'metodo2fa': state.metodo2fa,
                'requiereConfiguracionTotp': state.requiereConfiguracionTotp,
                'totpSecreto': state.totpSecreto,
                'totpOtpAuthUri': state.totpOtpAuthUri,
                'totpQrBase64': state.totpQrBase64,
              });
            } else if (state is AuthAuthenticated) {
              context.go('/catalogo');
            }
          },
          builder: (context, state) {
            final isLoading = state is AuthLoading;
            final aviso = state is AuthUnauthenticated ? state.motivo : null;
            final error =
                state is AuthError && !state.duranteOtp ? state.mensaje : null;

            return SafeArea(
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 24),
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 400),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              Container(
                                width: 36,
                                height: 36,
                                decoration: BoxDecoration(
                                  color: AppColors.brand,
                                  borderRadius: BorderRadius.circular(11),
                                ),
                                alignment: Alignment.center,
                                child: const Text(
                                  'A',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 17,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 10),
                              const Text(
                                'AppHasani',
                                style: TextStyle(
                                  fontWeight: FontWeight.w700,
                                  fontSize: 18,
                                  color: AppColors.ink,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 36),
                          const Text(
                            'Iniciar sesión',
                            style: TextStyle(
                              fontSize: 30,
                              fontWeight: FontWeight.w800,
                              letterSpacing: -0.4,
                              color: AppColors.ink,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Wrap(
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: [
                              const Text(
                                '¿No tienes cuenta? ',
                                style: TextStyle(
                                  color: AppColors.ink2,
                                  fontSize: 14,
                                ),
                              ),
                              GestureDetector(
                                onTap: isLoading
                                    ? null
                                    : () {
                                        context.read<AuthCubit>().cancelarConflicto();
                                        context.go('/registro');
                                      },
                                child: const Text(
                                  'Crear cuenta',
                                  style: TextStyle(
                                    color: AppColors.brand,
                                    fontWeight: FontWeight.w700,
                                    fontSize: 14,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 28),
                          if (aviso != null)
                            _Banner(
                              texto: aviso,
                              color: AppColors.warn,
                              fondo: AppColors.warnSoft,
                            ),
                          if (error != null)
                            _Banner(
                              texto: error,
                              color: AppColors.danger,
                              fondo: AppColors.dangerSoft,
                            ),
                          TextFormField(
                            controller: _correoCtrl,
                            keyboardType: TextInputType.emailAddress,
                            enabled: !isLoading,
                            inputFormatters: AppInputFormatters.correo,
                            decoration: AppTheme.pillInputDecoration(
                              hintText: 'Correo electrónico',
                              prefixIcon: Icons.mail_outline_rounded,
                            ),
                            validator: (v) => (v == null || !v.contains('@'))
                                ? 'Correo no válido'
                                : null,
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _passwordCtrl,
                            obscureText: _ocultarPassword,
                            enabled: !isLoading,
                            inputFormatters: AppInputFormatters.password,
                            decoration: AppTheme.pillInputDecoration(
                              hintText: 'Contraseña',
                              prefixIcon: Icons.lock_outline_rounded,
                              suffixIcon: IconButton(
                                icon: Icon(
                                  _ocultarPassword
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                  size: 20,
                                  color: AppColors.ink3,
                                ),
                                onPressed: () => setState(
                                    () => _ocultarPassword = !_ocultarPassword),
                              ),
                            ),
                            validator: (v) => (v == null || v.isEmpty)
                                ? 'Campo obligatorio'
                                : null,
                          ),
                          const SizedBox(height: 28),
                          Align(
                            alignment: Alignment.centerRight,
                            child: PillActionButton(
                              label: 'Continuar',
                              icon: Icons.login_rounded,
                              loading: isLoading,
                              onPressed: () => _enviarLogin(context),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _Banner extends StatelessWidget {
  final String texto;
  final Color color;
  final Color fondo;

  const _Banner({required this.texto, required this.color, required this.fondo});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: fondo,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Text(texto, style: TextStyle(color: color, fontSize: 13.5)),
    );
  }
}

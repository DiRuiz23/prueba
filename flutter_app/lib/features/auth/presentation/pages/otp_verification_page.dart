import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/utils/input_formatters.dart';
import '../../../../core/widgets/auth_aura_background.dart';
import '../cubit/auth_cubit.dart';
import '../cubit/auth_state.dart';
import '../widgets/totp_setup_panel.dart';

class OtpVerificationPage extends StatefulWidget {
  final String correo;
  final String mensajeInicial;
  final bool requiereConfiguracionTotp;
  final String? totpSecreto;
  final String? totpOtpAuthUri;
  final String? totpQrBase64;

  const OtpVerificationPage({
    super.key,
    required this.correo,
    this.mensajeInicial = '',
    this.requiereConfiguracionTotp = false,
    this.totpSecreto,
    this.totpOtpAuthUri,
    this.totpQrBase64,
  });

  @override
  State<OtpVerificationPage> createState() => _OtpVerificationPageState();
}

class _OtpVerificationPageState extends State<OtpVerificationPage> {
  final _formKey = GlobalKey<FormState>();
  final _codigoCtrl = TextEditingController();

  @override
  void dispose() {
    _codigoCtrl.dispose();
    super.dispose();
  }

  void _validarOtp(BuildContext context) {
    if (!_formKey.currentState!.validate()) return;
    context.read<AuthCubit>().validarOtp(
          correo: widget.correo,
          codigo: _codigoCtrl.text.trim(),
        );
  }

  @override
  Widget build(BuildContext context) {
    final mostrarConfiguracion = widget.requiereConfiguracionTotp &&
        (widget.totpSecreto != null && widget.totpSecreto!.isNotEmpty);

    return Scaffold(
      body: AuthAuraBackground(
        child: BlocConsumer<AuthCubit, AuthState>(
          listener: (context, state) {
            if (state is AuthAuthenticated) context.go('/catalogo');
          },
          builder: (context, state) {
            final isLoading = state is AuthLoading;

            return SafeArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(12, 8, 16, 0),
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: IconButton(
                        icon: const Icon(
                          Icons.arrow_back_ios_new_rounded,
                          size: 20,
                          color: AppColors.ink,
                        ),
                        onPressed: () {
                          context.read<AuthCubit>().cancelarConflicto();
                          context.go('/login');
                        },
                      ),
                    ),
                  ),
                  Expanded(
                    child: Center(
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 28, vertical: 16),
                        child: ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 400),
                          child: Form(
                            key: _formKey,
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                const Text(
                                  'Google Authenticator',
                                  style: TextStyle(
                                    fontSize: 30,
                                    fontWeight: FontWeight.w800,
                                    letterSpacing: -0.4,
                                    color: AppColors.ink,
                                  ),
                                ),
                                const SizedBox(height: 6),
                                Text(
                                  mostrarConfiguracion
                                      ? 'Vincula tu cuenta ${widget.correo} con código QR o clave manual e ingresa el código de 6 dígitos.'
                                      : 'Ingresa el código de 6 dígitos de tu app para ${widget.correo}',
                                  style: const TextStyle(
                                    color: AppColors.ink2,
                                    fontSize: 14,
                                  ),
                                ),
                                const SizedBox(height: 20),
                                if (state is AuthError)
                                  Container(
                                    margin: const EdgeInsets.only(bottom: 16),
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 14, vertical: 12),
                                    decoration: BoxDecoration(
                                      color: AppColors.dangerSoft,
                                      borderRadius: BorderRadius.circular(14),
                                    ),
                                    child: Text(
                                      state.mensaje,
                                      style: const TextStyle(
                                        color: AppColors.danger,
                                        fontSize: 13.5,
                                      ),
                                    ),
                                  ),
                                if (mostrarConfiguracion) ...[
                                  TotpSetupPanel(
                                    correo: widget.correo,
                                    secreto: widget.totpSecreto!,
                                    qrBase64: widget.totpQrBase64 ?? '',
                                  ),
                                  const SizedBox(height: 18),
                                ],
                                TextFormField(
                                  controller: _codigoCtrl,
                                  enabled: !isLoading,
                                  keyboardType: TextInputType.number,
                                  textAlign: TextAlign.center,
                                  inputFormatters:
                                      AppInputFormatters.soloDigitos(6),
                                  style: const TextStyle(
                                    fontSize: 26,
                                    fontWeight: FontWeight.w700,
                                    letterSpacing: 10,
                                    fontFeatures: [FontFeature.tabularFigures()],
                                  ),
                                  decoration: AppTheme.pillInputDecoration(
                                    hintText: '000000',
                                    counterText: '',
                                  ),
                                  validator: (v) =>
                                      (v == null || v.trim().length != 6)
                                          ? 'Ingresa los 6 dígitos'
                                          : null,
                                ),
                                const SizedBox(height: 14),
                                const Text(
                                  'Los códigos se renuevan automáticamente cada 30 s.',
                                  style: TextStyle(
                                    fontSize: 12.5,
                                    color: AppColors.ink2,
                                  ),
                                ),
                                const SizedBox(height: 28),
                                Align(
                                  alignment: Alignment.centerRight,
                                  child: PillActionButton(
                                    label: 'Verificar',
                                    icon: Icons.verified_user_outlined,
                                    loading: isLoading,
                                    onPressed: () => _validarOtp(context),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}

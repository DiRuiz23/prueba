import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/theme/app_theme.dart';
import '../cubit/auth_cubit.dart';
import '../cubit/auth_state.dart';

class OtpVerificationPage extends StatefulWidget {
  final String correo;
  final int segundosIniciales;
  final String mensajeInicial;

  const OtpVerificationPage({
    super.key,
    required this.correo,
    this.segundosIniciales = 300,
    this.mensajeInicial = '',
  });

  @override
  State<OtpVerificationPage> createState() => _OtpVerificationPageState();
}

class _OtpVerificationPageState extends State<OtpVerificationPage> {
  final _formKey = GlobalKey<FormState>();
  final _codigoCtrl = TextEditingController();
  late int _segundosRestantes;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _segundosRestantes = widget.segundosIniciales;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;
      if (_segundosRestantes <= 1) {
        setState(() => _segundosRestantes = 0);
        timer.cancel();
      } else {
        setState(() => _segundosRestantes--);
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _codigoCtrl.dispose();
    super.dispose();
  }

  String _formatoTiempo(int seg) =>
      '${(seg ~/ 60).toString().padLeft(2, '0')}:${(seg % 60).toString().padLeft(2, '0')}';

  void _validarOtp(BuildContext context) {
    if (!_formKey.currentState!.validate()) return;
    context.read<AuthCubit>().validarOtp(
          correo: widget.correo,
          codigo: _codigoCtrl.text.trim(),
        );
  }

  @override
  Widget build(BuildContext context) {
    final progreso = widget.segundosIniciales > 0
        ? (_segundosRestantes / widget.segundosIniciales).clamp(0.0, 1.0)
        : 0.0;
    final bajo = _segundosRestantes <= 30;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Verificación'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            context.read<AuthCubit>().cancelarConflicto();
            context.go('/login');
          },
        ),
      ),
      body: BlocConsumer<AuthCubit, AuthState>(
        listener: (context, state) {
          if (state is AuthAuthenticated) context.go('/catalogo');
        },
        builder: (context, state) {
          final isLoading = state is AuthLoading;

          return SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 400),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(
                          widget.correo,
                          style: const TextStyle(color: AppColors.ink2, fontSize: 14),
                        ),
                        const SizedBox(height: 20),
                        if (state is AuthError)
                          Container(
                            margin: const EdgeInsets.only(bottom: 14),
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: AppColors.dangerSoft,
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              state.mensaje,
                              style: const TextStyle(color: AppColors.danger, fontSize: 13.5),
                            ),
                          ),
                        TextFormField(
                          controller: _codigoCtrl,
                          enabled: !isLoading && _segundosRestantes > 0,
                          keyboardType: TextInputType.number,
                          textAlign: TextAlign.center,
                          maxLength: 6,
                          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                          style: const TextStyle(
                            fontSize: 26,
                            fontWeight: FontWeight.w600,
                            letterSpacing: 10,
                          ),
                          decoration: const InputDecoration(
                            labelText: 'Código de 6 dígitos',
                            counterText: '',
                          ),
                          validator: (v) => (v == null || v.trim().length != 6)
                              ? 'Ingresa los 6 dígitos'
                              : null,
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: LinearProgressIndicator(
                                value: progreso,
                                minHeight: 4,
                                borderRadius: BorderRadius.circular(99),
                                color: bajo ? AppColors.danger : AppColors.brand,
                                backgroundColor: AppColors.line,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Text(
                              _formatoTiempo(_segundosRestantes),
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: bajo ? AppColors.danger : AppColors.ink2,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 22),
                        FilledButton(
                          onPressed: (isLoading || _segundosRestantes <= 0)
                              ? null
                              : () => _validarOtp(context),
                          child: isLoading
                              ? const SizedBox(
                                  height: 20,
                                  width: 20,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.white,
                                  ),
                                )
                              : const Text('Verificar'),
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
    );
  }
}

import 'package:equatable/equatable.dart';
import '../../../compra/domain/entities/transaccion_entity.dart';

abstract class HistorialState extends Equatable {
  const HistorialState();

  @override
  List<Object?> get props => [];
}

class HistorialInitial extends HistorialState {
  const HistorialInitial();
}

class HistorialLoading extends HistorialState {
  const HistorialLoading();
}

class HistorialLoaded extends HistorialState {
  final List<TransaccionEntity> items;
  final int paginaActual;
  final bool esUltima;
  final bool cargandoMas;

  const HistorialLoaded({
    required this.items,
    required this.paginaActual,
    required this.esUltima,
    this.cargandoMas = false,
  });

  @override
  List<Object?> get props => [items, paginaActual, esUltima, cargandoMas];
}

class HistorialError extends HistorialState {
  final String mensaje;

  const HistorialError(this.mensaje);

  @override
  List<Object?> get props => [mensaje];
}


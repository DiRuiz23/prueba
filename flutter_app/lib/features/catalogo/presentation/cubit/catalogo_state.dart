import 'package:equatable/equatable.dart';
import '../../domain/entities/producto_entity.dart';

enum CategoriaFiltro { todos, recargas, servicios }

abstract class CatalogoState extends Equatable {
  const CatalogoState();

  @override
  List<Object?> get props => [];
}

class CatalogoInitial extends CatalogoState {
  const CatalogoInitial();
}

class CatalogoLoading extends CatalogoState {
  const CatalogoLoading();
}

class CatalogoLoaded extends CatalogoState {
  final List<ProductoEntity> todos;
  final List<ProductoEntity> filtrados;
  final String busqueda;
  final CategoriaFiltro categoria;

  const CatalogoLoaded({
    required this.todos,
    required this.filtrados,
    this.busqueda = '',
    this.categoria = CategoriaFiltro.todos,
  });

  @override
  List<Object?> get props => [todos, filtrados, busqueda, categoria];
}

class CatalogoError extends CatalogoState {
  final String mensaje;

  const CatalogoError(this.mensaje);

  @override
  List<Object?> get props => [mensaje];
}


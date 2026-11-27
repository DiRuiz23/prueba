import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { ClienteResponse, GenericResponse, RegistroClienteRequest, UbicacionCatalogoResponse } from '../../core/models/api.models';

type CatalogoPostalItem = UbicacionCatalogoResponse;

const CATALOGO_SEPOMEX: CatalogoPostalItem[] = [
  { estado: 'Aguascalientes', codigoPostal: '20000', municipio: 'Aguascalientes' },
  { estado: 'Baja California', codigoPostal: '22000', municipio: 'Tijuana' },
  { estado: 'Baja California', codigoPostal: '21100', municipio: 'Mexicali' },
  { estado: 'Baja California Sur', codigoPostal: '23000', municipio: 'La Paz' },
  { estado: 'Campeche', codigoPostal: '24000', municipio: 'Campeche' },
  { estado: 'Chiapas', codigoPostal: '29000', municipio: 'Tuxtla Gutiérrez' },
  { estado: 'Chihuahua', codigoPostal: '31000', municipio: 'Chihuahua' },
  { estado: 'Chihuahua', codigoPostal: '32000', municipio: 'Juárez' },
  { estado: 'Ciudad de México', codigoPostal: '06000', municipio: 'Cuauhtémoc' },
  { estado: 'Ciudad de México', codigoPostal: '06600', municipio: 'Cuauhtémoc' },
  { estado: 'Ciudad de México', codigoPostal: '06700', municipio: 'Cuauhtémoc' },
  { estado: 'Ciudad de México', codigoPostal: '03100', municipio: 'Benito Juárez' },
  { estado: 'Ciudad de México', codigoPostal: '11560', municipio: 'Miguel Hidalgo' },
  { estado: 'Ciudad de México', codigoPostal: '04510', municipio: 'Coyoacán' },
  { estado: 'Ciudad de México', codigoPostal: '01000', municipio: 'Álvaro Obregón' },
  { estado: 'Ciudad de México', codigoPostal: '14000', municipio: 'Tlalpan' },
  { estado: 'Coahuila', codigoPostal: '25000', municipio: 'Saltillo' },
  { estado: 'Coahuila', codigoPostal: '27000', municipio: 'Torreón' },
  { estado: 'Colima', codigoPostal: '28000', municipio: 'Colima' },
  { estado: 'Durango', codigoPostal: '34000', municipio: 'Durango' },
  { estado: 'Estado de México', codigoPostal: '50000', municipio: 'Toluca' },
  { estado: 'Estado de México', codigoPostal: '53100', municipio: 'Naucalpan de Juárez' },
  { estado: 'Guanajuato', codigoPostal: '36000', municipio: 'Guanajuato' },
  { estado: 'Guanajuato', codigoPostal: '37000', municipio: 'León' },
  { estado: 'Guerrero', codigoPostal: '39000', municipio: 'Chilpancingo de los Bravo' },
  { estado: 'Guerrero', codigoPostal: '39300', municipio: 'Acapulco de Juárez' },
  { estado: 'Hidalgo', codigoPostal: '42000', municipio: 'Pachuca de Soto' },
  { estado: 'Jalisco', codigoPostal: '44100', municipio: 'Guadalajara' },
  { estado: 'Jalisco', codigoPostal: '45010', municipio: 'Zapopan' },
  { estado: 'Jalisco', codigoPostal: '45500', municipio: 'San Pedro Tlaquepaque' },
  { estado: 'Michoacán', codigoPostal: '58000', municipio: 'Morelia' },
  { estado: 'Morelos', codigoPostal: '62000', municipio: 'Cuernavaca' },
  { estado: 'Nayarit', codigoPostal: '63000', municipio: 'Tepic' },
  { estado: 'Nuevo León', codigoPostal: '64000', municipio: 'Monterrey' },
  { estado: 'Nuevo León', codigoPostal: '66220', municipio: 'San Pedro Garza García' },
  { estado: 'Nuevo León', codigoPostal: '66450', municipio: 'San Nicolás de los Garza' },
  { estado: 'Oaxaca', codigoPostal: '68000', municipio: 'Oaxaca de Juárez' },
  { estado: 'Puebla', codigoPostal: '72000', municipio: 'Puebla' },
  { estado: 'Querétaro', codigoPostal: '76000', municipio: 'Querétaro' },
  { estado: 'Quintana Roo', codigoPostal: '77000', municipio: 'Othón P. Blanco' },
  { estado: 'Quintana Roo', codigoPostal: '77500', municipio: 'Benito Juárez' },
  { estado: 'San Luis Potosí', codigoPostal: '78000', municipio: 'San Luis Potosí' },
  { estado: 'Sinaloa', codigoPostal: '80000', municipio: 'Culiacán' },
  { estado: 'Sonora', codigoPostal: '83000', municipio: 'Hermosillo' },
  { estado: 'Tabasco', codigoPostal: '86000', municipio: 'Centro' },
  { estado: 'Tamaulipas', codigoPostal: '87000', municipio: 'Victoria' },
  { estado: 'Tlaxcala', codigoPostal: '90000', municipio: 'Tlaxcala' },
  { estado: 'Veracruz', codigoPostal: '91000', municipio: 'Xalapa' },
  { estado: 'Veracruz', codigoPostal: '91700', municipio: 'Veracruz' },
  { estado: 'Yucatán', codigoPostal: '97000', municipio: 'Mérida' },
  { estado: 'Zacatecas', codigoPostal: '98000', municipio: 'Zacatecas' }
];

const OCUPACIONES_FALLBACK: string[] = [
  'Empleado(a) sector privado',
  'Empleado(a) sector público',
  'Estudiante',
  'Profesionista independiente',
  'Comerciante',
  'Empresario(a)',
  'Docente / Profesor(a)',
  'Ingeniero(a) / Desarrollador(a)',
  'Personal de salud',
  'Técnico(a) especializado(a)',
  'Trabajador(a) por cuenta propia',
  'Jubilado(a) / Pensionado(a)',
  'Hogar'
];

const COLONIAS_FALLBACK: Record<string, string[]> = {
  '62000': ['Cuernavaca Centro', 'Las Palmas', 'Carolina', 'Gualupita', 'San Antón', 'Vista Hermosa', 'Lomas de la Selva', 'Delicias', 'Acapantzingo', 'Tlaltenango'],
  '62550': ['Jiutepec Centro', 'Civac', 'Tejalpa', 'Tlahuapan', 'Progreso', 'La Joya', 'Pedregal de las Fuentes'],
  '62580': ['Temixco Centro', 'Burgos', 'Lomas del Carril', 'Rubén Jaramillo', 'Acatlipa', 'Pueblo Viejo'],
  '62740': ['Cuautla Centro', 'Emiliano Zapata', 'Gabriel Tepepa', 'Otilio Montaño', 'Año de Juárez'],
  '62760': ['Emiliano Zapata Centro', 'Tres de Mayo', 'Tezoyuca', 'Benito Juárez', 'Palo Escrito'],
  '62730': ['Yautepec Centro', 'Oaxtepec', 'Cocoyoc', 'Rancho Nuevo'],
  '62790': ['Xochitepec Centro', 'Alpuyeca', 'Chiconcuac', 'Real del Puente'],
  '62900': ['Jojutla Centro', 'Panchimalco', 'Tehuixtla'],
  '62780': ['Zacatepec Centro', 'Galeana', 'Lázaro Cárdenas'],
  '62520': ['Tepoztlán Centro', 'San Miguel', 'Santo Domingo', 'Amatlán'],
  '06000': ['Centro Histórico', 'Guerrero', 'Tabacalera'],
  '06600': ['Juárez', 'Zona Rosa'],
  '06700': ['Roma Norte', 'Roma Sur', 'Condesa'],
  '03100': ['Del Valle Centro', 'Del Valle Norte', 'Narvarte'],
  '11560': ['Polanco IV Sección', 'Polanco V Sección', 'Anzures', 'Granada'],
  '04510': ['Ciudad Universitaria', 'Pedregal de Santo Domingo', 'Copilco'],
  '01000': ['San Ángel', 'Chimalistac', 'Guadalupe Inn'],
  '14000': ['Tlalpan Centro', 'Toriello Guerra', 'Isidro Fabela'],
  '44100': ['Guadalajara Centro', 'Americana', 'Lafayette'],
  '64000': ['Monterrey Centro', 'Obispado', 'Barrio Antiguo'],
  '72000': ['Puebla Centro', 'Analco', 'La Paz'],
  '76000': ['Querétaro Centro', 'Álamos', 'Jurica']
};

const PATRON_NOMBRE = /^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]{2,50}$/;
const PATRON_OCUPACION = /^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]{2,100}$/;
const PATRON_EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PATRON_CURP = /^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[A-Z0-9]{2}$/i;
const PATRON_RFC = /^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$/i;
const PATRON_TELEFONO = /^[0-9]{10}$/;
const PATRON_CP = /^[0-9]{5}$/;
const PATRON_NUM_DOMICILIO = /^[A-Za-z0-9/\-]{1,6}$/;
const PATRON_PASSWORD = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,}$/;

const RANGOS_INGRESO_MENSUAL: ReadonlyArray<{ etiqueta: string; valor: number }> = [
  { etiqueta: 'Menos de $5,000 MXN', valor: 5000 },
  { etiqueta: '$5,001 a $10,000 MXN', valor: 10000 },
  { etiqueta: '$10,001 a $20,000 MXN', valor: 20000 },
  { etiqueta: '$20,001 a $35,000 MXN', valor: 35000 },
  { etiqueta: '$35,001 a $50,000 MXN', valor: 50000 },
  { etiqueta: '$50,001 a $100,000 MXN', valor: 100000 },
  { etiqueta: 'Más de $100,000 MXN', valor: 150000 }
];

function mayorDeEdadValidator(control: AbstractControl): ValidationErrors | null {
  const val = String(control.value ?? '').trim();
  if (!val) return null;
  const nacimiento = new Date(`${val}T00:00:00`);
  if (Number.isNaN(nacimiento.getTime())) return { fechaInvalida: true };
  const hoy = new Date();
  const limite = new Date(hoy.getFullYear() - 18, hoy.getMonth(), hoy.getDate());
  return nacimiento <= limite ? null : { menorEdad: true };
}

function coincidirPasswordValidator(group: AbstractControl): ValidationErrors | null {
  const pass = group.get('password')?.value ?? '';
  const confirm = group.get('confirmarPassword')?.value ?? '';
  return pass && confirm && pass !== confirm ? { noCoinciden: true } : null;
}

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-wrap">
      <div class="auth-card box">
        <div class="top-row">
          @if (!cuentaCreada()) {
            <button type="button" class="icon-btn back-btn" aria-label="Volver" (click)="volverPasoOAtras()">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
            </button>
          }
          <div class="logo"><span class="mark">A</span><strong>AppHasani</strong></div>
        </div>

        @if (cuentaCreada(); as cli) {
          <span class="badge badge-success">Cuenta creada</span>
          <h1 style="margin-top: 0.65rem;">Registro completado</h1>
          <dl class="dl" style="margin-top: 1rem;">
            <div><dt>Titular</dt><dd>{{ cli.nombre }} {{ cli.apellidoPaterno }} {{ cli.apellidoMaterno }}</dd></div>
            <div><dt>Correo</dt><dd>{{ cli.correo }}</dd></div>
            <div><dt>Cuenta</dt><dd class="num">{{ cli.numeroCuentas[0] || '—' }}</dd></div>
            <div><dt>Saldo inicial</dt><dd class="num amount-ok">{{ 1000 | currency: 'MXN' : 'symbol-narrow' : '1.2-2' }}</dd></div>
          </dl>
          <div class="cta-end">
            <button type="button" class="btn btn-primary btn-pill-lg" (click)="irALogin()">
              <span>Iniciar sesión</span>
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
            </button>
          </div>
        } @else {
          <h1>Crear cuenta</h1>
          <p class="muted small sub-lead">
            ¿Ya tienes cuenta? <a routerLink="/login" class="link-brand">Iniciar sesión</a>
          </p>

          <div class="steps" role="tablist" aria-label="Pasos de registro">
            <div class="step" [class.active]="pasoActual() === 1" [class.done]="pasoActual() > 1">1. Personal</div>
            <div class="step" [class.active]="pasoActual() === 2" [class.done]="pasoActual() > 2">2. Contacto</div>
            <div class="step" [class.active]="pasoActual() === 3" [class.done]="pasoActual() > 3">3. Domicilio</div>
            <div class="step" [class.active]="pasoActual() === 4">4. Seguridad</div>
          </div>

          @if (errorMsg()) {
            <div class="alert alert-danger" role="alert">{{ errorMsg() }}</div>
          }

          @switch (pasoActual()) {
            @case (1) {
              <form [formGroup]="personalForm" (ngSubmit)="avanzarDesdePaso1()">
                <div class="grid-2">
                  <div class="field">
                    <label for="nombre">Nombre</label>
                    <input id="nombre" type="text" maxlength="50" class="input" formControlName="nombre"
                           (input)="sanitizarCampo(personalForm, 'nombre', 'LETRAS', 50)"
                           [class.invalid]="tocadoInvalido(personalForm, 'nombre')" />
                    @if (tocadoInvalido(personalForm, 'nombre')) {
                      <span class="field-error">Solo letras (2 a 50)</span>
                    }
                  </div>
                  <div class="field">
                    <label for="segundoNombre">Segundo nombre <span class="faint">(opcional)</span></label>
                    <input id="segundoNombre" type="text" maxlength="50" class="input" formControlName="segundoNombre"
                           (input)="sanitizarCampo(personalForm, 'segundoNombre', 'LETRAS', 50)"
                           [class.invalid]="tocadoInvalido(personalForm, 'segundoNombre')" />
                    @if (tocadoInvalido(personalForm, 'segundoNombre')) {
                      <span class="field-error">Solo letras (2 a 50)</span>
                    }
                  </div>
                </div>

                <div class="grid-2">
                  <div class="field">
                    <label for="apellidoPaterno">Apellido paterno</label>
                    <input id="apellidoPaterno" type="text" maxlength="50" class="input" formControlName="apellidoPaterno"
                           (input)="sanitizarCampo(personalForm, 'apellidoPaterno', 'LETRAS', 50)"
                           [class.invalid]="tocadoInvalido(personalForm, 'apellidoPaterno')" />
                    @if (tocadoInvalido(personalForm, 'apellidoPaterno')) {
                      <span class="field-error">Solo letras (2 a 50)</span>
                    }
                  </div>
                  <div class="field">
                    <label for="apellidoMaterno">Apellido materno</label>
                    <input id="apellidoMaterno" type="text" maxlength="50" class="input" formControlName="apellidoMaterno"
                           (input)="sanitizarCampo(personalForm, 'apellidoMaterno', 'LETRAS', 50)"
                           [class.invalid]="tocadoInvalido(personalForm, 'apellidoMaterno')" />
                    @if (tocadoInvalido(personalForm, 'apellidoMaterno')) {
                      <span class="field-error">Solo letras (2 a 50)</span>
                    }
                  </div>
                </div>

                <div class="grid-3">
                  <div class="field">
                    <label for="fechaNacimiento">Nacimiento</label>
                    <input id="fechaNacimiento" type="date" class="input num" [max]="maxFechaNacimiento"
                           formControlName="fechaNacimiento"
                           [class.invalid]="tocadoInvalido(personalForm, 'fechaNacimiento')" />
                    @if (personalForm.controls.fechaNacimiento.touched && personalForm.controls.fechaNacimiento.hasError('required')) {
                      <span class="field-error">Selecciona tu fecha de nacimiento</span>
                    } @else if (personalForm.controls.fechaNacimiento.touched && personalForm.controls.fechaNacimiento.hasError('menorEdad')) {
                      <span class="field-error">Mínimo 18 años</span>
                    }
                  </div>
                  <div class="field">
                    <label for="sexo">Sexo</label>
                    <select id="sexo" class="input" formControlName="sexo">
                      <option value="H">H</option>
                      <option value="M">M</option>
                      <option value="Indefinido">Indefinido</option>
                    </select>
                  </div>
                  <div class="field">
                    <label for="estadoCivil">Estado civil</label>
                    <select id="estadoCivil" class="input" formControlName="estadoCivil">
                      <option value="SOLTERO">Soltero/a</option>
                      <option value="CASADO">Casado/a</option>
                      <option value="DIVORCIADO">Divorciado/a</option>
                      <option value="VIUDO">Viudo/a</option>
                      <option value="UNION_LIBRE">Unión libre</option>
                    </select>
                  </div>
                </div>

                <div class="grid-2">
                  <div class="field">
                    <label for="curp">CURP</label>
                    <input id="curp" type="text" maxlength="18" class="input num upper" formControlName="curp"
                           (input)="alEditarCurp()"
                           [class.invalid]="tocadoInvalido(personalForm, 'curp') || !!errorCurpDuplicada()" />
                    @if (tocadoInvalido(personalForm, 'curp')) {
                      <span class="field-error">Formato: 4 letras + 6 números + H/M + 5 letras + 2 homoclave</span>
                    } @else if (errorCurpDuplicada()) {
                      <span class="field-error">{{ errorCurpDuplicada() }}</span>
                    }
                  </div>
                  <div class="field">
                    <label for="rfc">RFC</label>
                    <input id="rfc" type="text" maxlength="13" class="input num upper" formControlName="rfc"
                           (input)="alEditarRfc()"
                           [class.invalid]="tocadoInvalido(personalForm, 'rfc') || !!errorRfcDuplicado()" />
                    @if (tocadoInvalido(personalForm, 'rfc')) {
                      <span class="field-error">Formato: 4 letras + 6 números (AAMMDD) + 3 homoclave</span>
                    } @else if (errorRfcDuplicado()) {
                      <span class="field-error">{{ errorRfcDuplicado() }}</span>
                    }
                  </div>
                </div>

                <button type="submit" class="btn btn-primary btn-block" [disabled]="validandoPaso()">
                  @if (validandoPaso()) { <span class="spinner"></span> } @else { Continuar }
                </button>
              </form>
            }

            @case (2) {
              <form [formGroup]="contactoForm" (ngSubmit)="avanzarDesdePaso2()">
                <div class="field">
                  <label for="correo">Correo electrónico
                    @if (correoVerificado()) {
                      <span class="badge badge-success" style="margin-left: 0.4rem;">Verificado</span>
                    }
                  </label>
                  <input id="correo" type="email" maxlength="100" class="input" formControlName="correo" autocomplete="email"
                         (input)="alEditarCorreo()"
                         [class.invalid]="tocadoInvalido(contactoForm, 'correo') || !!errorCorreoDuplicado()" />
                  @if (tocadoInvalido(contactoForm, 'correo')) {
                    <span class="field-error">Ingresa un correo electrónico válido</span>
                  } @else if (errorCorreoDuplicado()) {
                    <span class="field-error">{{ errorCorreoDuplicado() }}</span>
                  } @else {
                    <span class="faint small">Aceptamos cualquier proveedor (Gmail, Outlook, Hotmail, Yahoo, institucional, etc.)</span>
                  }
                </div>

                @if (codigoEnviado() && !correoVerificado()) {
                  <div class="verify-box">
                    @if (avisoCodigoCorreo()) {
                      <p class="small" style="color: var(--brand); font-weight: 600; margin-bottom: 0.65rem;">{{ avisoCodigoCorreo() }}</p>
                    }
                    <div class="verify-row">
                      <input type="text" inputmode="numeric" maxlength="6" class="pill-input num"
                             placeholder="Código de 6 dígitos" [value]="codigoCorreo()" (input)="alEditarCodigo($event)" />
                      <button type="button" class="btn btn-primary" [disabled]="confirmandoCodigo()" (click)="confirmarCodigo()">
                        @if (confirmandoCodigo()) { <span class="spinner"></span> } @else { Validar }
                      </button>
                    </div>
                    @if (errorCodigoCorreo()) {
                      <span class="field-error" style="display:block; margin-top: 0.35rem;">{{ errorCodigoCorreo() }}</span>
                    }
                    <div style="text-align: right; margin-top: 0.4rem;">
                      <button type="button" class="link-btn small" [disabled]="confirmandoCodigo()" (click)="reenviarCodigoCorreo()">
                        Reenviar código
                      </button>
                    </div>
                  </div>
                }

                <div class="grid-2">
                  <div class="field">
                    <label for="telefonoMovil">Teléfono móvil</label>
                    <input id="telefonoMovil" type="text" maxlength="10" inputmode="numeric" class="input num"
                           formControlName="telefonoMovil"
                           (input)="alEditarTelefonoMovil()"
                           [class.invalid]="tocadoInvalido(contactoForm, 'telefonoMovil') || !!errorTelefonoDuplicado()" />
                    @if (tocadoInvalido(contactoForm, 'telefonoMovil')) {
                      <span class="field-error">10 dígitos numéricos</span>
                    } @else if (errorTelefonoDuplicado()) {
                      <span class="field-error">{{ errorTelefonoDuplicado() }}</span>
                    }
                  </div>
                  <div class="field">
                    <label for="telefonoAlternativo">Tel. alternativo <span class="faint">(opcional)</span></label>
                    <input id="telefonoAlternativo" type="text" maxlength="10" inputmode="numeric" class="input num"
                           formControlName="telefonoAlternativo"
                           (input)="sanitizarCampo(contactoForm, 'telefonoAlternativo', 'DIGITOS', 10)"
                           [class.invalid]="tocadoInvalido(contactoForm, 'telefonoAlternativo')" />
                    @if (tocadoInvalido(contactoForm, 'telefonoAlternativo')) {
                      <span class="field-error">10 dígitos numéricos</span>
                    }
                  </div>
                </div>

                <div class="grid-2">
                  <div class="field">
                    <label for="ocupacionCatalogo">Ocupación</label>
                    <select id="ocupacionCatalogo" class="input" formControlName="ocupacionCatalogo"
                            (change)="alCambiarOcupacion()"
                            [class.invalid]="tocadoInvalido(contactoForm, 'ocupacionCatalogo')">
                      <option value="">Selecciona una ocupación</option>
                      @for (oc of ocupacionesDisponibles(); track oc) {
                        <option [value]="oc">{{ oc }}</option>
                      }
                      <option value="Otro">Otro (especificar)</option>
                    </select>
                    @if (tocadoInvalido(contactoForm, 'ocupacionCatalogo')) {
                      <span class="field-error">Selecciona una ocupación</span>
                    }
                  </div>
                  <div class="field">
                    <label for="empresa">Empresa @if (esEstudiante()) { <span class="faint">(no aplica)</span> }</label>
                    <input id="empresa" type="text" maxlength="100" class="input" formControlName="empresa"
                           [placeholder]="esEstudiante() ? 'No aplica para estudiantes' : 'Nombre de empresa o negocio'"
                           (input)="sanitizarCampo(contactoForm, 'empresa', 'TEXTO', 100)"
                           [class.invalid]="!esEstudiante() && tocadoInvalido(contactoForm, 'empresa')" />
                    @if (!esEstudiante() && tocadoInvalido(contactoForm, 'empresa')) {
                      <span class="field-error">La empresa es obligatoria</span>
                    }
                  </div>
                </div>

                @if (contactoForm.controls.ocupacionCatalogo.value === 'Otro') {
                  <div class="field">
                    <label for="ocupacionOtra">Especifica tu ocupación</label>
                    <input id="ocupacionOtra" type="text" maxlength="100" class="input" formControlName="ocupacionOtra"
                           placeholder="Ej. Diseñador gráfico"
                           (input)="sanitizarCampo(contactoForm, 'ocupacionOtra', 'LETRAS', 100)"
                           [class.invalid]="tocadoInvalido(contactoForm, 'ocupacionOtra')" />
                    @if (tocadoInvalido(contactoForm, 'ocupacionOtra')) {
                      <span class="field-error">Especifica tu ocupación (2 a 100 letras)</span>
                    }
                  </div>
                }

                <div class="field">
                  <label for="ingresoMensual">Ingreso mensual (MXN)</label>
                  <select id="ingresoMensual" class="input" formControlName="ingresoMensual"
                          [class.invalid]="tocadoInvalido(contactoForm, 'ingresoMensual')">
                    <option value="">Selecciona un rango de ingresos</option>
                    @for (rango of rangosIngreso; track rango.valor) {
                      <option [value]="rango.valor">{{ rango.etiqueta }}</option>
                    }
                  </select>
                  @if (tocadoInvalido(contactoForm, 'ingresoMensual')) {
                    <span class="field-error">Selecciona un rango de ingreso mensual</span>
                  }
                </div>

                <div class="nav-actions">
                  <button type="button" class="btn btn-secondary" [disabled]="validandoPaso()" (click)="pasoActual.set(1)">Atrás</button>
                  <button type="submit" class="btn btn-primary grow-btn" [disabled]="validandoPaso()">
                    @if (validandoPaso()) { <span class="spinner"></span> } @else { Continuar }
                  </button>
                </div>
              </form>
            }

            @case (3) {
              <form [formGroup]="domicilioForm" (ngSubmit)="avanzarDesdePaso3()">
                <div class="field">
                  <label for="codigoPostal">Código postal <span class="faint">(escribe 5 dígitos para autocompletar)</span></label>
                  <input id="codigoPostal" type="text" maxlength="5" inputmode="numeric" class="input num"
                         formControlName="codigoPostal" placeholder="Ej. 62000"
                         (input)="onCodigoPostalInput()"
                         [class.invalid]="tocadoInvalido(domicilioForm, 'codigoPostal')" />
                  @if (tocadoInvalido(domicilioForm, 'codigoPostal')) {
                    <span class="field-error">Código postal de 5 dígitos</span>
                  } @else if (avisoCp()) {
                    <span class="faint small">{{ avisoCp() }}</span>
                  }
                </div>

                <div class="grid-2">
                  <div class="field">
                    <label for="estado">Estado</label>
                    <select id="estado" class="input" formControlName="estado" (change)="alCambiarEstado()"
                            [class.invalid]="tocadoInvalido(domicilioForm, 'estado')">
                      @for (est of estadosDisponibles; track est) {
                        <option [value]="est">{{ est }}</option>
                      }
                    </select>
                    @if (tocadoInvalido(domicilioForm, 'estado')) {
                      <span class="field-error">Selecciona un estado</span>
                    }
                  </div>
                  <div class="field">
                    <label for="ubicacion">Municipio · CP</label>
                    <select id="ubicacion" class="input num" formControlName="ubicacionClave" (change)="alCambiarUbicacion()"
                            [class.invalid]="tocadoInvalido(domicilioForm, 'ubicacionClave')">
                      @for (u of ubicacionesDelEstado(); track u.codigoPostal + '-' + u.municipio) {
                        <option [value]="claveUbicacion(u)">{{ u.municipio }} ({{ u.codigoPostal }})</option>
                      }
                    </select>
                    @if (tocadoInvalido(domicilioForm, 'ubicacionClave')) {
                      <span class="field-error">Selecciona un municipio</span>
                    }
                  </div>
                </div>

                <div class="field">
                  <label for="coloniaCatalogo">Colonia</label>
                  <select id="coloniaCatalogo" class="input" formControlName="coloniaCatalogo"
                          (change)="alCambiarColonia()"
                          [class.invalid]="tocadoInvalido(domicilioForm, 'coloniaCatalogo')">
                    <option value="">Selecciona una colonia</option>
                    @for (col of coloniasDelCp(); track col) {
                      <option [value]="col">{{ col }}</option>
                    }
                    <option value="Otra">Otra (escribir colonia)</option>
                  </select>
                  @if (tocadoInvalido(domicilioForm, 'coloniaCatalogo')) {
                    <span class="field-error">Selecciona una colonia</span>
                  }
                </div>

                @if (domicilioForm.controls.coloniaCatalogo.value === 'Otra') {
                  <div class="field">
                    <label for="coloniaOtra">Nombre de tu colonia</label>
                    <input id="coloniaOtra" type="text" maxlength="100" class="input" formControlName="coloniaOtra"
                           placeholder="Escribe el nombre de tu colonia"
                           (input)="sanitizarCampo(domicilioForm, 'coloniaOtra', 'TEXTO', 100)"
                           [class.invalid]="tocadoInvalido(domicilioForm, 'coloniaOtra')" />
                    @if (tocadoInvalido(domicilioForm, 'coloniaOtra')) {
                      <span class="field-error">Escribe el nombre de tu colonia</span>
                    }
                  </div>
                }

                <div class="field">
                  <label for="calle">Calle</label>
                  <input id="calle" type="text" maxlength="100" class="input" formControlName="calle"
                         (input)="sanitizarCampo(domicilioForm, 'calle', 'TEXTO', 100)"
                         [class.invalid]="tocadoInvalido(domicilioForm, 'calle')" />
                  @if (tocadoInvalido(domicilioForm, 'calle')) {
                    <span class="field-error">Ingresa el nombre de la calle</span>
                  }
                </div>

                <div class="grid-2">
                  <div class="field">
                    <label for="numeroExterior">Número exterior <span class="faint">(máx. 6)</span></label>
                    <input id="numeroExterior" type="text" maxlength="6" class="input num" formControlName="numeroExterior"
                           placeholder="Ej. 123 o S/N"
                           (input)="sanitizarCampo(domicilioForm, 'numeroExterior', 'NUM_DOMICILIO', 6)"
                           [class.invalid]="tocadoInvalido(domicilioForm, 'numeroExterior')" />
                    @if (tocadoInvalido(domicilioForm, 'numeroExterior')) {
                      <span class="field-error">Máx. 6 caracteres (letras, números, - o /)</span>
                    }
                  </div>
                  <div class="field">
                    <label for="numeroInterior">Interior <span class="faint">(opc., máx. 6)</span></label>
                    <input id="numeroInterior" type="text" maxlength="6" class="input num" formControlName="numeroInterior"
                           placeholder="Ej. 4B"
                           (input)="sanitizarCampo(domicilioForm, 'numeroInterior', 'NUM_DOMICILIO', 6)"
                           [class.invalid]="tocadoInvalido(domicilioForm, 'numeroInterior')" />
                    @if (tocadoInvalido(domicilioForm, 'numeroInterior')) {
                      <span class="field-error">Máx. 6 caracteres (letras, números, - o /)</span>
                    }
                  </div>
                </div>

                <div class="nav-actions">
                  <button type="button" class="btn btn-secondary" (click)="pasoActual.set(2)">Atrás</button>
                  <button type="submit" class="btn btn-primary grow-btn">Continuar</button>
                </div>
              </form>
            }

            @case (4) {
              <form [formGroup]="seguridadForm" (ngSubmit)="enviarRegistro()">
                <div class="field">
                  <label for="password">Contraseña</label>
                  <div class="pw">
                    <input id="password" maxlength="64" [type]="mostrarPassword() ? 'text' : 'password'" class="input"
                           formControlName="password" autocomplete="new-password"
                           (input)="sanitizarCampo(seguridadForm, 'password', 'SIN_ESPACIOS', 64)"
                           [class.invalid]="tocadoInvalido(seguridadForm, 'password')" />
                    <button type="button" class="toggle" (click)="mostrarPassword.set(!mostrarPassword())">
                      {{ mostrarPassword() ? 'Ocultar' : 'Mostrar' }}
                    </button>
                  </div>
                  @if (tocadoInvalido(seguridadForm, 'password')) {
                    <span class="field-error">La contraseña no cumple los requisitos mínimos</span>
                  }
                </div>

                <div class="field">
                  <label for="confirmarPassword">Confirmar contraseña</label>
                  <div class="pw">
                    <input id="confirmarPassword" maxlength="64" [type]="mostrarConfirmacion() ? 'text' : 'password'" class="input"
                           formControlName="confirmarPassword" autocomplete="new-password"
                           (input)="sanitizarCampo(seguridadForm, 'confirmarPassword', 'SIN_ESPACIOS', 64)"
                           [class.invalid]="seguridadForm.controls.confirmarPassword.touched && seguridadForm.hasError('noCoinciden')" />
                    <button type="button" class="toggle" (click)="mostrarConfirmacion.set(!mostrarConfirmacion())">
                      {{ mostrarConfirmacion() ? 'Ocultar' : 'Mostrar' }}
                    </button>
                  </div>
                  @if (seguridadForm.controls.confirmarPassword.touched && seguridadForm.hasError('noCoinciden')) {
                    <span class="field-error">Las contraseñas no coinciden</span>
                  }
                </div>

                <ul class="rules" aria-label="Requisitos de contraseña">
                  <li [class.ok]="passValue().length >= 8">Mínimo 8 caracteres</li>
                  <li [class.ok]="tieneMayusYMinus()">Mayúscula y minúscula</li>
                  <li [class.ok]="tieneNumero()">Al menos un número</li>
                  <li [class.ok]="tieneSimbolo()">Un símbolo (&#64;#$%^&amp;+=!._-)</li>
                </ul>

                <div class="nav-actions">
                  <button type="button" class="btn btn-secondary" [disabled]="cargando()" (click)="pasoActual.set(3)">Atrás</button>
                  <button type="submit" class="btn btn-primary grow-btn" [disabled]="cargando()">
                    @if (cargando()) { <span class="spinner"></span> } @else { Crear cuenta }
                  </button>
                </div>
              </form>
            }
          }
        }
      </div>
    </div>
  `,
  styles: [`
    .box { max-width: 520px; }
    .top-row { display: flex; align-items: center; gap: 0.6rem; margin-bottom: 1rem; }
    .back-btn { margin-left: -0.5rem; }
    .logo { display: flex; align-items: center; gap: 0.6rem; }
    .mark { width: 32px; height: 32px; border-radius: 10px; background: var(--brand); color: #fff; font-weight: 800; display: inline-flex; align-items: center; justify-content: center; }
    .link-brand { color: var(--brand); font-weight: 700; text-decoration: none; }
    .link-brand:hover { text-decoration: underline; }
    h1 { font-size: 1.75rem; font-weight: 800; margin-bottom: 0.35rem; }
    .sub-lead { margin-bottom: 1.15rem; }
    .btn-pill-lg { min-height: 48px; padding: 0 1.65rem; }
    .verify-box { background: var(--brand-soft); border-radius: 14px; padding: 0.9rem 1rem; margin-bottom: 1rem; }
    .verify-row { display: flex; gap: 0.5rem; align-items: center; }
    .verify-row .pill-input { min-height: 42px; text-align: center; letter-spacing: 0.25rem; font-weight: 700; }
    .link-btn { background: none; border: none; color: var(--brand); font-weight: 600; cursor: pointer; padding: 0; }
    .link-btn:hover { text-decoration: underline; }
    .steps { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.4rem; margin-bottom: 1.25rem; }
    .step { font-size: 0.76rem; font-weight: 600; color: var(--ink-3); text-align: center; padding: 0.4rem 0.25rem; border-bottom: 2px solid var(--line); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .step.active { color: var(--brand); border-bottom-color: var(--brand); }
    .step.done { color: var(--ink-2); border-bottom-color: var(--brand); }
    .grid-3 { display: grid; grid-template-columns: 1.2fr 0.8fr 1fr; gap: 0 0.75rem; }
    .upper { text-transform: uppercase; }
    .pw { position: relative; }
    .pw .input { padding-right: 4.5rem; }
    .toggle { position: absolute; right: 0.5rem; top: 50%; transform: translateY(-50%); background: none; border: none; color: var(--brand); font: inherit; font-size: 0.82rem; font-weight: 600; cursor: pointer; }
    .rules { list-style: none; padding: 0; margin: 0 0 1.25rem; display: grid; grid-template-columns: 1fr 1fr; gap: 0.35rem 0.75rem; font-size: 0.8rem; color: var(--ink-3); }
    .rules li::before { content: '• '; }
    .rules li.ok { color: var(--brand); font-weight: 600; }
    .rules li.ok::before { content: '✓ '; }
    .nav-actions { display: flex; gap: 0.65rem; }
    .grow-btn { flex: 1; }
    .amount-ok { color: var(--brand); }
    @media (max-width: 600px) {
      .grid-3 { grid-template-columns: 1fr; }
      .rules { grid-template-columns: 1fr; }
    }
  `]
})
export class RegistroComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly pasoActual = signal<1 | 2 | 3 | 4>(1);
  readonly cargando = signal(false);
  readonly validandoPaso = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly errorCurpDuplicada = signal<string | null>(null);
  readonly errorRfcDuplicado = signal<string | null>(null);
  readonly errorCorreoDuplicado = signal<string | null>(null);
  readonly errorTelefonoDuplicado = signal<string | null>(null);
  readonly rangosIngreso = RANGOS_INGRESO_MENSUAL;
  readonly codigoEnviado = signal(false);
  readonly correoVerificado = signal(false);
  readonly correoEnviadoPara = signal('');
  readonly codigoCorreo = signal('');
  readonly confirmandoCodigo = signal(false);
  readonly errorCodigoCorreo = signal<string | null>(null);
  readonly avisoCodigoCorreo = signal<string | null>(null);
  readonly cuentaCreada = signal<ClienteResponse | null>(null);
  readonly mostrarPassword = signal(false);
  readonly mostrarConfirmacion = signal(false);

  readonly estadosDisponibles = Array.from(new Set(CATALOGO_SEPOMEX.map((i) => i.estado)));
  readonly estadoSeleccionado = signal<string>('Ciudad de México');
  readonly ubicacionesDelEstado = signal<CatalogoPostalItem[]>(
    CATALOGO_SEPOMEX.filter((i) => i.estado === 'Ciudad de México')
  );
  readonly avisoCp = signal<string | null>(null);
  readonly ocupacionesDisponibles = signal<string[]>(OCUPACIONES_FALLBACK);
  readonly coloniasDelCp = signal<string[]>(COLONIAS_FALLBACK[CATALOGO_SEPOMEX[8].codigoPostal] ?? ['Centro']);
  readonly esEstudiante = signal(false);

  readonly maxFechaNacimiento = (() => {
    const h = new Date();
    const y = h.getFullYear() - 18;
    const m = String(h.getMonth() + 1).padStart(2, '0');
    const d = String(h.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  })();

  readonly personalForm = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.pattern(PATRON_NOMBRE)]],
    segundoNombre: ['', [Validators.pattern(PATRON_NOMBRE)]],
    apellidoPaterno: ['', [Validators.required, Validators.pattern(PATRON_NOMBRE)]],
    apellidoMaterno: ['', [Validators.required, Validators.pattern(PATRON_NOMBRE)]],
    fechaNacimiento: ['', [Validators.required, mayorDeEdadValidator]],
    sexo: ['H', [Validators.required]],
    estadoCivil: ['SOLTERO', [Validators.required]],
    curp: ['', [Validators.required, Validators.pattern(PATRON_CURP)]],
    rfc: ['', [Validators.required, Validators.pattern(PATRON_RFC)]]
  });

  readonly contactoForm = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.pattern(PATRON_EMAIL), Validators.maxLength(100)]],
    telefonoMovil: ['', [Validators.required, Validators.pattern(PATRON_TELEFONO)]],
    telefonoAlternativo: ['', [Validators.pattern(PATRON_TELEFONO)]],
    ocupacionCatalogo: ['', [Validators.required]],
    ocupacionOtra: [''],
    empresa: ['', [Validators.required, Validators.maxLength(100)]],
    ingresoMensual: ['', [Validators.required]]
  });

  readonly domicilioForm = this.fb.nonNullable.group({
    codigoPostal: [CATALOGO_SEPOMEX[8].codigoPostal, [Validators.required, Validators.pattern(PATRON_CP)]],
    estado: ['Ciudad de México', [Validators.required]],
    ubicacionClave: [this.claveUbicacion(CATALOGO_SEPOMEX[8]), [Validators.required]],
    coloniaCatalogo: ['', [Validators.required]],
    coloniaOtra: [''],
    calle: ['', [Validators.required, Validators.maxLength(100)]],
    numeroExterior: ['', [Validators.required, Validators.maxLength(6), Validators.pattern(PATRON_NUM_DOMICILIO)]],
    numeroInterior: ['', [Validators.maxLength(6), Validators.pattern(/^(?:[A-Za-z0-9/\-]{1,6})?$/)]]
  });

  readonly seguridadForm = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.pattern(PATRON_PASSWORD)]],
      confirmarPassword: ['', [Validators.required]]
    },
    { validators: coincidirPasswordValidator }
  );

  readonly passValue = signal('');
  readonly tieneMayusYMinus = computed(() => /[a-z]/.test(this.passValue()) && /[A-Z]/.test(this.passValue()));
  readonly tieneNumero = computed(() => /[0-9]/.test(this.passValue()));
  readonly tieneSimbolo = computed(() => /[@#$%^&+=!._-]/.test(this.passValue()));

  constructor() {
    this.seguridadForm.controls.password.valueChanges.subscribe((v) => this.passValue.set(v ?? ''));
    this.cargarUbicacionesDeEstado('Ciudad de México', this.claveUbicacion(CATALOGO_SEPOMEX[8]));
    this.cargarOcupaciones();
    this.cargarColoniasDeCp(CATALOGO_SEPOMEX[8].codigoPostal);
  }

  private cargarOcupaciones(): void {
    this.authService.obtenerOcupaciones().subscribe({
      next: (resp) => {
        const lista = resp.datos ?? [];
        if (lista.length > 0) {
          this.ocupacionesDisponibles.set(lista);
        }
      },
      error: () => {
        // Conserva catálogo local OCUPACIONES_FALLBACK
      }
    });
  }

  alCambiarOcupacion(): void {
    const sel = this.contactoForm.controls.ocupacionCatalogo.value;
    const otraCtrl = this.contactoForm.controls.ocupacionOtra;
    const empCtrl = this.contactoForm.controls.empresa;

    if (sel === 'Otro') {
      otraCtrl.setValidators([Validators.required, Validators.pattern(PATRON_OCUPACION)]);
    } else {
      otraCtrl.clearValidators();
      otraCtrl.setValue('');
    }
    otraCtrl.updateValueAndValidity();

    const estudiante = sel === 'Estudiante';
    this.esEstudiante.set(estudiante);
    if (estudiante) {
      empCtrl.clearValidators();
      empCtrl.setValue('');
      empCtrl.disable();
    } else {
      empCtrl.enable();
      empCtrl.setValidators([Validators.required, Validators.maxLength(100)]);
    }
    empCtrl.updateValueAndValidity();
  }

  alCambiarColonia(): void {
    const sel = this.domicilioForm.controls.coloniaCatalogo.value;
    const otraCtrl = this.domicilioForm.controls.coloniaOtra;
    if (sel === 'Otra') {
      otraCtrl.setValidators([Validators.required, Validators.maxLength(100)]);
    } else {
      otraCtrl.clearValidators();
      otraCtrl.setValue('');
    }
    otraCtrl.updateValueAndValidity();
  }

  private cargarColoniasDeCp(cp: string): void {
    const limpio = cp.trim();
    const locales = COLONIAS_FALLBACK[limpio] ?? [];
    if (locales.length > 0) {
      this.aplicarColonias(locales);
    }
    this.authService.obtenerColoniasPorCp(limpio).subscribe({
      next: (resp) => {
        const remotas = resp.datos ?? [];
        if (this.domicilioForm.controls.codigoPostal.value.trim() === limpio) {
          this.aplicarColonias(remotas.length > 0 ? remotas : locales);
        }
      },
      error: () => {
        if (locales.length === 0) {
          this.aplicarColonias([]);
        }
      }
    });
  }

  private aplicarColonias(lista: string[]): void {
    const unicas = Array.from(new Set(lista));
    this.coloniasDelCp.set(unicas);
    const colCtrl = this.domicilioForm.controls.coloniaCatalogo;
    const actual = colCtrl.value;
    if (actual && actual !== 'Otra' && !unicas.includes(actual)) {
      colCtrl.setValue('');
      this.alCambiarColonia();
    }
  }

  claveUbicacion(item: CatalogoPostalItem): string {
    return `${item.codigoPostal}|${item.municipio}`;
  }

  alCambiarEstado(): void {
    this.avisoCp.set(null);
    const estado = this.domicilioForm.controls.estado.value;
    this.estadoSeleccionado.set(estado);
    this.cargarUbicacionesDeEstado(estado);
  }

  alCambiarUbicacion(): void {
    this.avisoCp.set(null);
    const [cp] = this.domicilioForm.controls.ubicacionClave.value.split('|');
    if (cp) {
      this.domicilioForm.controls.codigoPostal.setValue(cp);
      this.cargarColoniasDeCp(cp);
    }
  }

  onCodigoPostalInput(): void {
    const ctrl = this.domicilioForm.controls.codigoPostal;
    const cp = ctrl.value.replace(/\D/g, '').slice(0, 5);
    if (ctrl.value !== cp) {
      ctrl.setValue(cp, { emitEvent: false });
    }
    if (cp.length < 5) {
      this.avisoCp.set(null);
      return;
    }

    this.avisoCp.set('Buscando código postal...');
    this.cargarColoniasDeCp(cp);
    this.authService.buscarPorCodigoPostal(cp).subscribe({
      next: (resp) => {
        const encontrados = resp.datos ?? [];
        if (encontrados.length === 0) {
          this.avisoCp.set('CP no encontrado en catálogo; elige Estado y Municipio.');
          return;
        }
        const primero = encontrados[0];
        this.avisoCp.set(`${primero.municipio}, ${primero.estado}`);
        this.domicilioForm.controls.estado.setValue(primero.estado);
        this.estadoSeleccionado.set(primero.estado);
        this.cargarUbicacionesDeEstado(primero.estado, this.claveUbicacion(primero), primero);
      },
      error: () => {
        const local = CATALOGO_SEPOMEX.find((i) => i.codigoPostal === cp);
        if (local) {
          this.avisoCp.set(`${local.municipio}, ${local.estado}`);
          this.domicilioForm.controls.estado.setValue(local.estado);
          this.estadoSeleccionado.set(local.estado);
          this.cargarUbicacionesDeEstado(local.estado, this.claveUbicacion(local), local);
        } else {
          this.avisoCp.set('CP no encontrado en catálogo; elige Estado y Municipio.');
        }
      }
    });
  }

  private cargarUbicacionesDeEstado(estado: string, clavePreferida?: string, itemExtra?: CatalogoPostalItem): void {
    const locales = CATALOGO_SEPOMEX.filter((i) => i.estado === estado);
    this.aplicarListaUbicaciones(locales, clavePreferida, itemExtra);

    this.authService.obtenerUbicacionesPorEstado(estado).subscribe({
      next: (resp) => {
        const remotas = resp.datos ?? [];
        if ((remotas.length > 0 || itemExtra) && this.estadoSeleccionado() === estado) {
          this.aplicarListaUbicaciones(remotas.length > 0 ? remotas : locales, clavePreferida, itemExtra);
        }
      },
      error: () => {
        // Conserva catálogo local si no hay conexión con /catalogos/ubicaciones
      }
    });
  }

  private aplicarListaUbicaciones(
    lista: CatalogoPostalItem[],
    clavePreferida?: string,
    itemExtra?: CatalogoPostalItem
  ): void {
    let combinada = [...lista];
    if (itemExtra && !combinada.some((i) => this.claveUbicacion(i) === this.claveUbicacion(itemExtra))) {
      combinada = [itemExtra, ...combinada];
    }
    if (combinada.length === 0) return;
    this.ubicacionesDelEstado.set(combinada);
    const destino =
      (clavePreferida && combinada.find((i) => this.claveUbicacion(i) === clavePreferida)) || combinada[0];
    this.domicilioForm.controls.ubicacionClave.setValue(this.claveUbicacion(destino));
    this.domicilioForm.controls.codigoPostal.setValue(destino.codigoPostal);
    this.cargarColoniasDeCp(destino.codigoPostal);
  }

  sanitizarCampo(
    form: { get(k: string): AbstractControl | null },
    campo: string,
    tipo: 'LETRAS' | 'DIGITOS' | 'DECIMAL' | 'CURP' | 'RFC' | 'SIN_ESPACIOS' | 'TEXTO' | 'NUM_DOMICILIO',
    maxLen: number
  ): void {
    const ctrl = form.get(campo);
    if (!ctrl) return;
    let val = String(ctrl.value ?? '');
    switch (tipo) {
      case 'LETRAS':
        val = val.replace(/[^a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]/g, '');
        break;
      case 'DIGITOS':
        val = val.replace(/\D/g, '');
        break;
      case 'DECIMAL': {
        val = val.replace(/[^0-9.]/g, '');
        const partes = val.split('.');
        if (partes.length > 2) {
          val = `${partes[0]}.${partes.slice(1).join('')}`;
        }
        const [ent, dec] = val.split('.');
        val = dec !== undefined ? `${ent.slice(0, 13)}.${dec.slice(0, 2)}` : ent.slice(0, 13);
        break;
      }
      case 'CURP':
        val = val.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
        break;
      case 'RFC':
        val = val.replace(/[^a-zA-ZÑñ&0-9]/g, '').toUpperCase();
        break;
      case 'SIN_ESPACIOS':
        val = val.replace(/\s/g, '');
        break;
      case 'TEXTO':
        val = val.replace(/[^a-zA-ZáéíóúÁÉÍÓÚüÜñÑ0-9\s.,#/\-&]/g, '');
        break;
      case 'NUM_DOMICILIO':
        val = val.replace(/[^a-zA-Z0-9/\-]/g, '');
        break;
    }
    val = val.slice(0, maxLen);
    if (ctrl.value !== val) {
      ctrl.setValue(val);
      ctrl.updateValueAndValidity();
    }
  }

  tocadoInvalido(form: { get(k: string): AbstractControl | null }, campo: string): boolean {
    const ctrl = form.get(campo);
    return !!ctrl && ctrl.touched && ctrl.invalid;
  }

  alEditarCurp(): void {
    this.errorCurpDuplicada.set(null);
    this.sanitizarCampo(this.personalForm, 'curp', 'CURP', 18);
  }

  alEditarRfc(): void {
    this.errorRfcDuplicado.set(null);
    this.sanitizarCampo(this.personalForm, 'rfc', 'RFC', 13);
  }

  private resumirCamposInvalidos(
    form: { get(k: string): AbstractControl | null },
    etiquetas: Array<[string, string]>
  ): string {
    const faltantes = etiquetas
      .filter(([campo]) => {
        const ctrl = form.get(campo);
        return !!ctrl && ctrl.enabled && ctrl.invalid;
      })
      .map(([, label]) => label);
    return faltantes.length > 0
      ? `Revisa los campos marcados en rojo: ${faltantes.join(', ')}.`
      : 'Revisa los campos marcados en rojo.';
  }

  avanzarDesdePaso1(): void {
    this.errorMsg.set(null);
    this.errorCurpDuplicada.set(null);
    this.errorRfcDuplicado.set(null);
    if (this.personalForm.invalid) {
      this.personalForm.markAllAsTouched();
      this.errorMsg.set(this.resumirCamposInvalidos(this.personalForm, [
        ['nombre', 'Nombre'],
        ['segundoNombre', 'Segundo nombre'],
        ['apellidoPaterno', 'Apellido paterno'],
        ['apellidoMaterno', 'Apellido materno'],
        ['fechaNacimiento', 'Fecha de nacimiento'],
        ['curp', 'CURP'],
        ['rfc', 'RFC']
      ]));
      return;
    }
    if (this.validandoPaso()) return;

    const { curp, rfc } = this.personalForm.getRawValue();
    this.validandoPaso.set(true);
    this.authService.verificarDisponibilidad({ curp, rfc }).subscribe({
      next: (resp) => {
        this.validandoPaso.set(false);
        const disp = resp.datos;
        if (!disp) {
          this.pasoActual.set(2);
          return;
        }
        if (!disp.curpDisponible) {
          this.errorCurpDuplicada.set('Esta CURP ya está registrada');
        }
        if (!disp.rfcDisponible) {
          this.errorRfcDuplicado.set('Este RFC ya está registrado');
        }
        if (disp.curpDisponible && disp.rfcDisponible) {
          this.pasoActual.set(2);
        }
      },
      error: () => {
        this.validandoPaso.set(false);
        this.errorMsg.set('No se pudo verificar la disponibilidad de CURP/RFC.');
      }
    });
  }

  volverPasoOAtras(): void {
    const paso = this.pasoActual();
    if (paso > 1 && !this.cargando() && !this.validandoPaso()) {
      this.errorMsg.set(null);
      this.pasoActual.set((paso - 1) as 1 | 2 | 3);
    } else {
      this.irALogin();
    }
  }

  alEditarTelefonoMovil(): void {
    this.errorTelefonoDuplicado.set(null);
    this.sanitizarCampo(this.contactoForm, 'telefonoMovil', 'DIGITOS', 10);
  }

  alEditarCorreo(): void {
    this.errorCorreoDuplicado.set(null);
    this.sanitizarCampo(this.contactoForm, 'correo', 'SIN_ESPACIOS', 100);
    const norm = this.contactoForm.controls.correo.value.trim().toLowerCase();
    if (norm !== this.correoEnviadoPara()) {
      this.correoVerificado.set(false);
      this.codigoEnviado.set(false);
      this.codigoCorreo.set('');
      this.errorCodigoCorreo.set(null);
      this.avisoCodigoCorreo.set(null);
    }
  }

  alEditarCodigo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/\D/g, '').slice(0, 6);
    if (input.value !== limpio) input.value = limpio;
    this.codigoCorreo.set(limpio);
    this.errorCodigoCorreo.set(null);
  }

  reenviarCodigoCorreo(): void {
    const correo = this.contactoForm.controls.correo.value.trim().toLowerCase();
    this.confirmandoCodigo.set(true);
    this.errorCodigoCorreo.set(null);
    this.authService.enviarCodigoVerificacionCorreo(correo).subscribe({
      next: () => {
        this.confirmandoCodigo.set(false);
        this.codigoEnviado.set(true);
        this.correoEnviadoPara.set(correo);
        this.avisoCodigoCorreo.set(`Enviamos un nuevo código de 6 dígitos a ${correo}.`);
      },
      error: (err: HttpErrorResponse) => {
        this.confirmandoCodigo.set(false);
        const body = err.error as GenericResponse<unknown> | null;
        this.errorCodigoCorreo.set(body?.mensaje || 'No fue posible reenviar el código.');
      }
    });
  }

  confirmarCodigo(avanzarSiOk = false): void {
    const correo = this.contactoForm.controls.correo.value.trim().toLowerCase();
    const codigo = this.codigoCorreo().trim();
    if (codigo.length !== 6) {
      this.errorCodigoCorreo.set('Ingresa los 6 dígitos');
      return;
    }
    this.confirmandoCodigo.set(true);
    this.errorCodigoCorreo.set(null);
    this.authService.confirmarCodigoVerificacionCorreo(correo, codigo).subscribe({
      next: (resp) => {
        this.confirmandoCodigo.set(false);
        if (resp.datos) {
          this.correoVerificado.set(true);
          if (avanzarSiOk) {
            this.pasoActual.set(3);
          }
        } else {
          this.errorCodigoCorreo.set('Código incorrecto');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.confirmandoCodigo.set(false);
        const body = err.error as GenericResponse<unknown> | null;
        this.errorCodigoCorreo.set(body?.mensaje || 'Código inválido o expirado.');
      }
    });
  }

  avanzarDesdePaso2(): void {
    this.errorMsg.set(null);
    this.errorCorreoDuplicado.set(null);
    this.errorTelefonoDuplicado.set(null);
    if (this.contactoForm.invalid) {
      this.contactoForm.markAllAsTouched();
      this.errorMsg.set(this.resumirCamposInvalidos(this.contactoForm, [
        ['correo', 'Correo electrónico'],
        ['telefonoMovil', 'Teléfono móvil'],
        ['telefonoAlternativo', 'Tel. alternativo'],
        ['ocupacionCatalogo', 'Ocupación'],
        ['ocupacionOtra', 'Especifica tu ocupación'],
        ['empresa', 'Empresa'],
        ['ingresoMensual', 'Ingreso mensual']
      ]));
      return;
    }
    if (this.validandoPaso() || this.confirmandoCodigo()) return;

    const correo = this.contactoForm.controls.correo.value.trim().toLowerCase();
    const telefonoMovil = this.contactoForm.controls.telefonoMovil.value.trim();
    this.validandoPaso.set(true);
    this.authService.verificarDisponibilidad({ correo, telefonoMovil }).subscribe({
      next: (resp) => {
        const disp = resp.datos;
        if (disp) {
          if (!disp.correoDisponible) {
            this.errorCorreoDuplicado.set('Este correo ya está registrado');
          }
          if (disp.telefonoDisponible === false) {
            this.errorTelefonoDuplicado.set('Este teléfono ya está asociado a otra cuenta activa');
          }
          if (!disp.correoDisponible || disp.telefonoDisponible === false) {
            this.validandoPaso.set(false);
            return;
          }
        }
        if (this.correoVerificado()) {
          this.validandoPaso.set(false);
          this.pasoActual.set(3);
          return;
        }
        if (!this.codigoEnviado()) {
          this.authService.enviarCodigoVerificacionCorreo(correo).subscribe({
            next: () => {
              this.validandoPaso.set(false);
              this.codigoEnviado.set(true);
              this.correoEnviadoPara.set(correo);
              this.avisoCodigoCorreo.set(`Enviamos un código de 6 dígitos a ${correo}. Ingrésalo para confirmar tu correo.`);
            },
            error: (err: HttpErrorResponse) => {
              this.validandoPaso.set(false);
              const body = err.error as GenericResponse<unknown> | null;
              this.errorCorreoDuplicado.set(body?.mensaje || 'No se pudo enviar el código a este correo.');
            }
          });
          return;
        }
        this.validandoPaso.set(false);
        this.confirmarCodigo(true);
      },
      error: () => {
        this.validandoPaso.set(false);
        this.errorMsg.set('No se pudo verificar la disponibilidad del correo.');
      }
    });
  }

  avanzarDesdePaso3(): void {
    this.errorMsg.set(null);
    if (this.domicilioForm.invalid) {
      this.domicilioForm.markAllAsTouched();
      this.errorMsg.set(this.resumirCamposInvalidos(this.domicilioForm, [
        ['codigoPostal', 'Código postal'],
        ['estado', 'Estado'],
        ['ubicacionClave', 'Municipio'],
        ['coloniaCatalogo', 'Colonia'],
        ['coloniaOtra', 'Nombre de tu colonia'],
        ['calle', 'Calle'],
        ['numeroExterior', 'Número exterior'],
        ['numeroInterior', 'Número interior']
      ]));
      return;
    }
    this.pasoActual.set(4);
  }

  enviarRegistro(): void {
    this.errorMsg.set(null);
    if (this.seguridadForm.invalid || this.cargando()) {
      this.seguridadForm.markAllAsTouched();
      if (!this.cargando()) {
        this.errorMsg.set(this.seguridadForm.hasError('noCoinciden')
          ? 'Las contraseñas no coinciden.'
          : this.resumirCamposInvalidos(this.seguridadForm, [
              ['password', 'Contraseña'],
              ['confirmarPassword', 'Confirmar contraseña']
            ]));
      }
      return;
    }

    const p = this.personalForm.getRawValue();
    const c = this.contactoForm.getRawValue();
    const d = this.domicilioForm.getRawValue();
    const s = this.seguridadForm.getRawValue();
    const [codigoPostal, municipio] = d.ubicacionClave.split('|');
    const ocupacionFinal = c.ocupacionCatalogo === 'Otro'
      ? c.ocupacionOtra.trim()
      : c.ocupacionCatalogo.trim();
    const coloniaFinal = d.coloniaCatalogo === 'Otra'
      ? d.coloniaOtra.trim()
      : d.coloniaCatalogo.trim();

    const payload: RegistroClienteRequest = {
      nombre: p.nombre.trim(),
      segundoNombre: p.segundoNombre.trim() || null,
      apellidoPaterno: p.apellidoPaterno.trim(),
      apellidoMaterno: p.apellidoMaterno.trim(),
      fechaNacimiento: p.fechaNacimiento,
      curp: p.curp.trim().toUpperCase(),
      rfc: p.rfc.trim().toUpperCase(),
      sexo: p.sexo,
      nacionalidad: 'Mexicana',
      estadoCivil: p.estadoCivil,
      correo: c.correo.trim().toLowerCase(),
      telefonoMovil: c.telefonoMovil.trim(),
      telefonoAlternativo: c.telefonoAlternativo.trim() || null,
      ocupacion: ocupacionFinal,
      empresa: c.ocupacionCatalogo === 'Estudiante' ? null : c.empresa.trim(),
      ingresoMensual: Number(c.ingresoMensual),
      domicilio: {
        calle: d.calle.trim(),
        numeroExterior: d.numeroExterior.trim(),
        numeroInterior: d.numeroInterior.trim() || null,
        colonia: coloniaFinal,
        municipio,
        estado: d.estado,
        codigoPostal,
        pais: 'México'
      },
      password: s.password
    };

    this.cargando.set(true);
    this.authService.registrarCliente(payload).subscribe({
      next: (resp) => {
        this.cargando.set(false);
        if (resp.datos) {
          this.cuentaCreada.set(resp.datos);
        } else {
          this.errorMsg.set(resp.mensaje || 'Registro completado sin detalle.');
        }
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        const body = err.error as GenericResponse<unknown> | null;
        if (body?.errores && Object.keys(body.errores).length > 0) {
          this.errorMsg.set(Object.values(body.errores).join(' · '));
        } else {
          this.errorMsg.set(body?.mensaje || 'No se pudo completar el registro.');
        }
      }
    });
  }

  irALogin(): void {
    void this.router.navigate(['/login']);
  }
}

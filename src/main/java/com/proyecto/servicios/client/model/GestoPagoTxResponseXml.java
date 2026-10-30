package com.proyecto.servicios.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class GestoPagoTxResponseXml {

    @XmlElement(name = "ID_TX")
    private String idTx;

    @XmlElement(name = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @XmlElement(name = "SALDO")
    private String saldo;

    @XmlElement(name = "COMISION")
    private String comision;

    @XmlElement(name = "FECHA")
    private String fecha;

    @XmlElement(name = "MONTO")
    private String monto;

    @XmlElement(name = "REFERENCIA")
    private String referencia;

    @XmlElement(name = "MENSAJE")
    private Mensaje mensaje;

    public String getCodigo() {
        return mensaje != null ? mensaje.getCodigo() : null;
    }

    public String getTexto() {
        return mensaje != null ? mensaje.getTexto() : null;
    }

    public void setCodigo(String codigo) {
        if (this.mensaje == null) {
            this.mensaje = new Mensaje();
        }
        this.mensaje.setCodigo(codigo);
    }

    public void setTexto(String texto) {
        if (this.mensaje == null) {
            this.mensaje = new Mensaje();
        }
        this.mensaje.setTexto(texto);
    }

    /**
     * Según la especificación oficial de GestoPago, la transacción fue exitosa
     * ÚNICAMENTE cuando el código retornado es exactamente "01".
     */
    public boolean esExitosa() {
        return "01".equals(getCodigo());
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class Mensaje {
        @XmlElement(name = "CODIGO")
        private String codigo;

        @XmlElement(name = "TEXTO")
        private String texto;
    }
}

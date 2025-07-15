package vista;

import javafx.beans.property.SimpleStringProperty;

public class ItemTablaSimbolos {

    private final SimpleStringProperty nombre;
    private final SimpleStringProperty tipo;
    private final SimpleStringProperty valor;
    private final SimpleStringProperty alcance;
    private final SimpleStringProperty secuenciaDeOperaciones;

    public ItemTablaSimbolos(String nombre, String tipo, String valor, String alcance, String secuenciaDeOperaciones) {
        this.nombre = new SimpleStringProperty(nombre);
        this.tipo = new SimpleStringProperty(tipo);
        this.valor = new SimpleStringProperty(valor);
        this.alcance = new SimpleStringProperty(alcance);
        this.secuenciaDeOperaciones = new SimpleStringProperty(secuenciaDeOperaciones);
    }

    public String getNombre() {
        return nombre.get();
    }

    public SimpleStringProperty nombreProperty() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public String getTipo() {
        return tipo.get();
    }

    public SimpleStringProperty tipoProperty() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo.set(tipo);
    }

    public String getValor() {
        return valor.get();
    }

    public SimpleStringProperty valorProperty() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor.set(valor);
    }

    public String getAlcance() {
        return alcance.get();
    }

    public SimpleStringProperty alcanceProperty() {
        return alcance;
    }

    public void setAlcance(String alcance) {
        this.alcance.set(alcance);
    }

    public String getSecuenciaDeOperaciones() {
        return secuenciaDeOperaciones.get();
    }

    public SimpleStringProperty secuenciaDeOperacionesProperty() {
        return secuenciaDeOperaciones;
    }

    public void setSecuenciaDeOperaciones(String secuenciaDeOperaciones) {
        this.secuenciaDeOperaciones.set(secuenciaDeOperaciones);
    }
}

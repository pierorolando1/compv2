package vista;

import javafx.beans.property.SimpleStringProperty;

public class ItemTablaSimbolos {

    private final SimpleStringProperty nombre;
    private final SimpleStringProperty tipo;
    private final SimpleStringProperty tipoDato;
    private final SimpleStringProperty ambito;
    private final SimpleStringProperty parametros;

    public ItemTablaSimbolos(String nombre, String tipo, String tipoDato, String ambito, String parametros) {
        this.nombre = new SimpleStringProperty(nombre);
        this.tipo = new SimpleStringProperty(tipo);
        this.tipoDato = new SimpleStringProperty(tipoDato);
        this.ambito = new SimpleStringProperty(ambito);
        this.parametros = new SimpleStringProperty(parametros);
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

    public String getTipoDato() {
        return tipoDato.get();
    }

    public SimpleStringProperty tipoDatoProperty() {
        return tipoDato;
    }

    public void setTipoDato(String tipoDato) {
        this.tipoDato.set(tipoDato);
    }

    public String getAmbito() {
        return ambito.get();
    }

    public SimpleStringProperty ambitoProperty() {
        return ambito;
    }

    public void setAmbito(String ambito) {
        this.ambito.set(ambito);
    }

    public String getParametros() {
        return parametros.get();
    }

    public SimpleStringProperty parametrosProperty() {
        return parametros;
    }

    public void setParametros(String parametros) {
        this.parametros.set(parametros);
    }
}

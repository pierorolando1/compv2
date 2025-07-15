package semantico;

import java.util.ArrayList;
import java.util.List;

public class Simbolo {
    private String nombre;
    private String ambito;
    private int fila;
    private int columna;
    private Object valor;
    private List<String> secuenciaDeOperaciones;

    public Simbolo(String nombre, String ambito, int fila, int columna) {
        this.nombre = nombre;
        this.ambito = ambito;
        this.fila = fila;
        this.columna = columna;
        this.secuenciaDeOperaciones = new ArrayList<>();
    }

    public Simbolo() {
        this.secuenciaDeOperaciones = new ArrayList<>();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAmbito() {
        return ambito;
    }

    public void setAmbito(String ambito) {
        this.ambito = ambito;
    }

    public int getFila() {
        return fila;
    }

    public void setFila(int fila) {
        this.fila = fila;
    }

    public int getColumna() {
        return columna;
    }

    public void setColumna(int columna) {
        this.columna = columna;
    }

    public Object getValor() {
        return valor;
    }

    public void setValor(Object valor) {
        this.valor = valor;
    }

    public List<String> getSecuenciaDeOperaciones() {
        return secuenciaDeOperaciones;
    }

    public void agregarOperacion(String operacion) {
        this.secuenciaDeOperaciones.add(operacion);
    }
}

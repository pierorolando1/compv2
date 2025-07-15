package semantico;

import java.util.List;

public class Variable extends Simbolo {
    private TipoDato tipoDato;

    public Variable(String nombre, TipoDato tipo, String ambito, int fila, int columna) {
        super(nombre, ambito, fila, columna);
        this.tipoDato = tipo;
    }

    public Variable() {
        super();
    }

    public TipoDato getTipoDato() {
        return tipoDato;
    }

    public void setTipoDato(TipoDato tipoDato) {
        this.tipoDato = tipoDato;
    }

    /**
     * Asigna un valor a la variable y registra la operación en la secuencia
     * @param nuevoValor El nuevo valor a asignar
     * @param operacion Descripción de la operación realizada
     */
    public void asignarValor(Object nuevoValor, String operacion) {
        Object valorAnterior = getValor();
        setValor(nuevoValor);
        
        String descripcionOperacion = String.format("%s: %s -> %s", 
            operacion, 
            valorAnterior != null ? valorAnterior.toString() : "null", 
            nuevoValor != null ? nuevoValor.toString() : "null");
        
        agregarOperacion(descripcionOperacion);
    }

    /**
     * Asigna un valor inicial a la variable
     * @param valorInicial El valor inicial de la variable
     */
    public void inicializar(Object valorInicial) {
        setValor(valorInicial);
        agregarOperacion("Inicialización: " + (valorInicial != null ? valorInicial.toString() : "null"));
    }

    /**
     * Incrementa el valor de la variable (para operadores ++ y +=)
     * @param incremento El valor a incrementar
     * @param esPostIncremento true si es post-incremento (var++), false si es pre-incremento (++var)
     */
    public void incrementar(Object incremento, boolean esPostIncremento) {
        Object valorAnterior = getValor();
        
        if (valorAnterior instanceof Number && incremento instanceof Number) {
            double valorActual = ((Number) valorAnterior).doubleValue();
            double valorIncremento = ((Number) incremento).doubleValue();
            double nuevoValor = valorActual + valorIncremento;
            
            setValor(nuevoValor);
            
            String tipoOperacion = esPostIncremento ? "Post-incremento" : "Pre-incremento";
            String descripcion = String.format("%s (+%s): %s -> %s", 
                tipoOperacion, incremento, valorAnterior, nuevoValor);
            
            agregarOperacion(descripcion);
        }
    }

    /**
     * Decrementa el valor de la variable (para operadores -- y -=)
     * @param decremento El valor a decrementar
     * @param esPostDecremento true si es post-decremento (var--), false si es pre-decremento (--var)
     */
    public void decrementar(Object decremento, boolean esPostDecremento) {
        Object valorAnterior = getValor();
        
        if (valorAnterior instanceof Number && decremento instanceof Number) {
            double valorActual = ((Number) valorAnterior).doubleValue();
            double valorDecremento = ((Number) decremento).doubleValue();
            double nuevoValor = valorActual - valorDecremento;
            
            setValor(nuevoValor);
            
            String tipoOperacion = esPostDecremento ? "Post-decremento" : "Pre-decremento";
            String descripcion = String.format("%s (-%s): %s -> %s", 
                tipoOperacion, decremento, valorAnterior, nuevoValor);
            
            agregarOperacion(descripcion);
        }
    }

    /**
     * Obtiene el historial completo de cambios de la variable
     * @return Lista con todas las operaciones realizadas sobre la variable
     */
    public List<String> getHistorialCambios() {
        return getSecuenciaDeOperaciones();
    }

    /**
     * Obtiene el valor actual de la variable con información del tipo
     * @return String con el valor y tipo de la variable
     */
    public String getValorConTipo() {
        Object valor = getValor();
        String tipoStr = tipoDato != null ? tipoDato.toString() : "Sin tipo";
        return String.format("Valor: %s, Tipo: %s", 
            valor != null ? valor.toString() : "null", tipoStr);
    }

    /**
     * Verifica si la variable ha sido inicializada
     * @return true si la variable tiene un valor asignado
     */
    public boolean estaInicializada() {
        return getValor() != null;
    }

    @Override
    public String toString() {
        return String.format("Variable[nombre=%s, ambito=%s, tipo=%s, valor=%s, operaciones=%d]",
            getNombre(), getAmbito(), tipoDato, getValor(), getSecuenciaDeOperaciones().size());
    }
}

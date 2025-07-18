/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package semantico;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Cristhian
 */
public class TablaSimbolos {

    public static ArrayList<Simbolo> tablaSimbolos = new ArrayList<>();

    //pila que almacena todos los tipos de datos que se definen (algunos deben ser desechados cuando hay error)
    private Deque<TipoDato> tiposDato = new LinkedList<>();

    //contiene todas las variables EN ORDEN SECUENCIAL
    public ArrayList<Variable> variables;
    //una pila que contiene una pila de variables
    private Deque<Deque<Variable>> pilaVariablesSinTipo;
    //pila de variables que inician sin tipo (por precedencia en las gramáticas)
    private Deque<Variable> variablesSinTipo;

    //Almacena los parámetros de una funcion/procedimiento
    public ArrayList<Variable> parametros;
    private boolean parametrosCorrectos;

    //Guarda las variables que están siendo usadas dentro de cuerpo para luego ser verificadas
    public ArrayList<Variable> variablesUsadas;

    //Esta guarda los parámetros que llaman a una función/procedimiento
    private ArrayList<Object> parametrosLlamada;

    public TablaSimbolos()
    {
        tablaSimbolos = new ArrayList<>();

        variables = new ArrayList<>();
        pilaVariablesSinTipo = new LinkedList<>();
        variablesSinTipo = new LinkedList<>();

        parametros = new ArrayList<>();
        parametrosCorrectos = true;

        variablesUsadas = new ArrayList<>();

        parametrosLlamada = new ArrayList<>();
    }

    /**
     * Inserta un símbolo a la tabla de símbolos xD
     * @param simbolo Simbolo a insertar (Variable o Función)
     */
    public void insertar(Simbolo simbolo)
    {
        tablaSimbolos.add(simbolo);
    }

    /**
     * Revisa si ya existe un símbolo en la tabla de símbolos.
     * @param simbolo Variable o Función
     * @return True existe, False no existe
     */
    public boolean existeSimbolo(Simbolo simbolo){
        Simbolo auxSimbolo;

        if(simbolo instanceof Variable){
            Variable aux = (Variable) simbolo;

            for(int i=0; i<tablaSimbolos.size(); i++){
                auxSimbolo = tablaSimbolos.get(i);
                if(auxSimbolo instanceof Variable){
                    if(auxSimbolo.getNombre().equals(aux.getNombre()) &&
                        auxSimbolo.getAmbito().equals(aux.getAmbito()))
                        return true;
                }
            }
            return false;
        }
        else if(simbolo instanceof Funcion){
            Funcion funcion = (Funcion) simbolo;
            Funcion aux;
            int cantidadIguales;

            for(int i=0; i<tablaSimbolos.size(); i++){
                auxSimbolo = tablaSimbolos.get(i);
                if(auxSimbolo instanceof Funcion){
                    aux = (Funcion)auxSimbolo;
                    cantidadIguales = 0;

                    if(aux.getNombre().equals(funcion.getNombre()) &&
                            aux.getParametros().size() == funcion.getParametros().size()){

                        for(int j=0; j<funcion.getParametros().size(); j++){
                            Variable varAux = aux.getParametros().get(j);
                            Variable varFuncion = funcion.getParametros().get(j);

                            //si en algún momento difieren los parámetros
                            if(varAux.getTipoDato() != varFuncion.getTipoDato()){
                                break;
                            }
                            cantidadIguales++;
                        }
                        //si llega aquí significa que sí tienen los parámetros iguales
                        if(cantidadIguales == funcion.getParametros().size())
                            return true;
                    }
                }
            }

            return false;
        }
        return false; //en realidad nunca va a llegar aquí pero yolo
    }


    /**
     * Verifica si un identificador existe dentro de las variables en la tabla de símbolos
     * @param identificador Nombre de la variable
     * @param ambito Nombre de la función o procedimiento al que pertenece la variable, null si es del cuerpo principal
     * @return True existe, False no existe
     */
    public boolean existeVariable(String identificador, String ambito){
        for(Simbolo s : tablaSimbolos){
            if(s instanceof Variable){
                if(s.getNombre().equals(identificador) &&
                        (s.getAmbito().equals("Global") || s.getAmbito().equals(ambito))){
                    return true;
                }
            }
        }
        return false;
    }

    //############################### QUICKSORT sobre las variables porque me lleva puta >:v

    /* Esta funcion toma el ultimo elemento como pivot, lo coloca es la posición correcta del arreglo
       y coloca los más pequeños que el pivot a la izquierda del pivot, los más grandes a la derecha */
    private int partition(ArrayList<Variable> vars, int low, int high)
    {
        Variable pivot = vars.get(high);
        int i = (low-1); // index of smaller element
        for (int j=low; j<high; j++){
            // Si la fila es menor
            if (vars.get(j).getFila() < pivot.getFila()) {
                i++;
                // swap arr[i] and arr[j]
                Variable temp = vars.get(i);
                vars.set(i, vars.get(j));
                vars.set(j, temp);
            }
            //Si la fila es igual pero la columna es menor
            else if(vars.get(j).getFila() == pivot.getFila() &&
                    vars.get(j).getColumna() <= pivot.getColumna()){
                i++;
                // swap arr[i] and arr[j]
                Variable temp = vars.get(i);
                vars.set(i, vars.get(j));
                vars.set(j, temp);
            }
        }
        // cambio(swap) arr[i+1] y arr[high] (o el pivot)
        Variable temp = vars.get(i+1);
        vars.set(i+1, vars.get(high));
        vars.set(high, temp);

        return i+1;
    }
    /**
     * Aplica un quicksort sobre las variables, porque el orden cambia mucho dependiendo de la cantidad
     * @param vars el atributo variables de esta clase
     * @param low indice de inicio
     * @param high indice final
     */
    private void quicksort(ArrayList<Variable> vars, int low, int high)
    {
        if (low < high)
        {
            //pi = partitioning index
            int pi = partition(vars, low, high);

            // Recursivamnte ordena los elementos antes y después de la particion
            quicksort(vars, low, pi-1);
            quicksort(vars, pi+1, high);
        }
    }

    //Llama al quicksort porque las variables no tienen orden >:v
    public void ordenarVariables(){
        quicksort(variables, 0, variables.size()-1);
    }


    //################################ VARIABLES

    /**
     * Agrega la primera variable del no terminal _variables
     *  (la de menor precedencia, por lo tanto la última en agregarse)
     * @param identificador Nombre de la variable
     */
    public void agregarVariable(String identificador, int fila, int columna){
        Variable var = new Variable(identificador, tiposDato.pollLast(), "", fila+1, columna);

        //Agrega la primera variable de la producción
        variables.add(var);

        //Saca las últimas variables sin tipo
        if(pilaVariablesSinTipo.size() != 0){
            Deque<Variable> auxVarSinTipo = pilaVariablesSinTipo.pollLast();
            while(auxVarSinTipo.size() != 0){
                variables.add(auxVarSinTipo.pollFirst());
            }
        }


        /*System.out.println();
        for(Variable v: variables) {
            System.out.println(v.identificador + " " + ((Variable) v).getTipo() + " " + v.fila + "," + v.columna);
        }*/
    }

    /**
     * Agrega una variable iniciada sin tipo, proviene del no terminal __variables
     * @param identificador Nombre de la variable
     */
    public void agregarVariableSinTipo(String identificador, int fila, int columna){
        Variable var = new Variable();
        var.setNombre(identificador);
        var.setFila(fila+1);
        var.setColumna(columna);

        variablesSinTipo.add(var);
    }

    /**
     * Retorna una variable existente dentro de la tabla de símbolos
     * @param nombreVariable Identificador de la variable
     * @param ambito Ámbito en el que se quiere la variable (si no se encuentra se busca en Global)
     * @return Variable si la encuentra, null si no la encuentra
     */
    private Variable getVariable(String nombreVariable, String ambito){
        //Le da prioridad a las variables del ámbito
        for(Simbolo s : tablaSimbolos){
            if(s instanceof Variable &&
                    s.getNombre().equals(nombreVariable) && s.getAmbito().equals(ambito)){
                return (Variable) s;
            }
        }

        //Si no la encuentra en el ámbito, busca en las globales
        for(Simbolo s : tablaSimbolos){
            if(s instanceof Variable &&
                    s.getNombre().equals(nombreVariable) && s.getAmbito().equals("Global")){
                return (Variable) s;
            }
        }

        return null;
    }

    /**
     * Alias público para getVariable que es usado por otros métodos
     * @param nombreVariable Identificador de la variable
     * @param ambito Ámbito en el que se quiere la variable
     * @return Variable si la encuentra, null si no la encuentra
     */
    public Variable getVariableEnTabla(String nombreVariable, String ambito){
        return getVariable(nombreVariable, ambito);
    }


    //################################ FUNCIONES

    /**
     * Crea una Función, le agrega parámetros y tipo de dato
     * @param identificador Identificador de la función
     * @param fila Número de fila donde fue declarada
     * @param columna Número de columna donde fue declarada
     * @return Función con parámetros y tipo
     */
    public Funcion crearFuncion(String identificador, int fila, int columna){
        Funcion funcion = new Funcion(identificador, "", parametros, tiposDato.pollLast(), fila+1, columna);
        return funcion;
    }

    /**
     * Agrega un parámetro a la lista de parámetros que se agregan a una función
     * @param identificador Identificador del parámetro
     * @param fila Número de fila donde fue declarado
     * @param columna Número de columna donde fue declarado
     */
    public void agregarParametro(String identificador, int fila, int columna){
        Variable parametro = new Variable();
        parametro.setNombre(identificador);
        parametro.setTipoDato(tiposDato.pollLast());
        parametro.setFila(fila);
        parametro.setColumna(columna);

        parametros.add(0,parametro);
    }

    /**
     * Obtiene una lista con todos los parámetros erroneos que hayan el la declaración de la función
     * @return Lista de Varibles para parámetros
     */
    public ArrayList<Variable> getParametrosErroneos(){
        ArrayList<Variable> erroneos = new ArrayList<>();

        Variable param1, param2;
        for(int i=0; i<parametros.size(); i++){
            param1 = parametros.get(i);
            for(int j=i+1; j<parametros.size(); j++){
                param2 = parametros.get(j);

                if(param1.getNombre().equals(param2.getNombre()) &&
                        !erroneos.contains(param2)
                ){
                    erroneos.add(param2);
                }
            }
        }

        parametrosCorrectos = (erroneos.size() == 0);

        if(!parametrosCorrectos){ //hay parámetros repetidos
            return erroneos;
        }
        else{ //no hay parámetros repetidos
            return null;
        }
    }

    /**
     * Es para las funciones que fallan, agregan un tipo y se queda ahí
     *  entonces hay que desecharlo
     */
    public void desecharUltimoTipoDato(){
        tiposDato.pollLast();
    }


    /**
     * Cuando se llama a una función en el código, se agrega dicho parámetro a
     *  la lista de parámetros de la llamada
     * @param param Puede ser un string (referencia a Variable), o Tipo de Dato
     */
    public void agregarParametroLlamada(Object param){
        parametrosLlamada.add(param);
    }

    /**
     * Valida si los parámetros en la llamada de la función son correctos con respecto
     *  a las funciones / procedimientos existentes
     * @param identificador Identificador de la función a llamar
     * @return True es correcto
     *         False no se encuentra una función con esas características en la tabla de símbolos
     */
    public boolean validarLlamadaFuncion(String identificador){
        Funcion funcionLlamada;
        //System.out.println(identificador + " " + parametros.size());

        for(int i=0; i<tablaSimbolos.size(); i++){
            if(tablaSimbolos.get(i) instanceof Funcion &&
                    tablaSimbolos.get(i).getNombre().equals(identificador)){
                funcionLlamada = (Funcion)tablaSimbolos.get(i);

                if(parametrosLlamada.size() == funcionLlamada.getParametros().size()){
                    boolean iguales  = true;
                    for(int k=0; k<parametrosLlamada.size(); k++){
                        Object param = parametrosLlamada.get(k);
                        Variable paramFuncion = funcionLlamada.getParametros().get(k);

                        if(param instanceof String){    //Es una variable
                            Variable var = getVariable(param.toString(), identificador);
                            if((var == null) || (var.getTipoDato() != paramFuncion.getTipoDato())){
                                iguales = false;
                                break;
                            }
                        }
                        else if(param instanceof TipoDato){
                            if(param != paramFuncion.getTipoDato()){
                                iguales = false;
                                break;
                            }
                        }else{
                            break;
                        }
                    }
                    if(iguales){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Limpia la lista de parámetros que llaman a una función
     */
    public void limpiarParametrosLlamada(){
        parametrosLlamada = new ArrayList<>();
    }


    //################################ COMPARTIDO XD

    /**
     * Agrega el tipo de dato a la pila de Tipos de Datos
     * Y además inicializa la última lista de variables sin tipo
     * @param tipoDato Valor del enum Tipo de Dato
     */
    public void agregarTipoDato(TipoDato tipoDato){
        if(variablesSinTipo.size() != 0){
            for(Variable var : variablesSinTipo){
                var.setTipoDato(tipoDato);
            }

            pilaVariablesSinTipo.add(variablesSinTipo);
            variablesSinTipo = new LinkedList<>();
        }
        this.tiposDato.add(tipoDato);
    }

    /**
     * Es para las variables que se llaman en cuerpo, para comparar contra las que hay en la tabla
     * Se almacenan porque no se les puede meter ámbito al inicio.
     * @param identificador Nombre de la variable a llamar
     * @param fila Numero de fila desde donde se llama
     * @param columna Numero de columna desde donde se llama
     */
    public void agregarVariableUsada(String identificador, int fila, int columna){
        Variable var = new Variable(identificador, null, "", fila, columna);
        variablesUsadas.add(var);
    }

    public ArrayList<Simbolo> getTablaSimbolos(){
        return tablaSimbolos;
    }

    /**
     * Actualiza el valor de una variable en la tabla de símbolos
     * @param nombreVariable Nombre de la variable
     * @param valor Nuevo valor de la variable
     * @param operacion Operación realizada (ej: "= 5", "+= 2", "++")
     * @param ambito Ámbito de la variable
     */
    public void actualizarValorVariable(String nombreVariable, Object valor, String operacion, String ambito){
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if(variable != null){
            variable.asignarValor(valor, operacion);
        }
    }

    /**
     * Incrementa el valor de una variable
     * @param nombreVariable Nombre de la variable
     * @param incremento Valor a incrementar
     * @param esPostIncremento true si es post-incremento (var++), false si es pre-incremento (++var)
     * @param ambito Ámbito de la variable
     */
    public void incrementarVariable(String nombreVariable, Object incremento, boolean esPostIncremento, String ambito) {
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if(variable != null) {
            TipoDato tipoVariable = variable.getTipoDato();
            // Solo permitir incremento en variables numéricas
            boolean tipoNumerico = (tipoVariable == TipoDato.INT || tipoVariable == TipoDato.SHORTINT || 
                                  tipoVariable == TipoDato.LONGINT || tipoVariable == TipoDato.REAL);
            
            if(tipoNumerico) {
                variable.incrementar(incremento, esPostIncremento);
            }
        }
    }

    /**
     * Decrementa el valor de una variable
     * @param nombreVariable Nombre de la variable
     * @param decremento Valor a decrementar
     * @param esPostDecremento true si es post-decremento (var--), false si es pre-decremento (--var)
     * @param ambito Ámbito de la variable
     */
    public void decrementarVariable(String nombreVariable, Object decremento, boolean esPostDecremento, String ambito) {
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if(variable != null) {
            TipoDato tipoVariable = variable.getTipoDato();
            // Solo permitir decremento en variables numéricas
            boolean tipoNumerico = (tipoVariable == TipoDato.INT || tipoVariable == TipoDato.SHORTINT || 
                                  tipoVariable == TipoDato.LONGINT || tipoVariable == TipoDato.REAL);
            
            if(tipoNumerico) {
                variable.decrementar(decremento, esPostDecremento);
            }
        }
    }

    /**
     * Inicializa una variable con un valor inicial
     * @param nombreVariable Nombre de la variable
     * @param valorInicial Valor inicial de la variable
     * @param ambito Ámbito de la variable
     */
    public void inicializarVariable(String nombreVariable, Object valorInicial, String ambito) {
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if(variable != null) {
            variable.inicializar(valorInicial);
        }
    }

    /**
     * Aplica una operación aritmética sobre una variable
     * @param nombreVariable Nombre de la variable
     * @param operador Operador (+=, -=, *=, /=)
     * @param valor Valor de la operación
     * @param ambito Ámbito de la variable
     */
    public void aplicarOperacionAritmetica(String nombreVariable, String operador, Object valor, String ambito) {
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if(variable != null) {
            TipoDato tipoVariable = variable.getTipoDato();
            TipoDato tipoValor = obtenerTipoValor(valor);
            
            // Verificar que ambos tipos sean numéricos y compatibles
            boolean tipoVariableNumerico = (tipoVariable == TipoDato.INT || tipoVariable == TipoDato.SHORTINT || 
                                          tipoVariable == TipoDato.LONGINT || tipoVariable == TipoDato.REAL);
            boolean tipoValorNumerico = (tipoValor == TipoDato.INT || tipoValor == TipoDato.SHORTINT || 
                                       tipoValor == TipoDato.LONGINT || tipoValor == TipoDato.REAL);
            
            if(tipoVariableNumerico && tipoValorNumerico && variable.getValor() instanceof Number && valor instanceof Number) {
                double valorActual = ((Number) variable.getValor()).doubleValue();
                double operando = ((Number) valor).doubleValue();
                double nuevoValor = valorActual;
                
                switch(operador) {
                    case "+=":
                        nuevoValor = valorActual + operando;
                        break;
                    case "-=":
                        nuevoValor = valorActual - operando;
                        break;
                    case "*=":
                        nuevoValor = valorActual * operando;
                        break;
                    case "/=":
                        if(operando != 0) {
                            nuevoValor = valorActual / operando;
                        }
                        break;
                }
                
                String descripcion = String.format("Operación %s %s: %s -> %s", 
                    operador, valor, valorActual, nuevoValor);
                variable.asignarValor(nuevoValor, descripcion);
            }
        }
    }

    /**
     * Asigna un valor específico a una variable (para asignaciones directas como a := 1)
     * @param nombreVariable Nombre de la variable
     * @param valor Valor a asignar (número, string, boolean, etc.)
     * @param ambito Ámbito de la variable
     */
    public void asignarValorDirecto(String nombreVariable, Object valor, String ambito) {
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if(variable != null) {
            String descripcion = String.format("Asignación directa := %s", valor);
            variable.asignarValor(valor, descripcion);
        }
    }

    /**
     * Asigna un valor expresión evaluada a una variable
     * @param nombreVariable Nombre de la variable
     * @param valorEvaluado Valor ya evaluado de la expresión
     * @param expresionOriginal String original de la expresión para referencia
     * @param ambito Ámbito de la variable
     */
    public void asignarValorExpresion(String nombreVariable, Object valorEvaluado, String expresionOriginal, String ambito) {
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if(variable != null) {
            String descripcion = String.format("Asignación := %s (evaluado de: %s)", valorEvaluado, expresionOriginal);
            variable.asignarValor(valorEvaluado, descripcion);
        }
    }

    /**
     * Obtiene el tipo de dato de un valor
     * @param valor El valor a analizar
     * @return TipoDato correspondiente al valor
     */
    public TipoDato obtenerTipoValor(Object valor) {
        if (valor instanceof Integer || valor instanceof Long) {
            return TipoDato.INT;
        } else if (valor instanceof Double || valor instanceof Float) {
            return TipoDato.REAL;
        } else if (valor instanceof String) {
            // Verificar si el string representa un identificador de variable
            String valorStr = (String) valor;
            Variable variable = getVariableEnTabla(valorStr, "Global");
            if (variable != null) {
                // Es un identificador de variable, retornar el tipo de la variable
                return variable.getTipoDato();
            }
            // Si no es un identificador de variable, es un literal string
            return TipoDato.STRING;
        } else if (valor instanceof Boolean) {
            return TipoDato.BOOLEAN;
        } else if (valor instanceof Character) {
            return TipoDato.CHAR;
        }
        return null; // Tipo desconocido
    }

    /**
     * Obtiene el tipo de dato de un valor considerando un ámbito específico
     * @param valor El valor a analizar
     * @param ambito El ámbito donde buscar la variable
     * @return TipoDato correspondiente al valor
     */
    public TipoDato obtenerTipoValor(Object valor, String ambito) {
        if (valor instanceof Integer || valor instanceof Long) {
            return TipoDato.INT;
        } else if (valor instanceof Double || valor instanceof Float) {
            return TipoDato.REAL;
        } else if (valor instanceof String) {
            // Verificar si el string representa un identificador de variable
            String valorStr = (String) valor;
            Variable variable = getVariableEnTabla(valorStr, ambito);
            if (variable != null) {
                // Es un identificador de variable, retornar el tipo de la variable
                return variable.getTipoDato();
            }
            // Si no es un identificador de variable, es un literal string
            return TipoDato.STRING;
        } else if (valor instanceof Boolean) {
            return TipoDato.BOOLEAN;
        } else if (valor instanceof Character) {
            return TipoDato.CHAR;
        }
        return null; // Tipo desconocido
    }

    /**
     * Verifica si dos tipos son compatibles para asignación
     * @param tipoVariable Tipo de la variable de destino
     * @param tipoValor Tipo del valor a asignar
     * @return true si son compatibles, false si no
     */
    public boolean sonTiposCompatibles(TipoDato tipoVariable, TipoDato tipoValor) {
        if (tipoVariable == null || tipoValor == null) {
            return false;
        }
        
        // Tipos idénticos son siempre compatibles
        if (tipoVariable == tipoValor) {
            return true;
        }
        
        // Conversiones numéricas permitidas
        if ((tipoVariable == TipoDato.REAL) && 
            (tipoValor == TipoDato.INT || tipoValor == TipoDato.SHORTINT || tipoValor == TipoDato.LONGINT)) {
            return true;
        }
        
        if ((tipoVariable == TipoDato.LONGINT) && 
            (tipoValor == TipoDato.INT || tipoValor == TipoDato.SHORTINT)) {
            return true;
        }
        
        if ((tipoVariable == TipoDato.INT) && (tipoValor == TipoDato.SHORTINT)) {
            return true;
        }
        
        return false;
    }

    /**
     * Valida y asigna un valor a una variable verificando compatibilidad de tipos
     * @param nombreVariable Nombre de la variable
     * @param valor Valor a asignar
     * @param operacion Descripción de la operación
     * @param ambito Ámbito de la variable
     * @param fila Línea donde ocurre la asignación
     * @param columna Columna donde ocurre la asignación
     * @return true si la asignación es válida, false si hay error de tipo
     */
    public boolean validarYAsignarValor(String nombreVariable, Object valor, String operacion, String ambito, int fila, int columna) {
        Variable variable = getVariableEnTabla(nombreVariable, ambito);
        if (variable == null) {
            return false; // Variable no existe - otro tipo de error
        }
        
        TipoDato tipoVariable = variable.getTipoDato();
        TipoDato tipoValor = obtenerTipoValor(valor);
        
        if (!sonTiposCompatibles(tipoVariable, tipoValor)) {
            // Error de tipo - generar error semántico
            return false;
        }
        
        // Tipos compatibles - realizar asignación
        variable.asignarValor(valor, operacion);
        return true;
    }

    /**
     * Define un string con toda la información existente en la tabla de símbolos
     * @return String con la información de todos los símbolos
     */
    public String verTablaSimbolos(){
        String msj = "";
        for(Simbolo s : tablaSimbolos){
            if(s instanceof Variable){
                Variable var = (Variable) s;
                String valor = var.getValor() != null ? var.getValor().toString() : "sin valor";
                
                // Si el valor es "expresión", intentar obtener un valor más descriptivo
                if("expresión".equals(valor) && !var.getSecuenciaDeOperaciones().isEmpty()) {
                    // Buscar en la secuencia de operaciones el valor real asignado
                    List<String> operaciones = var.getSecuenciaDeOperaciones();
                    String ultimaOperacion = operaciones.get(operaciones.size() - 1);
                    
                    // Extraer el valor de la operación (formato: "Operación: valorAnterior -> valorNuevo")
                    if(ultimaOperacion.contains(" -> ")) {
                        String[] partes = ultimaOperacion.split(" -> ");
                        if(partes.length > 1) {
                            valor = partes[1].trim();
                        }
                    }
                }
                
                String secuencia = var.getSecuenciaDeOperaciones().isEmpty() ? 
                                 "sin operaciones" : 
                                 String.join(", ", var.getSecuenciaDeOperaciones());
                
                msj += var.getNombre() + "\t" + 
                       "Variable" + "\t" + 
                       var.getTipoDato() + "\t" + 
                       valor + "\t" + 
                       var.getAmbito() + "\t" + 
                       secuencia + "\n";
            }
            else if(s instanceof Funcion){
                Funcion func = (Funcion) s;
                String tipo = func.getTipoRetorno() == null ? "Procedimiento" : "Función";
                String tipoRetorno = func.getTipoRetorno() == null ? "-" : func.getTipoRetorno().toString();
                
                msj += func.getNombre() + "\t" + 
                       tipo + "\t" + 
                       tipoRetorno + "\t" + 
                       "-" + "\t" + 
                       func.getAmbito() + "\t" + 
                       "Parámetros: " + func.getParametros().size() + "\n";
            }
        }

        return msj;
    }

    /**
     * Evalúa una expresión aritmética entre dos operandos
     */
    public Object evaluarExpresionAritmetica(Object operando1, String operador, Object operando2, String ambito) {
        try {
            // Convertir operandos a números si es posible
            double val1 = convertirANumero(operando1, ambito);
            double val2 = convertirANumero(operando2, ambito);
            
            double resultado;
            switch (operador) {
                case "+":
                    resultado = val1 + val2;
                    break;
                case "-":
                    resultado = val1 - val2;
                    break;
                case "*":
                    resultado = val1 * val2;
                    break;
                case "/":
                case "DIV":
                    if (val2 == 0) {
                        System.err.println("Error: División por cero");
                        return null;
                    }
                    resultado = val1 / val2;
                    break;
                case "MOD":
                    if (val2 == 0) {
                        System.err.println("Error: División por cero en MOD");
                        return null;
                    }
                    resultado = val1 % val2;
                    break;
                default:
                    System.err.println("Operador aritmético no soportado: " + operador);
                    return null;
            }
            
            // Si ambos operandos son enteros y el resultado es entero, devolver entero
            if (esEntero(operando1, ambito) && esEntero(operando2, ambito) && resultado == Math.floor(resultado)) {
                return (int) resultado;
            }
            
            return resultado;
        } catch (Exception e) {
            System.err.println("Error evaluando expresión aritmética: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convierte un valor a número, resolviendo variables si es necesario
     */
    private double convertirANumero(Object valor, String ambito) {
        if (valor instanceof Number) {
            return ((Number) valor).doubleValue();
        }
        
        if (valor instanceof String) {
            String str = valor.toString();
            
            // Intentar convertir directamente si es un número
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                // Si no es un número, buscar como variable
                Variable var = getVariableEnTabla(str, ambito);
                if (var != null && var.getValor() instanceof Number) {
                    return ((Number) var.getValor()).doubleValue();
                }
            }
        }
        
        throw new IllegalArgumentException("No se puede convertir a número: " + valor);
    }
    
    /**
     * Verifica si un valor es un entero
     */
    private boolean esEntero(Object valor, String ambito) {
        if (valor instanceof Integer) {
            return true;
        }
        
        if (valor instanceof String) {
            String str = valor.toString();
            
            // Intentar convertir directamente
            try {
                Integer.parseInt(str);
                return true;
            } catch (NumberFormatException e) {
                // Si no es un número, buscar como variable
                Variable var = getVariableEnTabla(str, ambito);
                if (var != null) {
                    TipoDato tipo = var.getTipoDato();
                    return tipo == TipoDato.INT || tipo == TipoDato.SHORTINT || tipo == TipoDato.LONGINT;
                }
            }
        }
        
        return false;
    }

}

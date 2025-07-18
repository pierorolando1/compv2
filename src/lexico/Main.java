package lexico;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import lib.JavaKeywordsAsyncDemo;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.Subscription;
import semantico.Traductor.Generador;
import vista.ItemTablaErrores;
import vista.ItemTablaSimbolos;
import vista.ItemTablaTokens;
import vista.LineaToken;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java_cup.runtime.*;

import sintactico.*;
import semantico.*;

public class Main extends Application implements Cloneable  {

    public static Main miInstancia;
    @FXML public TextArea ta_insertar_texto_id;
    @FXML public TableView<ItemTablaTokens> tv_tokens_encontrados_id;
    @FXML public TableColumn<ItemTablaTokens, String> tc_token_id, tc_tipo_token_id, tc_linea_token_id;
    @FXML public TableView<ItemTablaErrores> tv_errores_lexicos_id;
    @FXML public TableColumn<ItemTablaErrores, String> tc_error_id, tc_linea_error_id, tc_tipo_error_id;
    @FXML public TableView<ItemTablaSimbolos> tv_tabla_simbolos_id;
    @FXML public TableColumn<ItemTablaSimbolos, String> tc_ts_nombre, tc_ts_tipo, tc_ts_valor, tc_ts_alcance, tc_ts_secuencia;
    private final ObservableList<ItemTablaTokens> info_tabla_tokens = FXCollections.observableArrayList();
    private final ObservableList<ItemTablaErrores> info_tabla_errores = FXCollections.observableArrayList();
    private final ObservableList<ItemTablaSimbolos> info_tabla_simbolos = FXCollections.observableArrayList();

    @FXML public Button btn_abrir_archivo_id, btn_procesar_id;
    @FXML public MenuItem menu_abrir, menu_guardar, menu_guardar_como, menu_exportar_pdf;

    @FXML private TextArea ta_errores_sintacticos_id;
    @FXML private TextArea ta_errores_semanticos_id;
    //@FXML private TextArea ta_tabla_simbolos_id;
    //@FXML private TextArea ta_codigo_ensamblador_id;

    @FXML public CodeArea ca_insertar_texto_id;
    private ExecutorService executor;

    private List<LineaToken> tokenslist, tokenslistErrores;

    private Stage miPrimaryStage;
    private File currentFile; // Para guardar el archivo actual

    private static final String[] KEYWORDS = new ArrayList<String>(){
        {
            addAll(Arrays.asList(sym.terminalNames));
            addAll(Arrays.asList(arrayToLower(sym.terminalNames)));
            addAll(Arrays.asList( new String[]{ "CK", "ck" }  ));
        }}.toArray(new String[0]);


    @Override
    public void start(Stage primaryStage) throws Exception
    {
        miPrimaryStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("../vista/GUI.fxml"));
        miPrimaryStage.setTitle("Analizador Léxico y Sintáctico");
        Scene miScene = new Scene(root);
        //miScene.getStylesheets().add(Main.class.getResource("tokyo-night.css").toExternalForm());
        miScene.getStylesheets().add(Main.class.getResource("java-keywords.css").toExternalForm());
        miPrimaryStage.setScene(miScene);
        miPrimaryStage.show();
    }

    @Override
    public void stop() {
        executor.shutdown();
    }

    public static void main(String[] args)
    {
        Path pathActual = Paths.get("");
        String pathRaiz = pathActual.toAbsolutePath().toString();

        generarLexer(pathRaiz);
        generarSyntax(pathRaiz);

        launch(args);
    }

    public static void generarLexer(String pPathRaiz)
    {
        String path = pPathRaiz + "/src/lexico/Lexer.flex";

        File file = new File(path);
        jflex.Main.generate(file);
    }

    private static void generarSyntax(String pPathRaiz)
    {
        String path = pPathRaiz + "/src/sintactico/Syntax.cup";

        String[] asintactico = {"-parser", "Syntax",
                                "-destdir", pPathRaiz + "/src/sintactico/",
                                "-symbols", "sym",
                                path};

        try { java_cup.Main.main(asintactico); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public void probarLexerFile()
    {
        reestablecerComponentes();

        File fichero = new File ("fichero.txt");
        PrintWriter writer;

        try {
            writer = new PrintWriter(fichero);
            // writer.print(ta_insertar_texto_id.getText()); // .toUpperCase();
            writer.print(ca_insertar_texto_id.getText());
            writer.close();
        }
        catch (FileNotFoundException ex) {
            // Logger.getLogger(interfaz.class.getName()).log(Level.SEVERE, null, ex);
        }

        // 2 reader's para solucionar problemas con las referencias de los objetos, porque el lexico itera el lexeme y
        // jala tambien la referencia del reader y desmadra los objetos para cuando el sintactico realiza el analisis
        Reader readerLexico = null, readerSintactico = null;
        try {
            readerLexico = new BufferedReader(new FileReader("fichero.txt"));
            readerSintactico = new BufferedReader(new FileReader("fichero.txt"));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // reader = new BufferedReader(new StringReader(ta_insertar_texto_id.getText()));

        ejecutarAnalizadorLexico(readerLexico, readerSintactico);
    }


    private void ejecutarAnalizadorLexico(Reader readerLexico, Reader readerSintactico)
    {
        Lexer lexerLexico = new Lexer(readerLexico);
        Lexer lexerSintactico = new Lexer(readerSintactico);

        while (true)
        {
            Symbol token = null;
            try
            {
                token = lexerLexico.next_token();
            }
            catch (IOException e) { }

            if (token == null || token.value == null)
            {
                agregarElementosTablaTokens();
                agregarElementosTablaTokensErrores();
                break; // return;
            }

            if ((token.sym != sym.error) && (token.sym != sym.ERROR_LITERAL) && (token.sym != sym.ERROR_IDENTIFICADOR
                    && (token.sym != sym.ERROR_OPERADOR) && (token.sym != sym.ERROR_PALABRA_RESERVADA)))
            {
                agregarLineaToken(token.value.toString(), sym.terminalNames[token.sym], token.left);
            }
            else
            {
                agregarLineaTokenErrores(token.value.toString(), sym.terminalNames[token.sym], token.left);
            }
        }
        ejecutarAnalizadorSintactico(lexerSintactico);
    }

    private void ejecutarAnalizadorSintactico(Lexer lexer)
    {
        try {
            sintactico.Syntax parser = new sintactico.Syntax(lexer);
            parser.parse();
            //ta_errores_sintacticos_id.setText(parser.getErrors());
            //ta_errores_semanticos_id.setText(parser.getSemanticErrors());

            info_tabla_simbolos.clear();
            for (Simbolo simbolo : TablaSimbolos.tablaSimbolos) {
                String nombre = simbolo.getNombre();
                String tipo = "";
                String valor = simbolo.getValor() != null ? simbolo.getValor().toString() : "";
                String alcance = "";
                String secuencia = String.join(", ", simbolo.getSecuenciaDeOperaciones());
                
                //print secuencia
                System.out.println("Secuencia de operaciones para " + nombre + ": " + secuencia);

                if (simbolo instanceof Variable) {
                    Variable var = (Variable) simbolo;
                    if (var.getTipoDato() != null) {
                        tipo = var.getTipoDato().name().toLowerCase();
                    }
                    alcance = var.getAmbito();
                } else if (simbolo instanceof Funcion) {
                    Funcion fun = (Funcion) simbolo;
                    tipo = fun.toString();
                    alcance = "global";
                }

                info_tabla_simbolos.add(new ItemTablaSimbolos(nombre, tipo, valor, alcance, secuencia));
            }

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    @FXML
    void initialize(){
        initTablesViews();
        initCodeArea();

        miInstancia = this;
    }

    private void initCodeArea()
    {
        executor = Executors.newSingleThreadExecutor();
        ca_insertar_texto_id.setParagraphGraphicFactory(LineNumberFactory.get(ca_insertar_texto_id));
        Subscription cleanupWhenDone = ca_insertar_texto_id.multiPlainChanges()
                .successionEnds(Duration.ofMillis(500))
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(ca_insertar_texto_id.multiPlainChanges())
                .filterMap(t -> {
                    if(t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(this::applyHighlighting);

        // modificar los espacios del tab, porque los hace muy grandes
        InputMap<KeyEvent> im = InputMap.consume(
                EventPattern.keyPressed(KeyCode.TAB),
                e -> {
                    ca_insertar_texto_id.replaceSelection(miTab);
                    e.consume();
                });

        Nodes.addInputMap(ca_insertar_texto_id, im);
        // call when no longer need it: `cleanupWhenFinished.unsubscribe();`

        //ca_insertar_texto_id.replaceText(0, 0, sampleCode);
    }

    /**
     * Inicializar los valores de las tablas asi como sus columnas
     */
    private void initTablesViews()
    {
        tc_token_id.setCellValueFactory(new PropertyValueFactory<>("token"));
        tc_tipo_token_id.setCellValueFactory(new PropertyValueFactory<>("tipoToken"));
        tc_linea_token_id.setCellValueFactory(new PropertyValueFactory<>("linea"));
        tv_tokens_encontrados_id.setItems(info_tabla_tokens);

        tc_error_id.setCellValueFactory(new PropertyValueFactory<>("token"));
        tc_tipo_error_id.setCellValueFactory(new PropertyValueFactory<>("tipoError"));
        tc_linea_error_id.setCellValueFactory(new PropertyValueFactory<>("linea"));
        tv_errores_lexicos_id.setItems(info_tabla_errores);

        tc_ts_nombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        tc_ts_tipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        tc_ts_valor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        tc_ts_alcance.setCellValueFactory(new PropertyValueFactory<>("alcance"));
        tc_ts_secuencia.setCellValueFactory(new PropertyValueFactory<>("secuenciaDeOperaciones"));
        tv_tabla_simbolos_id.setItems(info_tabla_simbolos);
    }

    /**
     * Agrega una nueva linea al tokenlist
     * Si ya existe el token llama a agregarLinea(int), sino crea una nueva Linea de Token con un nuevo HashMap
     * @param token token analizado
     * @param tipoToken tipo del token analizado
     * @param numeroLinea número de línea de aparición del token analizado
     */
    private void agregarLineaToken(String token, String tipoToken, int numeroLinea){
        LineaToken linea = null;
        boolean existe = false;
        for(int i=0; i < tokenslist.size(); i++)
        {
            linea = tokenslist.get(i);
            if(linea.token.equalsIgnoreCase(token))
            {
                existe = true;
                break;
            }
        }
        if(existe){
            linea.agregarLinea(numeroLinea);
        }else
        {
            Map<Integer, Integer> lineasAparicion = new HashMap<Integer, Integer>();
            lineasAparicion.put(numeroLinea, 1);
            tokenslist.add(new LineaToken(token, tipoToken, lineasAparicion));
        }
    }

    /**
     * Encargado de agregar valores a la tabla de tokens de la interfaz
     * Utiliza el LinkedList tokenlist que posee todas las lineas de código resumidas por apariciones del token
     */
    private void agregarElementosTablaTokens()
    {
        String lineas;
        for(LineaToken l : tokenslist)
        {
            lineas = "";
            Set<Integer> clavesLineas = l.lineasAparicion.keySet();     //retorna el set de claves del map
            for (Iterator<Integer> it = clavesLineas.iterator(); it.hasNext();)
            {
                Integer key = it.next();
                int cantidadApariciones = l.lineasAparicion.get(key);

                if(cantidadApariciones > 1)
                {
                    lineas += (key + 1) + "(" + l.lineasAparicion.get(key) + "), ";
                }else
                    {
                    lineas += (key + 1) + ", ";
                }
            }
            info_tabla_tokens.add(new ItemTablaTokens(new SimpleStringProperty(l.token),
                    new SimpleStringProperty(l.tipoToken), new SimpleStringProperty(lineas)));
        }
    }

    /**
     * Agrega una nueva linea al tokenlistErrrores
     * Si ya existe el token llama a agregarLinea(int), sino crea una nueva Linea de Token con un nuevo HashMap
     * @param token token analizado
     * @param numeroLinea número de línea de aparición del token analizado
     */
    private void agregarLineaTokenErrores(String token, String tipoError, int numeroLinea){
        LineaToken linea = null;
        boolean existe = false;
        for(int i=0; i< tokenslistErrores.size(); i++){
            linea = tokenslistErrores.get(i);
            if(linea.token.equalsIgnoreCase(token)){
                existe = true;
                break;
            }
        }
        if(existe){
            linea.agregarLinea(numeroLinea);
        }else{
            Map<Integer, Integer> lineasAparicion = new HashMap<Integer, Integer>();
            lineasAparicion.put(numeroLinea, 1);
            tokenslistErrores.add(new LineaToken(token, tipoError, lineasAparicion));
        }
    }

    /**
     * Encargado de agregar valores a la tabla de tokens de errores de la interfaz
     * Utiliza el LinkedList tokenlist que posee todas las lineas de código resumidas por apariciones del token
     */
    private void agregarElementosTablaTokensErrores()
    {
        String lineas;
        for(LineaToken l : tokenslistErrores){
            lineas = "";
            Set<Integer> clavesLineas = l.lineasAparicion.keySet();     //retorna el set de claves del map
            for (Iterator<Integer> it = clavesLineas.iterator(); it.hasNext(); ) {
                Integer key = it.next();
                int cantidadApariciones = l.lineasAparicion.get(key);

                if(cantidadApariciones > 1){
                    lineas += (key + 1) + "(" + l.lineasAparicion.get(key) + "), ";
                }else{
                    lineas += (key + 1) + ", ";
                }
            }
            info_tabla_errores.add(new ItemTablaErrores(new SimpleStringProperty(l.token), new SimpleStringProperty(l.tipoToken), new SimpleStringProperty(lineas)));
        }
    }

    /**
     * Reestablece y limpia los valores asociados a los componentes graficos
     */
    private void reestablecerComponentes()
    {
        tokenslist = new LinkedList<LineaToken>();
        tokenslistErrores = new LinkedList<LineaToken>();
        info_tabla_tokens.clear();
        info_tabla_errores.clear();
        info_tabla_simbolos.clear();
        ta_errores_sintacticos_id.clear();
        ta_errores_semanticos_id.clear();
        //ta_tabla_simbolos_id.clear();
        //ta_codigo_ensamblador_id.clear();
        Generador.contadorEtiq = 1;
        Generador.etiqAnterior = "L0";
        EliminarArchivoASM();
    }

    public void EliminarArchivoASM()
    {
        File file = new File("src/semantico/Traductor/Traduccion.asm");
        if(file.delete()) { /* Eliminado */}
        else { /* No se pudo eliminar (no existe) */ }
        //ta_codigo_ensamblador_id.clear();
    }

    /**
     * Encargado de abrir el FileChooser para seleccionar archivos .nola
     */
    public void btn_action_abrirArchivo() {
        FileChooser fileChooser = new FileChooser();

        //Set extension filters
        FileChooser.ExtensionFilter nolaFilter = new FileChooser.ExtensionFilter("NOLA files (*.nola)", "*.nola");
        FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*");
        
        fileChooser.getExtensionFilters().addAll(nolaFilter, allFilesFilter);
        fileChooser.setSelectedExtensionFilter(nolaFilter); // Set NOLA files as default

        //Show save file dialog
        File file = fileChooser.showOpenDialog(miPrimaryStage);
        //File file = new File("fichero.txt");
        if(file != null){
            // ta_insertar_texto_id.setText(readFile(file));
            ca_insertar_texto_id.replaceText(readFile(file));
            currentFile = file; // Guardar referencia del archivo actual
            miPrimaryStage.setTitle("Analizador Léxico y Sintáctico - " + file.getName());
        }
    }

    /**
     * Encargado de leer el archivo txt en memoria
     * @param file
     * @return
     */
    private String readFile(File file){
        StringBuilder stringBuffer = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {

            bufferedReader = new BufferedReader(new FileReader(file));

            String text;
            while ((text = bufferedReader.readLine()) != null) {
                stringBuffer.append(text.replaceAll("\t", miTab) + "\n");
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return stringBuffer.toString();
    }

    private void imprimir(String pMsg) { System.out.println(pMsg); }

    private void imprimir(int pMsg) { System.out.println(pMsg); }

    private static String[] arrayToLower(String[] pArray)
    {
        List<String> list = Arrays.asList(pArray);
        list.replaceAll(String::toLowerCase);

        return list.toArray(new String[list.size()]);
    }

    public void agregarErrorSintactico(String pError)
    {
        ta_errores_sintacticos_id.appendText(pError + "\n");
    }

    public void agregarErrorSemantico(String pError) { ta_errores_semanticos_id.appendText(pError + "\n" ); }
public void mostrarTablaSimbolos(String texto) {
                if (texto == null || texto.trim().isEmpty()) {
                    System.out.println("Debug: mostrarTablaSimbolos fue llamado con texto vacío o nulo.");
                    return;
                }

                Pattern pattern = Pattern.compile("Variable:\\s*(\\w+),\\s*tipo:\\s*(\\w+)");

                String[] simbolos = texto.split("\n");
                for (String s : simbolos) {
                    String linea = s.trim();
                    if (linea.isEmpty()) {
                        continue;
                    }

                    String[] campos = linea.split("\t");
                    if (campos.length >= 5) {
                        // Join all fields from index 4 onwards as the sequence field
                        String secuencia = "";
                        if (campos.length > 5) {
                            StringBuilder secBuilder = new StringBuilder();
                            for (int i = 5; i < campos.length; i++) {
                                if (i > 5) secBuilder.append("\t");
                                secBuilder.append(campos[i]);
                            }
                            // Parse the sequence to extract the flow: "null -> 1 -> 2"
                            String fullSequence = campos[4] + "\t" + secBuilder.toString();
                            secuencia = parseSequenceFlow(fullSequence);
                        } else {
                            secuencia = parseSequenceFlow(campos[4]);
                        }

                        info_tabla_simbolos.add(new ItemTablaSimbolos(campos[0], campos[1], campos[2], campos[3], secuencia));
                    } else {
                        // Fallback for the format "Variable: [name], tipo: [type]"
                        Matcher matcher = pattern.matcher(linea);
                        if (matcher.matches()) {
                            String nombre = matcher.group(1);
                            String tipoDato = matcher.group(2);
                            // Assuming default values for other fields
                            info_tabla_simbolos.add(new ItemTablaSimbolos(nombre, "Variable", tipoDato, "", ""));
                        } else {
                            System.out.println("Debug: Línea de tabla de símbolos mal formada: " + linea);
                        }
                    }
                }
            }

            private String parseSequenceFlow(String sequence) {
                // Extract values from assignments like "Asignación := 1: null -> 1, Asignación := 2: 1 -> 2"
                Pattern assignPattern = Pattern.compile("\\d+:\\s*([^,]+)");
                Matcher matcher = assignPattern.matcher(sequence);

                StringBuilder flow = new StringBuilder();
                while (matcher.find()) {
                    String assignment = matcher.group(1).trim();
                    if (flow.length() > 0) {
                        // Extract the final value from previous assignment and initial value from current
                        String[] parts = assignment.split("\\s*->\\s*");
                        if (parts.length == 2) {
                            flow.append(" -> ").append(parts[1]);
                        }
                    } else {
                        // First assignment, include both parts
                        flow.append(assignment);
                    }
                }

                return flow.toString();
            }

    public void agregarCodigoEnsamblador(String pCodigo)
    {
        //ta_codigo_ensamblador_id.appendText(pCodigo + "\n" );

        try
        {
            BufferedWriter out = new BufferedWriter(new FileWriter("src/semantico/Traductor/Traduccion.asm", true));
            pCodigo += "\n";
            out.write(pCodigo);
            out.close();
        }
        catch (IOException e) { e.printStackTrace(); }

    }

    // ============================ CodeArea ============================ \\

    private String miTab = "    ";

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String text = ca_insertar_texto_id.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        executor.execute(task);
        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        ca_insertar_texto_id.setStyleSpans(0, highlighting);
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("BRACE") != null ? "brace" :
                                            matcher.group("BRACKET") != null ? "bracket" :
                                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                                            matcher.group("STRING") != null ? "string" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            "normaltext"; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    // ================== MÉTODOS DEL MENÚ ================== //

    /**
     * Método para abrir archivo desde el menú
     */
    @FXML
    public void menu_action_abrir() {
        btn_action_abrirArchivo(); // Reutiliza la funcionalidad existente
    }

    /**
     * Método para guardar archivo
     */
    @FXML
    public void menu_action_guardar() {
        if (currentFile != null) {
            guardarArchivo(currentFile);
        } else {
            menu_action_guardar_como();
        }
    }

    /**
     * Método para guardar archivo como
     */
    @FXML
    public void menu_action_guardar_como() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar archivo como");
        
        // Establecer filtros de extensión
        FileChooser.ExtensionFilter nolaFilter = new FileChooser.ExtensionFilter("NOLA files (*.nola)", "*.nola");
        FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*");
        
        fileChooser.getExtensionFilters().addAll(nolaFilter, allFilesFilter);
        fileChooser.setSelectedExtensionFilter(nolaFilter); // Set NOLA files as default for saving
        
        // Mostrar diálogo de guardar
        File file = fileChooser.showSaveDialog(miPrimaryStage);
        
        if (file != null) {
            // Only add .nola extension if no extension is provided and NOLA filter is selected
            if (!file.getName().contains(".") && fileChooser.getSelectedExtensionFilter() == nolaFilter) {
                file = new File(file.getAbsolutePath() + ".nola");
            }
            guardarArchivo(file);
            currentFile = file;
            miPrimaryStage.setTitle("Analizador Léxico y Sintáctico - " + file.getName());
        }
    }

    /**
     * Método para exportar todo a PDF
     */
    @FXML
    public void menu_action_exportar_pdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar reporte como PDF");
        
        // Establecer filtros de extensión
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
        fileChooser.getExtensionFilters().add(extFilter);
        
        // Sugerir nombre de archivo con fecha y hora
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        fileChooser.setInitialFileName("Reporte_Analisis_" + fechaHora + ".pdf");
        
        // Mostrar diálogo de guardar
        File file = fileChooser.showSaveDialog(miPrimaryStage);
        
        if (file != null) {
            // Asegurar que el archivo tenga la extensión .pdf
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            exportarAPDF(file);
        }
    }

    /**
     * Guarda el contenido del CodeArea en un archivo
     */
    private void guardarArchivo(File file) {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.print(ca_insertar_texto_id.getText());
            
            // Mostrar mensaje de confirmación
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Guardado exitoso");
            alert.setHeaderText(null);
            alert.setContentText("El archivo se ha guardado correctamente.");
            alert.showAndWait();
            
        } catch (FileNotFoundException e) {
            // Mostrar mensaje de error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al guardar");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo guardar el archivo: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Exporta un reporte completo del análisis a PDF
     */
    private void exportarAPDF(File file) {
        try {
            // Crear un HTML temporal con todo el contenido
            String htmlContent = generarReporteHTML();
            
            // Crear archivo HTML temporal
            File tempHtml = new File("temp_reporte.html");
            try (PrintWriter writer = new PrintWriter(tempHtml)) {
                writer.print(htmlContent);
            }
            
            // Mostrar mensaje informativo sobre la generación del reporte
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Reporte generado");
            alert.setHeaderText(null);
            alert.setContentText("Se ha generado el archivo HTML temporal: " + tempHtml.getAbsolutePath() + 
                                "\n\nPara convertir a PDF, puede usar herramientas como:\n" +
                                "- Imprimir como PDF desde un navegador web\n" +
                                "- Usar herramientas online de conversión HTML a PDF\n" +
                                "- Usar herramientas de línea de comandos como wkhtmltopdf");
            alert.showAndWait();
            
            // Intentar abrir el archivo HTML en el navegador por defecto
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempHtml);
            }
            
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al exportar");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo generar el reporte: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Genera el contenido HTML del reporte completo
     */
    private String generarReporteHTML() {
        StringBuilder html = new StringBuilder();
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>Reporte de Análisis Léxico y Sintáctico</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("h1 { color: #333; border-bottom: 2px solid #333; }\n");
        html.append("h2 { color: #666; margin-top: 30px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 10px 0; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f4f4f4; }\n");
        html.append("pre { background-color: #f8f8f8; padding: 10px; border: 1px solid #ddd; overflow-x: auto; }\n");
        html.append(".codigo { font-family: 'Courier New', monospace; font-size: 12px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        // Título del reporte
        html.append("<h1>Reporte de Análisis Léxico y Sintáctico</h1>\n");
        html.append("<p><strong>Fecha y hora de generación:</strong> ").append(fechaHora).append("</p>\n");
        if (currentFile != null) {
            html.append("<p><strong>Archivo analizado:</strong> ").append(currentFile.getName()).append("</p>\n");
        }
        
        // Código fuente
        html.append("<h2>Código Fuente</h2>\n");
        html.append("<pre class='codigo'>").append(escaparHTML(ca_insertar_texto_id.getText())).append("</pre>\n");
        
        // Tokens encontrados
        html.append("<h2>Tokens Encontrados</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Token</th><th>Tipo de Token</th><th>Línea</th></tr>\n");
        for (ItemTablaTokens item : info_tabla_tokens) {
            html.append("<tr>");
            html.append("<td>").append(escaparHTML(item.getToken())).append("</td>");
            html.append("<td>").append(escaparHTML(item.getTipoToken())).append("</td>");
            html.append("<td>").append(escaparHTML(item.getLinea())).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>\n");
        
        // Errores léxicos
        html.append("<h2>Errores Léxicos</h2>\n");
        if (info_tabla_errores.isEmpty()) {
            html.append("<p>No se encontraron errores léxicos.</p>\n");
        } else {
            html.append("<table>\n");
            html.append("<tr><th>Error</th><th>Tipo de Error</th><th>Línea</th></tr>\n");
            for (ItemTablaErrores item : info_tabla_errores) {
                html.append("<tr>");
                html.append("<td>").append(escaparHTML(item.getError())).append("</td>");
                html.append("<td>").append(escaparHTML(item.getTipoError())).append("</td>");
                html.append("<td>").append(escaparHTML(item.getLinea_error())).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }
        
        // Errores sintácticos
        html.append("<h2>Errores Sintácticos</h2>\n");
        String erroresSintacticos = ta_errores_sintacticos_id.getText();
        if (erroresSintacticos == null || erroresSintacticos.trim().isEmpty()) {
            html.append("<p>No se encontraron errores sintácticos.</p>\n");
        } else {
            html.append("<pre>").append(escaparHTML(erroresSintacticos)).append("</pre>\n");
        }
        
        // Errores semánticos
        html.append("<h2>Errores Semánticos</h2>\n");
        String erroresSemanticos = ta_errores_semanticos_id.getText();
        if (erroresSemanticos == null || erroresSemanticos.trim().isEmpty()) {
            html.append("<p>No se encontraron errores semánticos.</p>\n");
        } else {
            html.append("<pre>").append(escaparHTML(erroresSemanticos)).append("</pre>\n");
        }
        
        // Tabla de símbolos
        html.append("<h2>Tabla de Símbolos</h2>\n");
        if (info_tabla_simbolos.isEmpty()) {
            html.append("<p>La tabla de símbolos está vacía.</p>\n");
        } else {
            html.append("<table>\n");
            html.append("<tr><th>Nombre</th><th>Tipo</th><th>Valor</th><th>Alcance</th><th>Secuencia de Operaciones</th></tr>\n");
            for (ItemTablaSimbolos item : info_tabla_simbolos) {
                html.append("<tr>");
                html.append("<td>").append(escaparHTML(item.getNombre())).append("</td>");
                html.append("<td>").append(escaparHTML(item.getTipo())).append("</td>");
                html.append("<td>").append(escaparHTML(item.getValor())).append("</td>");
                html.append("<td>").append(escaparHTML(item.getAlcance())).append("</td>");
                html.append("<td>").append(escaparHTML(item.getSecuenciaDeOperaciones())).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</table>\n");
        }
        
        html.append("</body>\n</html>");
        return html.toString();
    }

    /**
     * Escapa caracteres HTML para evitar problemas de formato
     */
    private String escaparHTML(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}

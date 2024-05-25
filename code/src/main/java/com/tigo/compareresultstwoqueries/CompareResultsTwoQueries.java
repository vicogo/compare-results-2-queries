package com.tigo.compareresultstwoqueries;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Compare Results Two Queries
 *
 * Para ejecutar consultas...
 *
 * @author Victor Hugo Gonzales
 */
public class CompareResultsTwoQueries {

    private static final String LOG_PROPERTIES_FILENAME = "log.properties";

    static {
        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(LOG_PROPERTIES_FILENAME));
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CompareResultsTwoQueries.class.getName());
    private static final String CONFIG_PROPERTIES_FILENAME = "config.properties";
    private static final String LOG_FILENAME = "compare-results.log";
    private static int MAX_RESULT_SET_LENGTH;
    private static String STRING_SEPARATOR;
    private static String STACK_TRACE_FILTERED;
    //Get data from properties using args parameters 
    //[0]->database reference one; 
    //[1]->database reference two; 
    //[2]->Query Template1; 
    //[3]->Query Template2; 
    //[4]->table name 
    //[5]->sql condition
    private static String url;
    private static String user;
    private static String password;
    private static String queryTemplate;
    private static String query;
    private static String url2;
    private static String user2;
    private static String password2;
    private static String queryTemplate2;
    private static String query2;

    static {
        //LOGGER.setUseParentHandlers(false);//para deshabilitar el log en consola
        try {
            File f = new File(LOG_FILENAME);
            FileHandler fh = new FileHandler(f.getAbsolutePath(), Boolean.TRUE);
            fh.setFormatter(new MyFormatter());
            fh.setFilter(new MyFilter());
            LOGGER.addHandler(fh);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Error al leer archivo:\n%s", getStackTrace(e)));
        }
    }

    /**
     *
     * @param args[]: [0]->database reference one [1]->Query Template1
     * [2]->table name1 (optional) [3]->sql condition1 (optional)
     *
     * [4]->database reference two [5]->Query Template2 [6]->table name2
     * (optional) [7]->sql condition2 (optional) [8]->Aceptable difference
     *
     *
     */
    public static void main(String[] args) {
        String databaseRef = args[0];
        String queryTemplateRef = args[1];
        String tableName = args[2];
        String sqlCondition = args[3];
        //
        String databaseRef2 = args[4];
        String queryTemplateRef2 = args[5];
        String tableName2 = args[6];
        String sqlCondition2 = args[7];
        //
        String acceptableDifferenceStr = args[8];
        int acceptableDifference = Integer.parseInt(acceptableDifferenceStr);
        setVarsUsingConfigProperties(databaseRef, queryTemplateRef, tableName, sqlCondition, databaseRef2, queryTemplateRef2, tableName2, sqlCondition2);
        compareQueryResults(databaseRef, databaseRef2, acceptableDifference);
    }

    /**
     *
     * @param databaseRef
     * @param databaseRef2
     * @param acceptableDifference
     */
    public static void compareQueryResults(String databaseRef, String databaseRef2, int acceptableDifference) {
        LOGGER.log(Level.INFO, String.format("%s-%s::****INICIO****", databaseRef, databaseRef2));
        String[] result = runQuery(databaseRef, url, user, password, query);
        String[] result2 = runQuery(databaseRef2, url2, user2, password2, query2);
        String comparisonResult;
        //comparar el campo result
        float difference = 0;
        boolean is_numeric_comparisson = false;
        if (result[0] != null && !"".equals(result[0]) && result2[0] != null && !"".equals(result2[0])) {
            //revisar si los valores son numericos, si es asi se debe comparar como numeros
            try {
                //en caso de que el result sea un numero entero
                float valor1 = Float.parseFloat(result[0]);
                float valor2 = Float.parseFloat(result2[0]);
                difference = Math.abs(valor1 - valor2);
                if (difference == 0.00) {
                    comparisonResult = "IGUAL";
                } else if (difference <= acceptableDifference) {
                    comparisonResult = "DIFERENCIA_ACEPTABLE";
                } else {
                    comparisonResult = "DIFERENTE";
                }
                is_numeric_comparisson = true;
            } catch (NumberFormatException n) {
                //en caso de que el result sea un string
                if (!"".equals(result[0]) && !"".equals(result2[0])) {
                    if (result[0].equals(result2[0])) {
                        comparisonResult = "IGUAL";
                    } else {
                        comparisonResult = "DIFERENTE";
                    }
                } else {
                    comparisonResult = "NO_DETERMINADO";
                }
            }
        } else {
            comparisonResult = "NO_DETERMINADO";
            LOGGER.log(Level.INFO, String.format("%s-%s::Una o mas consultas SQL no devuelven resultados.", databaseRef, databaseRef2));
        }
        //IMPRIMIR RESULTADOS
        System.out.println(comparisonResult);
        LOGGER.log(Level.INFO, String.format("%s-%s::Resultado comparacion: %s (%s)", databaseRef, databaseRef2, comparisonResult, (is_numeric_comparisson ? String.format("%.2f", difference) : "")));
        //
        if (result[0] != null && !"".equals(result[0])) {
            printQueryData(databaseRef, query, result);
        }
        if (result2[0] != null && !"".equals(result2[0])) {
            printQueryData(databaseRef2, query2, result2);
        }

    }

    /**
     * Execute query method
     *
     * @param databaseRef: database reference. Must be in config.properties file
     * @param url
     * @param user
     * @param password
     * @param query
     * @return
     *
     */
    public static String[] runQuery(String databaseRef, String url, String user, String password, String query) {
        String[] finalResult = new String[3];
        long startTime = System.currentTimeMillis();
        long endTime;
        long executionTime;
        // Connect to DB and execute Query
        try (Connection conn = DriverManager.getConnection(url, user, password); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            endTime = System.currentTimeMillis();
            executionTime = endTime - startTime;
            finalResult = getResult(rs, databaseRef);
            finalResult[2] = Long.toString(executionTime);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, String.format("%s::Error al conectarse a la base de datos (%s) o al ejecutar query (%s):\n%s", databaseRef, url, query, getStackTrace(e)));
            endTime = System.currentTimeMillis();
            executionTime = endTime - startTime;
            finalResult[0] = "";
            finalResult[1] = "0";
            finalResult[2] = Long.toString(executionTime);
        }
        return finalResult;
    }

    /**
     * filterStackTrace
     *
     * @param e: database reference. Must be in config.properties file
     * @param filterCriteria
     *
     */
    public static void filterStackTrace (Throwable e, String filterCriteria){
        // Filtrar el stacktrace
        StackTraceElement[] stackTrace = e.getStackTrace();
        List<StackTraceElement> filteredStackTrace = new ArrayList<>();
        // Iterar sobre el stacktrace y agregar solo las líneas que corresponden al criterio deseado
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith(filterCriteria)) {
                filteredStackTrace.add(element);
            }
        }
        // Establecer el nuevo stacktrace filtrado
        e.setStackTrace(filteredStackTrace.toArray(new StackTraceElement[0]));
    }
    
    /**
     * stackTrace TO String,
     *
     * @param e: Excepcion de la que queremos el StackTrace
     * @return StackTrace: de la excepcion en forma de String
     *
     */
    public static String getStackTrace(Exception e) {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter(sWriter);
        // Filtrar el stacktrace para mostarar las filas correspondientes al paquete
        if (STACK_TRACE_FILTERED.compareToIgnoreCase("1") == 0) {
            filterStackTrace (e, "com.tigo");
            filterStackTrace (e.getCause(), "com.tigo");
        }

        e.printStackTrace(pWriter);
        return sWriter.toString().trim();
    }

    /**
     *
     * @param rs
     * @param databaseRef
     * @return
     * @throws SQLException
     */
    public static String[] getResult(ResultSet rs, String databaseRef) throws SQLException {
        String[] result = new String[3];
        ResultSetMetaData rsmd = rs.getMetaData();
        int column_count = rsmd.getColumnCount();
        int rs_counter = 0;
        StringBuilder strRow = new StringBuilder();
        while (rs.next()) {
            if (rs_counter < MAX_RESULT_SET_LENGTH) {
                strRow = strRow.append((rs_counter > 0 ? "\n" : ""));
                for (int colIndex = 1; colIndex <= column_count; colIndex++) {
                    strRow = strRow.append((colIndex > 1 ? STRING_SEPARATOR : ""));
                    strRow = strRow.append(rs.getString(colIndex));
                }
            }
            rs_counter++;
        }
        result[0] = strRow.toString().trim();
        result[1] = String.valueOf(rs_counter);
        return result;
    }

    /**
     *
     * @param databaseRef
     * @param query
     * @param result
     */
    public static void printQueryData(String databaseRef, String query, String[] result) {
        LOGGER.log(Level.INFO, String.format("%s::Query ejecutado: %s", databaseRef, query));
        LOGGER.log(Level.INFO, String.format("%s::Tiempo de ejecución: %s ms", databaseRef, result[2]));
        if (Integer.parseInt(result[1]) > MAX_RESULT_SET_LENGTH) {
            LOGGER.log(Level.INFO, String.format("%s::Cantidad de registros mayor al permitido. Solo se mostrara los primeros (%s) registros", databaseRef, MAX_RESULT_SET_LENGTH));
        }
        LOGGER.log(Level.INFO, String.format("%s::Cantidad total de registros: %s", databaseRef, result[1]));
        LOGGER.log(Level.INFO, String.format("%s::Resultado: %s", databaseRef, result[0]));
    }

    /**
     * Read data from properties file based on parameters and set all necesary
     * variables
     *
     * @param databaseRef: Database reference
     * @param queryTemplateRef: Query template to use. Is defined in
     * CONFIG_PROPERTIES_FILENAME file
     * @param databaseRef2: Second Database reference
     * @param queryTemplateRef2: Second query template to use. Is defined in
     * CONFIG_PROPERTIES_FILENAME file
     * @param tableName: Table name
     * @param sqlCondition: SQL condition
     * @param tableName2
     * @param sqlCondition2
     */
    public static void setVarsUsingConfigProperties(String databaseRef, String queryTemplateRef, String tableName, String sqlCondition,
            String databaseRef2, String queryTemplateRef2, String tableName2, String sqlCondition2) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(CONFIG_PROPERTIES_FILENAME));
            url = props.getProperty(databaseRef + ".db.url");
            user = props.getProperty(databaseRef + ".db.user");
            password = props.getProperty(databaseRef + ".db.password");
            queryTemplate = props.getProperty("query." + queryTemplateRef);
            query = String.format(queryTemplate, tableName, sqlCondition);
            //
            url2 = props.getProperty(databaseRef2 + ".db.url");
            user2 = props.getProperty(databaseRef2 + ".db.user");
            password2 = props.getProperty(databaseRef2 + ".db.password");
            queryTemplate2 = props.getProperty("query." + queryTemplateRef2);
            query2 = String.format(queryTemplate2, tableName2, sqlCondition2);
            //
            MAX_RESULT_SET_LENGTH = Integer.parseInt(props.getProperty("MAX_RESULT_SET_LENGTH"));
            STRING_SEPARATOR = props.getProperty("STRING_SEPARATOR");
            String console_logging = props.getProperty("CONSOLE_LOGGING");
            if (console_logging.compareToIgnoreCase("1") != 0) {
                LOGGER.setUseParentHandlers(false);//para deshabilitar el log en consola
            }
            STACK_TRACE_FILTERED = props.getProperty("STACK_TRACE_FILTERED");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("%s::Error al cargar un archivo:\n%s", databaseRef, getStackTrace(e)));
        }
    }
}

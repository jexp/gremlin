package com.tinkerpop.gremlin;

import com.tinkerpop.gremlin.model.Element;
import com.tinkerpop.gremlin.statements.EvaluationException;
import com.tinkerpop.gremlin.statements.SyntaxException;
import jline.ConsoleReader;
import jline.History;
import org.apache.commons.jxpath.JXPathException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @version 0.1
 */
public class Console {

    private static final String INDENT = "         ";
    private static final int TAB_LENGTH = 2;
    private static final String PROMPT = "gremlin> ";
    private static final String QUIT = "quit";
    private static final String SINGLE_SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final String SYNTAX_ERROR = "Syntax error: ";
    private static final String EVALUATION_ERROR = "Evaluation error: ";
    private static final ElementPropertyHandler pathPropertyHandler = new ElementPropertyHandler() {
        public void setProperty(Object object, String propertyName, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getProperty(Object object, String key) {
            return containsProperty(key) ? super.getProperty(object,key) : null;
        }
    };
    //private static final String GREMLIN_VERSION = "0.1";


    public static void main(final String[] args) throws IOException {

        PrintStream output = System.out;

        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        reader.setUseHistory(true);

        try {
            History history = new History();
            history.setHistoryFile(new File(".gremlin_history"));
            reader.setHistory(history);
        } catch (IOException e) {
        }

        GremlinEvaluator gremlinEvaluator = new GremlinEvaluator();

        String line = "";
        output.println();
        output.println("         \\,,,/");
        output.println("         (o o)");
        output.println("-----oOOo-(_)-oOOo-----");
        //output.println("     Gremlin v" + GREMLIN_VERSION);

        while (line != null) {
            if (gremlinEvaluator.inStatement())
                line = reader.readLine(INDENT + generateIndentation(gremlinEvaluator.getDepth() * TAB_LENGTH));
            else {
                line = reader.readLine(PROMPT);
                if (null == line || line.equalsIgnoreCase(QUIT)) {
                    if (null == line) {
                        output.println();
                    }
                    break;
                }
            }
            if (line.length() > 0) {
                try {
                    printResults(output, gremlinEvaluator.evaluate(line));
                } catch (JXPathException e) {
                    exception(output, null, e);
                } catch (SyntaxException e) {
                    exception(output, SYNTAX_ERROR, e);
                } catch (EvaluationException e) {
                    exception(output, EVALUATION_ERROR, e);
                } catch (Exception e) {
                    exception(output, null, e);
                }
            }
        }
    }

    private static void exception(PrintStream output, String prefix, Exception e) {
        output.println("EX: " + (prefix != null ? prefix : "") + e.getMessage());
    }

    private static void printResults(PrintStream output, List results) {
        if (null == results || results.isEmpty()) return;

        if (results.size() == 1 && results.get(0) instanceof Map) {
            Map<Object,Object> map = (Map) results.get(0);
            for (Map.Entry entry : map.entrySet()) {
                output.printf("==> %s = %s%n",toString(entry.getKey()),toString(map.get(entry.getValue())));
            }
        } else {
            for (Object value : results) {
                output.printf("==> %s%n",toString(value));
            }
        }
    }

    public static String toString(Object value) {
        if (value == null) return "null";
        final String stringValue = value.toString();
        if (!(value instanceof Element)) return stringValue;
        final Object toString = pathPropertyHandler.getProperty(value, "toString");
        return toString != null ? String.format("%s (%s)", stringValue, toString) : stringValue;
    }


    private static String generateIndentation(final int spaces) {
        String spaceString = EMPTY_STRING;
        for (int i = 0; i < spaces; i++) {
            spaceString = spaceString + SINGLE_SPACE;
        }
        return spaceString;
    }
}

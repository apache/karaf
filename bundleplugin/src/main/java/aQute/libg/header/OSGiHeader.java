package aQute.libg.header;

import java.util.*;

import aQute.libg.generics.*;
import aQute.libg.qtokens.*;
import aQute.libg.reporter.*;

public class OSGiHeader {

    static public Map<String, Map<String, String>> parseHeader(String value) {
        return parseHeader(value, null);
    }

    /**
     * Standard OSGi header parser. This parser can handle the format clauses
     * ::= clause ( ',' clause ) + clause ::= name ( ';' name ) (';' key '='
     * value )
     * 
     * This is mapped to a Map { name => Map { attr|directive => value } }
     * 
     * @param value
     *            A string
     * @return a Map<String,Map<String,String>>
     */
    static public Map<String, Map<String, String>> parseHeader(String value,
            Reporter logger) {
        if (value == null || value.trim().length() == 0)
            return Create.map();

        Map<String, Map<String, String>> result = Create.map();
        QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
        char del = 0;
        do {
            boolean hadAttribute = false;
            Map<String, String> clause = Create.map();
            List<String> aliases = Create.list();
            String name = qt.nextToken(",;");

            del = qt.getSeparator();
            if (name == null || name.length() == 0) {
                if (logger != null && logger.isPedantic()) {
                    logger
                            .warning("Empty clause, usually caused by repeating a comma without any name field or by having spaces after the backslash of a property file: "
                                    + value);
                }
                if (name == null)
                    break;
            } else {
                name = name.trim();

                aliases.add(name);
                while (del == ';') {
                    String adname = qt.nextToken();
                    if ((del = qt.getSeparator()) != '=') {
                        if (hadAttribute)
                            if (logger != null) {
                                logger
                                        .error("Header contains name field after attribute or directive: "
                                                + adname
                                                + " from "
                                                + value
                                                + ". Name fields must be consecutive, separated by a ';' like a;b;c;x=3;y=4");
                            }
                        if (adname != null && adname.length() > 0)
                            aliases.add(adname.trim());
                    } else {
                        String advalue = qt.nextToken();
                        if (clause.containsKey(adname)) {
                            if (logger != null && logger.isPedantic())
                                logger
                                        .warning("Duplicate attribute/directive name "
                                                + adname
                                                + " in "
                                                + value
                                                + ". This attribute/directive will be ignored");
                        }
                        if (advalue == null) {
                            if (logger != null)
                                logger
                                        .error("No value after '=' sign for attribute "
                                                + adname);
                            advalue = "";
                        }
                        clause.put(adname.trim(), advalue.trim());
                        del = qt.getSeparator();
                        hadAttribute = true;
                    }
                }

                // Check for duplicate names. The aliases list contains
                // the list of nams, for each check if it exists. If so,
                // add a number of "~" to make it unique.
                for (String clauseName : aliases) {
                    if (result.containsKey(clauseName)) {
                        if (logger != null && logger.isPedantic())
                            logger
                                    .warning("Duplicate name "
                                            + clauseName
                                            + " used in header: '"
                                            + clauseName
                                            + "'. Duplicate names are specially marked in Bnd with a ~ at the end (which is stripped at printing time).");
                        while (result.containsKey(clauseName))
                            clauseName += "~";
                    }
                    result.put(clauseName, clause);
                }
            }
        } while (del == ',');
        return result;
    }

    public static Map<String, String> parseProperties(String input) {
        return parseProperties(input, null);
    }

    public static Map<String, String> parseProperties(String input, Reporter logger) {
        if (input == null || input.trim().length() == 0)
            return Create.map();

        Map<String, String> result = Create.map();
        QuotedTokenizer qt = new QuotedTokenizer(input, "=,");
        char del = ',';

        while (del == ',') {
            String key = qt.nextToken(",=");
            String value = "";
            del = qt.getSeparator();
            if (del == '=') {
                value = qt.nextToken(",=");
                del = qt.getSeparator();
            }
            result.put(key, value);
        }
        if (del != 0)
            if ( logger == null )
            throw new IllegalArgumentException(
                    "Invalid syntax for properties: " + input);
            else
                logger.error("Invalid syntax for properties: " + input);

        return result;
    }

}

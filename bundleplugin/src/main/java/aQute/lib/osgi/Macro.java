/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import aQute.libg.sed.*;
import aQute.libg.version.*;

/**
 * Provide a macro processor. This processor can replace variables in strings
 * based on a properties and a domain. The domain can implement functions that
 * start with a "_" and take args[], the names of these functions are available
 * as functions in the macro processor (without the _). Macros can nest to any
 * depth but may not contain loops.
 * 
 */
public class Macro implements Replacer {
    Properties properties;
    Processor  domain;
    Object     targets[];
    boolean    flattening;

    public Macro(Properties properties, Processor domain, Object... targets) {
        this.properties = properties;
        this.domain = domain;
        this.targets = targets;
        if (targets != null) {
            for (Object o : targets) {
                assert o != null;
            }
        }
    }

    public Macro(Processor processor) {
        this(new Properties(), processor);
    }

    public String process(String line) {
        return process(line, null);
    }

    String process(String line, Link link) {
        StringBuffer sb = new StringBuffer();
        process(line, 0, '\u0000', '\u0000', sb, link);
        return sb.toString();
    }

    int process(CharSequence org, int index, char begin, char end,
            StringBuffer result, Link link) {
        StringBuilder line = new StringBuilder(org);
        int nesting = 1;

        StringBuffer variable = new StringBuffer();
        outer: while (index < line.length()) {
            char c1 = line.charAt(index++);
            if (c1 == end) {
                if (--nesting == 0) {
                    result.append(replace(variable.toString(), link));
                    return index;
                }
            } else if (c1 == begin)
                nesting++;
            else if (c1 == '\\' && index < line.length() - 1
                    && line.charAt(index) == '$') {
                // remove the escape backslash and interpret the dollar as a
                // literal
                index++;
                variable.append('$');
                continue outer;
            } else if (c1 == '$' && index < line.length() - 2) {
                char c2 = line.charAt(index);
                char terminator = getTerminator(c2);
                if (terminator != 0) {
                    index = process(line, index + 1, c2, terminator, variable,
                            link);
                    continue outer;
                }
            }
            variable.append(c1);
        }
        result.append(variable);
        return index;
    }

    public static char getTerminator(char c) {
        switch (c) {
        case '(':
            return ')';
        case '[':
            return ']';
        case '{':
            return '}';
        case '<':
            return '>';
        case '\u00ab': // Guillemet double << >>
            return '\u00bb';
        case '\u2039': // Guillemet single
            return '\u203a';
        }
        return 0;
    }

    protected String replace(String key, Link link) {
        if (link != null && link.contains(key))
            return "${infinite:" + link.toString() + "}";

        if (key != null) {
            key = key.trim();
            if (key.length() > 0) {
                String value = (String) properties.getProperty(key);
                if (value != null)
                    return process(value, new Link(link, key));

                value = doCommands(key);
                if (value != null)
                    return process(value, new Link(link, key));

                if (key != null && key.trim().length() > 0) {
                    value = System.getProperty(key);
                    if (value != null)
                        return value;
                }
                if (!flattening)
                    domain.warning("No translation found for macro: " + key);
            } else {
                domain.warning("Found empty macro key");
            }
        } else {
            domain.warning("Found null macro key");
        }
        return "${" + key + "}";
    }

    /**
     * Parse the key as a command. A command consist of parameters separated by
     * ':'.
     * 
     * @param key
     * @return
     */
    static Pattern commands = Pattern.compile("(?<!\\\\);");

    private String doCommands(String key) {
        String[] args = commands.split(key);
        if (args == null || args.length == 0)
            return null;

        for (int i = 0; i < args.length; i++)
            if (args[i].indexOf('\\') >= 0)
                args[i] = args[i].replaceAll("\\\\;", ";");

        Processor rover = domain;
        while (rover != null) {
            String result = doCommand(rover, args[0], args);
            if (result != null)
                return result;

            rover = rover.getParent();
        }

        for (int i = 0; targets != null && i < targets.length; i++) {
            String result = doCommand(targets[i], args[0], args);
            if (result != null)
                return result;
        }

        return doCommand(this, args[0], args);
    }

    private String doCommand(Object target, String method, String[] args) {
        if (target == null)
            ; // System.out.println("Huh? Target should never be null " +
                // domain);
        else {
            String cname = "_" + method.replaceAll("-", "_");
            try {
                Method m = target.getClass().getMethod(cname,
                        new Class[] { String[].class });
                return (String) m.invoke(target, new Object[] { args });
            } catch (NoSuchMethodException e) {
                // Ignore
            } catch (InvocationTargetException e) {
                domain.warning("Exception in replace: " + e.getCause());
                e.printStackTrace();
            } catch (Exception e) {
                domain.warning("Exception in replace: " + e + " method="
                        + method);
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Return a unique list where the duplicates are removed.
     * 
     * @param args
     * @return
     */
    static String _uniqHelp = "${uniq;<list> ...}";

    public String _uniq(String args[]) {
        verifyCommand(args, _uniqHelp, null, 1, Integer.MAX_VALUE);
        Set<String> set = new LinkedHashSet<String>();
        for (int i = 1; i < args.length; i++) {
            Processor.split(args[i], set);
        }
        return Processor.join(set, ",");
    }

    public String _filter(String args[]) {
        return filter(args, false);
    }

    public String _filterout(String args[]) {
        return filter(args, true);

    }

    static String _filterHelp = "${%s;<list>;<regex>}";

    String filter(String[] args, boolean include) {
        verifyCommand(args, String.format(_filterHelp, args[0]), null, 3, 3);

        Collection<String> list = new ArrayList<String>(Processor
                .split(args[1]));
        Pattern pattern = Pattern.compile(args[2]);

        for (Iterator<String> i = list.iterator(); i.hasNext();) {
            if (pattern.matcher(i.next()).matches() == include)
                i.remove();
        }
        return Processor.join(list);
    }

    static String _sortHelp = "${sort;<list>...}";

    public String _sort(String args[]) {
        verifyCommand(args, _sortHelp, null, 2, Integer.MAX_VALUE);

        List<String> result = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            Processor.split(args[i], result);
        }
        Collections.sort(result);
        return Processor.join(result);
    }

    static String _joinHelp = "${join;<list>...}";

    public String _join(String args[]) {

        verifyCommand(args, _joinHelp, null, 1, Integer.MAX_VALUE);

        List<String> result = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            Processor.split(args[i], result);
        }
        return Processor.join(result);
    }

    static String _ifHelp = "${if;<condition>;<iftrue> [;<iffalse>] }";

    public String _if(String args[]) {
        verifyCommand(args, _ifHelp, null, 3, 4);
        String condition = args[1].trim();
        if (condition.length() != 0)
            return args[2];
        if (args.length > 3)
            return args[3];
        else
            return "";
    }

    public String _now(String args[]) {
        return new Date().toString();
    }

    public static String _fmodifiedHelp = "${fmodified;<list of filenames>...}, return latest modification date";

    public String _fmodified(String args[]) throws Exception {
        verifyCommand(args, _fmodifiedHelp, null, 2, Integer.MAX_VALUE);

        long time = 0;
        Collection<String> names = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            Processor.split(args[i], names);
        }
        for (String name : names) {
            File f = new File(name);
            if (f.exists() && f.lastModified() > time)
                time = f.lastModified();
        }
        return "" + time;
    }

    public String _long2date(String args[]) {
        try {
            return new Date(Long.parseLong(args[1])).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "not a valid long";
    }

    public String _literal(String args[]) {
        if (args.length != 2)
            throw new RuntimeException(
                    "Need a value for the ${literal;<value>} macro");
        return "${" + args[1] + "}";
    }

    public String _def(String args[]) {
        if (args.length != 2)
            throw new RuntimeException(
                    "Need a value for the ${def;<value>} macro");

        String value = properties.getProperty(args[1]);
        if (value == null)
            return "";
        else
            return value;
    }

    /**
     * 
     * replace ; <list> ; regex ; replace
     * 
     * @param args
     * @return
     */
    public String _replace(String args[]) {
        if (args.length != 4) {
            domain.warning("Invalid nr of arguments to replace "
                    + Arrays.asList(args));
            return null;
        }

        String list[] = args[1].split("\\s*,\\s*");
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (int i = 0; i < list.length; i++) {
            String element = list[i].trim();
            if (!element.equals("")) {
                sb.append(del);
                sb.append(element.replaceAll(args[2], args[3]));
                del = ", ";
            }
        }

        return sb.toString();
    }

    public String _warning(String args[]) {
        for (int i = 1; i < args.length; i++) {
            domain.warning(process(args[i]));
        }
        return "";
    }

    public String _error(String args[]) {
        for (int i = 1; i < args.length; i++) {
            domain.error(process(args[i]));
        }
        return "";
    }

    /**
     * toclassname ; <path>.class ( , <path>.class ) *
     * 
     * @param args
     * @return
     */
    static String _toclassnameHelp = "${classname;<list of class names>}, convert class paths to FQN class names ";

    public String _toclassname(String args[]) {
        verifyCommand(args, _toclassnameHelp, null, 2, 2);
        Collection<String> paths = Processor.split(args[1]);

        List<String> names = new ArrayList<String>(paths.size());
        for (String path : paths) {
            if (path.endsWith(".class")) {
                String name = path.substring(0, path.length() - 6).replace('/',
                        '.');
                names.add(name);
            } else if (path.endsWith(".java")) {
                String name = path.substring(0, path.length() - 5).replace('/',
                        '.');
                names.add(name);
            } else {
                domain
                        .warning("in toclassname, "
                                + args[1]
                                + " is not a class path because it does not end in .class");
            }
        }
        return Processor.join(names, ",");
    }

    /**
     * toclassname ; <path>.class ( , <path>.class ) *
     * 
     * @param args
     * @return
     */

    static String _toclasspathHelp = "${toclasspath;<list>[;boolean]}, convert a list of class names to paths";

    public String _toclasspath(String args[]) {
        verifyCommand(args, _toclasspathHelp, null, 2, 3);
        boolean cl= true;
        if (args.length>2)
            cl = new Boolean(args[2]);
        
        Collection<String> names = Processor.split(args[1]);
        Collection<String> paths = new ArrayList<String>(names.size());
        for (String name : names) {
            String path = name.replace('.', '/') + (cl ? ".class" : "");
            paths.add(path);
        }
        return Processor.join(paths, ",");
    }

    public String _dir(String args[]) {
        if (args.length < 2) {
            domain.warning("Need at least one file name for ${dir;...}");
            return null;
        } else {
            String del = "";
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < args.length; i++) {
                File f = new File(args[i]).getAbsoluteFile();
                if (f.exists() && f.getParentFile().exists()) {
                    sb.append(del);
                    sb.append(f.getParentFile().getAbsolutePath());
                    del = ",";
                }
            }
            return sb.toString();
        }

    }

    public String _basename(String args[]) {
        if (args.length < 2) {
            domain.warning("Need at least one file name for ${basename;...}");
            return null;
        } else {
            String del = "";
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < args.length; i++) {
                File f = new File(args[i]).getAbsoluteFile();
                if (f.exists() && f.getParentFile().exists()) {
                    sb.append(del);
                    sb.append(f.getName());
                    del = ",";
                }
            }
            return sb.toString();
        }

    }

    public String _isfile(String args[]) {
        if (args.length < 2) {
            domain.warning("Need at least one file name for ${isfile;...}");
            return null;
        } else {
            boolean isfile = true;
            for (int i = 1; i < args.length; i++) {
                File f = new File(args[i]).getAbsoluteFile();
                isfile &= f.isFile();
            }
            return isfile ? "true" : "false";
        }

    }

    public String _isdir(String args[]) {
        if (args.length < 2) {
            domain.warning("Need at least one file name for ${isdir;...}");
            return null;
        } else {
            boolean isdir = true;
            for (int i = 1; i < args.length; i++) {
                File f = new File(args[i]).getAbsoluteFile();
                isdir &= f.isDirectory();
            }
            return isdir ? "true" : "false";
        }

    }

    public String _tstamp(String args[]) {
        String format = "yyyyMMddHHmm";
        long now = System.currentTimeMillis();

        if (args.length > 1) {
            format = args[1];
            if (args.length > 2) {
                now = Long.parseLong(args[2]);
                if (args.length > 3) {
                    domain.warning("Too many arguments for tstamp: "
                            + Arrays.toString(args));
                }
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(now));
    }

    /**
     * Wildcard a directory. The lists can contain Instruction that are matched
     * against the given directory
     * 
     * ${wc;<dir>;<list>(;<list>)*}
     * 
     * @author aqute
     * 
     */

    public String _lsr(String args[]) {
        return ls(args, true);
    }

    public String _lsa(String args[]) {
        return ls(args, false);
    }

    String ls(String args[], boolean relative) {
        if (args.length < 2)
            throw new IllegalArgumentException(
                    "the ${ls} macro must at least have a directory as parameter");

        File dir = new File(args[1]);
        if (!dir.isAbsolute())
            throw new IllegalArgumentException(
                    "the ${ls} macro directory parameter is not absolute: "
                            + dir);

        if (!dir.exists())
            throw new IllegalArgumentException(
                    "the ${ls} macro directory parameter does not exist: "
                            + dir);

        if (!dir.isDirectory())
            throw new IllegalArgumentException(
                    "the ${ls} macro directory parameter points to a file instead of a directory: "
                            + dir);

        String[] files = dir.list();
        List<String> result;

        if (args.length < 3) {
            result = Arrays.asList(files);
        } else
            result = new ArrayList<String>();

        for (int i = 2; i < args.length; i++) {
            String parts[] = args[i].split("\\s*,\\s*");
            for (String pattern : parts) {
                // So make it in to an instruction
                Instruction instr = Instruction.getPattern(pattern);

                // For each project, match it against the instruction
                for (int f = 0; f < files.length; f++) {
                    if (files[f] != null) {
                        if (instr.matches(files[f])) {
                            if (!instr.isNegated()) {
                                if (relative)
                                    result.add(files[f]);
                                else
                                    result.add(new File(dir, files[f])
                                            .getAbsolutePath());
                            }
                            files[f] = null;
                        }
                    }
                }
            }
        }
        return Processor.join(result, ",");
    }

    public String _currenttime(String args[]) {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * Modify a version to set a version policy. Thed policy is a mask that is
     * mapped to a version.
     * 
     * <pre>
     * +           increment
     * -           decrement
     * =           maintain
     * &tilde;           discard
     * 
     * ==+      = maintain major, minor, increment micro, discard qualifier
     * &tilde;&tilde;&tilde;=     = just get the qualifier
     * version=&quot;[${version;==;${@}},${version;=+;${@}})&quot;
     * </pre>
     * 
     * 
     * 
     * 
     * @param args
     * @return
     */
    static String  _versionHelp      = "${version;<mask>;<version>}, modify a version\n"
                                             + "<mask> ::= [ M [ M [ M [ MQ ]]]\n"
                                             + "M ::= '+' | '-' | MQ\n"
                                             + "MQ ::= '~' | '='";
    static Pattern _versionPattern[] = new Pattern[] { null, null,
            Pattern.compile("[-+=~]{0,3}[=~]?"), Verifier.VERSION };

    public String _version(String args[]) {
        verifyCommand(args, _versionHelp, null, 3, 3);

        String mask = args[1];

        Version version = new Version(args[2]);
        StringBuilder sb = new StringBuilder();
        String del = "";

        for (int i = 0; i < mask.length(); i++) {
            char c = mask.charAt(i);
            String result = null;
            if (c != '~') {
                if (i == 3) {
                    result = version.getQualifier();
                } else if (Character.isDigit(c)) {
                	// Handle masks like +00, =+0
                	result = String.valueOf(c);
                } else {
                    int x = version.get(i);
                    switch (c) {
                    case '+':
                        x++;
                        break;
                    case '-':
                        x--;
                        break;
                    case '=':
                        break;
                    }
                    result = Integer.toString(x);
                }
                if (result != null) {
                    sb.append(del);
                    del = ".";
                    sb.append(result);
                }
            }
        }
        return sb.toString();
    }

    /**
     * System command. Execute a command and insert the result.
     * 
     * @param args
     * @param help
     * @param patterns
     * @param low
     * @param high
     */
    public String _system(String args[]) throws Exception {
        verifyCommand(args,
                "${system;<command>[;<in>]}, execute a system command", null,
                2, 3);
        String command = args[1];
        String input = null;

        if (args.length > 2) {
            input = args[2];
        }

        Process process = Runtime.getRuntime().exec(command, null,
                domain.getBase());
        if (input != null) {
            process.getOutputStream().write(input.getBytes("UTF-8"));
        }
        process.getOutputStream().close();

        String s = getString(process.getInputStream());
        process.getInputStream().close();
        int exitValue = process.waitFor();
        if (exitValue != 0) {
            domain.error("System command " + command + " failed with "
                    + exitValue);
        }
        return s.trim();
    }

    /**
     * Get the contents of a file.
     * 
     * @param in
     * @return
     * @throws IOException
     */

    public String _cat(String args[]) throws IOException {
        verifyCommand(args, "${cat;<in>}, get the content of a file", null, 2,
                2);
        File f = domain.getFile(args[1]);
        if (f.isFile()) {
            InputStream in = new FileInputStream(f);
            return getString(in);
        } else if (f.isDirectory()) {
            return Arrays.toString(f.list());
        } else {
            try {
                URL url = new URL(args[1]);
                InputStream in = url.openStream();
                return getString(in);
            } catch (MalformedURLException mfue) {
                // Ignore here
            }
            return null;
        }
    }

    public static String getString(InputStream in) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = rdr.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } finally {
            in.close();
        }
    }

    public static void verifyCommand(String args[], String help,
            Pattern[] patterns, int low, int high) {
        String message = "";
        if (args.length > high) {
            message = "too many arguments";
        } else if (args.length < low) {
            message = "too few arguments";
        } else {
            for (int i = 0; patterns != null && i < patterns.length
                    && i < args.length - 1; i++) {
                if (patterns[i] != null
                        && !patterns[i].matcher(args[i + 1]).matches()) {
                    message += String.format(
                            "Argument %s (%s) does not match %s\n", i, args[i],
                            patterns[i].pattern());
                }
            }
        }
        if (message.length() != 0) {
            StringBuilder sb = new StringBuilder();
            String del = "${";
            for (String arg : args) {
                sb.append(del);
                sb.append(arg);
                del = ";";
            }
            sb.append("}, is not understood. ");
            sb.append(message);
            throw new IllegalArgumentException(sb.toString());
        }
    }

    // Helper class to track expansion of variables
    // on the stack.
    static class Link {
        Link   previous;
        String key;

        public Link(Link previous, String key) {
            this.previous = previous;
            this.key = key;
        }

        public boolean contains(String key) {
            if (this.key.equals(key))
                return true;

            if (previous == null)
                return false;

            return previous.contains(key);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            String del = "[";
            for (Link r = this; r != null; r = r.previous) {
                sb.append(del);
                sb.append(r.key);
                del = ",";
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Take all the properties and translate them to actual values. This method
     * takes the set properties and traverse them over all entries, including
     * the default properties for that properties. The values no longer contain
     * macros.
     * 
     * @return A new Properties with the flattened values
     */
    public Properties getFlattenedProperties() {
        // Some macros only work in a lower processor, so we
        // do not report unknown macros while flattening
        flattening = true;
        try {
            Properties flattened = new Properties();
            for (Enumeration<?> e = properties.propertyNames(); e
                    .hasMoreElements();) {
                String key = (String) e.nextElement();
                if (!key.startsWith("_"))
                    if ( key.startsWith("-"))
                        flattened.put(key, properties.getProperty(key));
                    else
                        flattened.put(key, process(properties.getProperty(key)));
            }
            return flattened;
        } finally {
            flattening = false;
        }
    };

}

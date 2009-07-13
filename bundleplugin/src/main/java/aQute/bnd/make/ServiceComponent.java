package aQute.bnd.make;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.service.*;
import aQute.lib.filter.*;
import aQute.lib.osgi.*;
import aQute.libg.version.*;

/**
 * This class is an analyzer plugin. It looks at the properties and tries to
 * find out if the Service-Component header contains the bnd shortut syntax. If
 * not, the header is copied to the output, if it does, an XML file is created
 * and added to the JAR and the header is modified appropriately.
 */
public class ServiceComponent implements AnalyzerPlugin {
    public final static String      NAMESPACE_STEM                 = "http://www.osgi.org/xmlns/scr";
    public final static String      JIDENTIFIER                    = "<<identifier>>";
    public final static String      COMPONENT_FACTORY              = "factory:";
    public final static String      COMPONENT_SERVICEFACTORY       = "servicefactory:";
    public final static String      COMPONENT_IMMEDIATE            = "immediate:";
    public final static String      COMPONENT_ENABLED              = "enabled:";
    public final static String      COMPONENT_DYNAMIC              = "dynamic:";
    public final static String      COMPONENT_MULTIPLE             = "multiple:";
    public final static String      COMPONENT_PROVIDE              = "provide:";
    public final static String      COMPONENT_OPTIONAL             = "optional:";
    public final static String      COMPONENT_PROPERTIES           = "properties:";
    public final static String      COMPONENT_IMPLEMENTATION       = "implementation:";

    // v1.1.0
    public final static String      COMPONENT_VERSION              = "version:";
    public final static String      COMPONENT_CONFIGURATION_POLICY = "configuration-policy:";
    public final static String      COMPONENT_MODIFIED             = "modified:";
    public final static String      COMPONENT_ACTIVATE             = "activate:";
    public final static String      COMPONENT_DEACTIVATE           = "deactivate:";

    public final static String[]    componentDirectives            = new String[] {
            COMPONENT_FACTORY, COMPONENT_IMMEDIATE, COMPONENT_ENABLED,
            COMPONENT_DYNAMIC, COMPONENT_MULTIPLE, COMPONENT_PROVIDE,
            COMPONENT_OPTIONAL, COMPONENT_PROPERTIES, COMPONENT_IMPLEMENTATION,
            COMPONENT_SERVICEFACTORY, COMPONENT_VERSION,
            COMPONENT_CONFIGURATION_POLICY, COMPONENT_MODIFIED,
            COMPONENT_ACTIVATE, COMPONENT_DEACTIVATE       };

    public final static Set<String> SET_COMPONENT_DIRECTIVES       = new HashSet<String>(
                                                                    Arrays
                                                                            .asList(componentDirectives));

    public final static Set<String> SET_COMPONENT_DIRECTIVES_1_1   = //
                                                            new HashSet<String>(
                                                                    Arrays
                                                                            .asList(
                                                                                    COMPONENT_VERSION,
                                                                                    COMPONENT_CONFIGURATION_POLICY,
                                                                                    COMPONENT_MODIFIED,
                                                                                    COMPONENT_ACTIVATE,
                                                                                    COMPONENT_DEACTIVATE));

    public boolean analyzeJar(Analyzer analyzer) throws Exception {

        ComponentMaker m = new ComponentMaker(analyzer);

        Map<String, Map<String, String>> l = m.doServiceComponent();

        if (!l.isEmpty())
            analyzer.setProperty(Constants.SERVICE_COMPONENT, Processor
                    .printClauses(l, ""));

        analyzer.getInfo(m, "Service Component");
        m.close();
        return false;
    }

    private static class ComponentMaker extends Processor {
        Analyzer analyzer;

        ComponentMaker(Analyzer analyzer) {
            super(analyzer);
            this.analyzer = analyzer;
        }

        Map<String, Map<String, String>> doServiceComponent() throws Exception {
            String header = getProperty(SERVICE_COMPONENT);
            return doServiceComponent(header);
        }

        /**
         * Check if a service component header is actually referring to a class.
         * If so, replace the reference with an XML file reference. This makes
         * it easier to create and use components.
         * 
         * @throws UnsupportedEncodingException
         * 
         */
        public Map<String, Map<String, String>> doServiceComponent(
                String serviceComponent) throws IOException {
            Map<String, Map<String, String>> list = newMap();
            Map<String, Map<String, String>> sc = parseHeader(serviceComponent);
            Map<String, String> empty = Collections.emptyMap();

            for (Iterator<Map.Entry<String, Map<String, String>>> i = sc
                    .entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Map<String, String>> entry = i.next();
                String name = entry.getKey();
                Map<String, String> info = entry.getValue();
                if (name == null) {
                    error("No name in Service-Component header: " + info);
                    continue;
                }
                if (name.indexOf("*") >= 0 || analyzer.getJar().exists(name)) {
                    // Normal service component, we do not process them
                    list.put(name, info);
                } else {
                    String impl = name;

                    if (info.containsKey(COMPONENT_IMPLEMENTATION))
                        impl = info.get(COMPONENT_IMPLEMENTATION);

                    if (!analyzer.checkClass(impl)) {
                        error("Not found Service-Component header: " + name);
                    } else {
                        // We have a definition, so make an XML resources
                        Resource resource = createComponentResource(name, info);
                        analyzer.getJar().putResource(
                                "OSGI-INF/" + name + ".xml", resource);
                        list.put("OSGI-INF/" + name + ".xml", empty);
                    }
                }
            }
            return list;
        }

        /**
         * Create the resource for a DS component.
         * 
         * @param list
         * @param name
         * @param info
         * @throws UnsupportedEncodingException
         */
        Resource createComponentResource(String name, Map<String, String> info)
                throws IOException {
            String namespace = getNamespace(info);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out,
                    "UTF-8"));
            pw.println("<?xml version='1.0' encoding='utf-8'?>");
            pw.print("<component name='" + name + "'");
            if (namespace != null) {
                pw.print(" xmlns='" + namespace + "'");
            }

            doAttribute(pw, info.get(COMPONENT_FACTORY), "factory");
            doAttribute(pw, info.get(COMPONENT_IMMEDIATE), "immediate",
                    "false", "true");
            doAttribute(pw, info.get(COMPONENT_ENABLED), "enabled", "true",
                    "false");
            doAttribute(pw, info.get(COMPONENT_CONFIGURATION_POLICY),
                    "configuration-policy", "optional", "require", "ignore");
            doAttribute(pw, info.get(COMPONENT_ACTIVATE), "activate",
                    JIDENTIFIER);
            doAttribute(pw, info.get(COMPONENT_DEACTIVATE), "deactivate",
                    JIDENTIFIER);
            doAttribute(pw, info.get(COMPONENT_MODIFIED), "modified",
                    JIDENTIFIER);

            pw.println(">");

            // Allow override of the implementation when people
            // want to choose their own name
            String impl = (String) info.get(COMPONENT_IMPLEMENTATION);
            pw.println("  <implementation class='"
                    + (impl == null ? name : impl) + "'/>");

            String provides = info.get(COMPONENT_PROVIDE);
            boolean servicefactory = Boolean.getBoolean(info
                    .get(COMPONENT_SERVICEFACTORY)
                    + "");
            provides(pw, provides, servicefactory);
            properties(pw, info);
            reference(info, pw);
            pw.println("</component>");
            pw.close();
            byte[] data = out.toByteArray();
            out.close();
            return new EmbeddedResource(data, 0);
        }

        private void doAttribute(PrintWriter pw, String value, String name,
                String... matches) {
            if (value != null) {
                if (matches.length != 0) {
                    if (matches.length == 1 && matches[0].equals(JIDENTIFIER)) {
                        if (!Verifier.isIdentifier(value))
                            error(
                                    "Component attribute %s has value %s but is not a Java identifier",
                                    name, value);
                    } else {

                        if (!Verifier.isMember(value, matches))
                            error(
                                    "Component attribute %s has value %s but is not a member of %s",
                                    name, value, Arrays.toString(matches));
                    }
                }
                pw.print(" ");
                pw.print(name);
                pw.print("='");
                pw.print(value);
                pw.print("'");
            }
        }

        /**
         * Check if we need to use the v1.1 namespace (or later).
         * 
         * @param info
         * @return
         */
        private String getNamespace(Map<String, String> info) {
            String version = info.get(COMPONENT_VERSION);
            if (version != null) {
                try {
                    Version v = new Version(version);
                    return NAMESPACE_STEM + "/v" + v;
                } catch (Exception e) {
                    error("version: specified on component header but not a valid version: "
                            + version);
                    return null;
                }
            }
            for (String key : info.keySet()) {
                if (SET_COMPONENT_DIRECTIVES_1_1.contains(key)) {
                    return NAMESPACE_STEM + "/v1.1.0";
                }
            }
            return null;
        }

        /**
         * Print the Service-Component properties element
         * 
         * @param pw
         * @param info
         */
        void properties(PrintWriter pw, Map<String, String> info) {
            Collection<String> properties = split(info
                    .get(COMPONENT_PROPERTIES));
            for (Iterator<String> p = properties.iterator(); p.hasNext();) {
                String clause = p.next();
                int n = clause.indexOf('=');
                if (n <= 0) {
                    error("Not a valid property in service component: "
                            + clause);
                } else {
                    String type = null;
                    String name = clause.substring(0, n);
                    if (name.indexOf('@') >= 0) {
                        String parts[] = name.split("@");
                        name = parts[1];
                        type = parts[0];
                    }
                    String value = clause.substring(n + 1).trim();
                    // TODO verify validity of name and value.
                    pw.print("<property name='");
                    pw.print(name);
                    pw.print("'");

                    if (type != null) {
                        if (VALID_PROPERTY_TYPES.matcher(type).matches()) {
                            pw.print(" type='");
                            pw.print(type);
                            pw.print("'");
                        } else {
                            warning("Invalid property type '" + type
                                    + "' for property " + name);
                        }
                    }

                    String parts[] = value.split("\\s*(\\||\\n)\\s*");
                    if (parts.length > 1) {
                        pw.println(">");
                        for (String part : parts) {
                            pw.println(part);
                        }
                        pw.println("</property>");
                    } else {
                        pw.print(" value='");
                        pw.print(parts[0]);
                        pw.print("'/>");
                    }
                }
            }
        }

        /**
         * @param pw
         * @param provides
         */
        void provides(PrintWriter pw, String provides, boolean servicefactory) {
            if (provides != null) {
                if (!servicefactory)
                    pw.println("  <service>");
                else
                    pw.println("  <service servicefactory='true'>");

                StringTokenizer st = new StringTokenizer(provides, ",");
                while (st.hasMoreTokens()) {
                    String interfaceName = st.nextToken();
                    pw.println("    <provide interface='" + interfaceName
                            + "'/>");
                    if (!analyzer.checkClass(interfaceName))
                        error("Component definition provides a class that is neither imported nor contained: "
                                + interfaceName);
                }
                pw.println("  </service>");
            }
        }

        public final static Pattern REFERENCE = Pattern.compile("([^(]+)(\\(.+\\))?");

        /**
         * @param info
         * @param pw
         */

        void reference(Map<String, String> info, PrintWriter pw) {
            Collection<String> dynamic = new ArrayList<String>(split(info.get(COMPONENT_DYNAMIC)));
            Collection<String> optional =  new ArrayList<String>(split(info.get(COMPONENT_OPTIONAL)));
            Collection<String> multiple = new ArrayList<String>(split(info.get(COMPONENT_MULTIPLE)));

            for (Iterator<Map.Entry<String, String>> r = info.entrySet()
                    .iterator(); r.hasNext();) {
                Map.Entry<String, String> ref = r.next();
                String referenceName = (String) ref.getKey();
                String target = null;
                String interfaceName = (String) ref.getValue();
                if (interfaceName == null || interfaceName.length() == 0) {
                    error("Invalid Interface Name for references in Service Component: "
                            + referenceName + "=" + interfaceName);
                }
                char c = interfaceName.charAt(interfaceName.length() - 1);
                if ("?+*~".indexOf(c) >= 0) {
                    if (c == '?' || c == '*' || c == '~')
                        optional.add(referenceName);
                    if (c == '+' || c == '*')
                        multiple.add(referenceName);
                    if (c == '+' || c == '*' || c == '?')
                        dynamic.add(referenceName);
                    interfaceName = interfaceName.substring(0, interfaceName
                            .length() - 1);
                }

                if (referenceName.endsWith(":")) {
                    if (!SET_COMPONENT_DIRECTIVES.contains(referenceName))
                        error("Unrecognized directive in Service-Component header: "
                                + referenceName);
                    continue;
                }

                Matcher m = REFERENCE.matcher(interfaceName);
                if (m.matches()) {
                    interfaceName = m.group(1);
                    target = m.group(2);
                }

                if (!analyzer.checkClass(interfaceName))
                    error("Component definition refers to a class that is neither imported nor contained: "
                            + interfaceName);

                pw.print("  <reference name='" + referenceName
                        + "' interface='" + interfaceName + "'");

                String cardinality = optional.contains(referenceName) ? "0"
                        : "1";
                cardinality += "..";
                cardinality += multiple.contains(referenceName) ? "n" : "1";
                if (!cardinality.equals("1..1"))
                    pw.print(" cardinality='" + cardinality + "'");

                if (Character.isLowerCase(referenceName.charAt(0))) {
                    String z = referenceName.substring(0, 1).toUpperCase()
                            + referenceName.substring(1);
                    pw.print(" bind='set" + z + "'");
                    pw.print(" unbind='unset" + z + "'");
                    // TODO Verify that the methods exist
                }

                if (dynamic.contains(referenceName)) {
                    pw.print(" policy='dynamic'");
                }

                if (target != null) {
                    Filter filter = new Filter(target);
                    if (filter.verify() == null)
                        pw.print(" target='" + filter.toString() + "'");
                    else
                        error("Target for " + referenceName
                                + " is not a correct filter: " + target + " "
                                + filter.verify());
                }
                pw.println("/>");
            }
        }
    }
}

package org.apache.felix.bundleplugin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import aQute.bnd.service.AnalyzerPlugin;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.Resource;
import aQute.libg.generics.Create;
import aQute.libg.qtokens.QuotedTokenizer;
import aQute.libg.reporter.Reporter;

public class BlueprintPlugin implements AnalyzerPlugin {


    static Pattern QN = Pattern.compile("[_A-Za-z$][_A-Za-z0-9$]*(\\.[_A-Za-z$][_A-Za-z0-9$]*)*");
    static Pattern PATHS = Pattern.compile( ".*\\.xml");

    Transformer transformer;

    public BlueprintPlugin() throws Exception {
        transformer = getTransformer(getClass().getResource("blueprint.xsl"));
    }

    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        transformer.setParameter("nsh_interface", analyzer.getProperty("nsh_interface") != null ? analyzer.getProperty("nsh_interface") : "");
        transformer.setParameter("nsh_namespace", analyzer.getProperty("nsh_namespace") != null ? analyzer.getProperty("nsh_namespace") : "");

        Set<String> headers = Create.set();

        String bpHeader = analyzer.getProperty("Bundle-Blueprint", "OSGI-INF/blueprint");
        Map<String, Map<String,String>> map = Processor.parseHeader(bpHeader, null);
        for (String root : map.keySet())
        {
            Jar jar = analyzer.getJar();
            Map<String, Resource> dir = jar.getDirectories().get(root);
            if(dir == null || dir.isEmpty())
            {
                Resource resource = jar.getResource(root);
                if(resource != null)
                    process(analyzer, root, resource, headers);
                return false;
            }
            for(Map.Entry<String,Resource> entry : dir.entrySet())
            {
                String path = entry.getKey();
                Resource resource = entry.getValue();
                if(PATHS.matcher(path).matches())
                    process(analyzer, path, resource, headers);
            }

        }

        // Group and analyze
        Map<String, Set<Attribute>> hdrs = Create.map();
        for (String str : headers) {
            int idx = str.indexOf(':');
            if (idx < 0)
            {
                analyzer.warning((new StringBuilder("Error analyzing services in blueprint resource: ")).append(str).toString());
                continue;
            }
            String h = str.substring(0, idx).trim();
            String v = str.substring(idx + 1).trim();
            Set<Attribute> att = hdrs.get(h);
            if (att == null) {
                att = new TreeSet<Attribute>();
                hdrs.put(h, att);
            }
            att.addAll(parseHeader(v, null));
        }
        // Merge
        for (String header : hdrs.keySet())
        {
            if ("Import-Class".equals(header) || "Import-Package".equals(header))
            {
                Set<Attribute> newAttr = hdrs.get(header);
                for (Attribute a : newAttr)
                {
                    String pkg = a.getName();
                    if ("Import-Class".equals(header))
                    {
                        int n = a.getName().lastIndexOf('.');
                        if (n > 0) {
                            pkg = pkg.subSequence(0, n).toString();
                        } else {
                            continue;
                        }
                    }
                    if (!analyzer.getReferred().containsKey(pkg))
                    {
                        analyzer.getReferred().put(pkg, a.getProperties());
                    }
                }
            }
            else
            {
                Set<Attribute> orgAttr = parseHeader(analyzer.getProperty(header), null);
                Set<Attribute> newAttr = hdrs.get(header);
                for (Iterator<Attribute> it = newAttr.iterator(); it.hasNext();)
                {
                    Attribute a = it.next();
                    for (Attribute b : orgAttr)
                    {
                        if (b.getName().equals(a.getName()))
                        {
                            it.remove();
                            break;
                        }
                    }
                }
                orgAttr.addAll(newAttr);
                // Rebuild from orgAttr
                StringBuilder sb = new StringBuilder();
                for (Attribute a : orgAttr)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(",");
                    }
                    sb.append(a.getName());
                    for (Map.Entry<String, String> prop : a.getProperties().entrySet())
                    {
                        sb.append(';').append(prop.getKey()).append("=");
                        if (prop.getValue().matches("[0-9a-zA-Z_-]+")) {
                            sb.append(prop.getValue());
                        } else {
                            sb.append("\"");
                            sb.append(prop.getValue().replace("\"", "\\\""));
                            sb.append("\"");
                        }
                    }
                }
                analyzer.setProperty(header, sb.toString());
            }
        }
        return false;
    }

    private void process(Analyzer analyzer, String path, Resource resource, Set<String> headers)
    {
        InputStream in = null;
        try
        {
            in = resource.openInputStream();

            // Retrieve headers
            Set<String> set = analyze(in);
            headers.addAll(set);
        }
        catch(Exception e)
        {
            analyzer.error((new StringBuilder("Unexpected exception in processing spring resources(")).append(path).append("): ").append(e).toString());
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
            }
            catch (IOException e)
            {
            }
        }
    }

    public Set<String> analyze(InputStream in)
        throws Exception
    {
        Set<String> refers = new HashSet<String>();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        javax.xml.transform.Result r = new StreamResult(bout);
        javax.xml.transform.Source s = new StreamSource(in);
        transformer.transform(s, r);
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        bout.close();
        BufferedReader br = new BufferedReader(new InputStreamReader(bin));
        for(String line = br.readLine(); line != null; line = br.readLine())
        {
            line = line.trim();
            line = line.replace(";availability:=mandatory", "");
            if(line.length() > 0)
            {
                refers.add(line);
            }
        }

        br.close();
        return refers;
    }

    protected Transformer getTransformer(URL url)
        throws Exception
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        javax.xml.transform.Source source = new StreamSource(url.openStream());
        return tf.newTransformer(source);
    }

    public static class Attribute implements Comparable<Attribute>
    {
        private final String name;
        private final Map<String,String> properties;

        public Attribute(String name, Map<String, String> properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public int compareTo(Attribute a) {
            int c = name.compareTo(a.name);
            if (c == 0)
            {
                c = properties.equals(a.properties) ? 0 :
                        properties.size() < a.properties.size() ? -1 :
                            properties.hashCode() < a.properties.hashCode() ? -1 : +1;
            }
            return c;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Attribute attribute = (Attribute) o;

            if (name != null ? !name.equals(attribute.name) : attribute.name != null) return false;
            if (properties != null ? !properties.equals(attribute.properties) : attribute.properties != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (properties != null ? properties.hashCode() : 0);
            return result;
        }
    }

    public static Set<Attribute> parseHeader(String value, Reporter logger)
    {
        if ((value == null) || (value.trim().length() == 0)) {
            return new TreeSet<Attribute>();
        }
        Set<Attribute> result = new TreeSet<Attribute>();
        QuotedTokenizer qt = new QuotedTokenizer(value, ";=,");
        char del = '\0';
        do {
            boolean hadAttribute = false;
            Map<String,String> clause = Create.map();
            List<String> aliases = Create.list();
            String name = qt.nextToken(",;");

            del = qt.getSeparator();
            if ((name == null) || (name.length() == 0)) {
                if ((logger != null) && (logger.isPedantic())) {
                    logger.warning("Empty clause, usually caused by repeating a comma without any name field or by having " +
                                            "spaces after the backslash of a property file: " +
                                            value);
                }

                if (name != null)
                    continue;
                break;
            }
            name = name.trim();

            aliases.add(name);
            String advalue;
            while (del == ';') {
                String adname = qt.nextToken();
                if ((del = qt.getSeparator()) != '=') {
                    if ((hadAttribute) && (logger != null)) {
                        logger.error("Header contains name field after attribute or directive: " +
                                        adname +
                                        " from " +
                                        value +
                                        ". Name fields must be consecutive, separated by a ';' like a;b;c;x=3;y=4");
                    }

                    if ((adname != null) && (adname.length() > 0))
                        aliases.add(adname.trim());
                } else {
                    advalue = qt.nextToken();
                    if ((clause.containsKey(adname)) && (logger != null) && (logger.isPedantic())) {
                        logger.warning("Duplicate attribute/directive name " +
                            adname +
                            " in " +
                            value +
                            ". This attribute/directive will be ignored");
                    }

                    if (advalue == null) {
                        if (logger != null) {
                            logger.error("No value after '=' sign for attribute " + adname);
                        }
                        advalue = "";
                    }
                    clause.put(adname.trim(), advalue.trim());
                    del = qt.getSeparator();
                    hadAttribute = true;
                }
            }

            for (String clauseName : aliases) {
                result.add(new Attribute(clauseName, clause));
            }
        }
        while (del == ',');
        return result;
    }

}

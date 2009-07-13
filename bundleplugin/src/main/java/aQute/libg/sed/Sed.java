package aQute.libg.sed;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Sed {
    final File                 file;
    final Replacer             macro;
    File                       output;

    final Map<Pattern, String> replacements = new LinkedHashMap<Pattern, String>();

    public Sed(Replacer macro, File file) {
        assert file.isFile();
        this.file = file;
        this.macro = macro;
    }

    public void setOutput(File f) {
        output = f;
    }

    public void replace(String pattern, String replacement) {
        replacements.put(Pattern.compile(pattern), replacement);
    }

    public void doIt() throws IOException {
        BufferedReader brdr = new BufferedReader(new FileReader(file));
        File out;
        if (output != null)
            out = output;
        else
            out = new File(file.getAbsolutePath() + ".tmp");
        File bak = new File(file.getAbsolutePath() + ".bak");
        PrintWriter pw = new PrintWriter(new FileWriter(out));
        try {
            String line;
            while ((line = brdr.readLine()) != null) {
                for (Pattern p : replacements.keySet()) {
                    String replace = replacements.get(p);
                    Matcher m = p.matcher(line);

                    StringBuffer sb = new StringBuffer();
                    while (m.find()) {
                        String tmp = setReferences(m, replace);
                        tmp = macro.process(tmp);
                        m.appendReplacement(sb, Matcher.quoteReplacement(tmp));
                    }
                    m.appendTail(sb);

                    line = sb.toString();
                }
                pw.println(line);
            }
            pw.close();
            if (output == null) {
                file.renameTo(bak);
                out.renameTo(file);
            }
        } finally {
            brdr.close();
            pw.close();
        }
    }

    private String setReferences(Matcher m, String replace) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < replace.length(); i++) {
            char c = replace.charAt(i);
            if (c == '$' && i < replace.length() - 1
                    && Character.isDigit(replace.charAt(i + 1))) {
                int n = replace.charAt(i + 1) - '0';
                if ( n <= m.groupCount() )
                    sb.append(m.group(n));
                i++;
            } else
                sb.append(c);
        }
        return sb.toString();
    }
}

package org.apache.geronimo.gshell.commands.utils;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

@CommandComponent(id="utils:grep", description="Grep")
public class GrepCommand extends OsgiCommandSupport {

    @Argument(required=true, description="Regular expression")
    private String regex;

    @Option(name = "-v", description = "Inverse matching")
    private boolean inverse;

    protected Object doExecute() throws Exception {
        Pattern p = Pattern.compile(regex);
        try {
            while (true) {
                String line = readLine(io.in);
                if (p.matcher(line).matches() ^ inverse) {
                    io.out.println(line);
                }
            }
        } catch (IOException e) {
        }
        return null;
    }

    private String readLine(Reader in) throws IOException {
        StringBuffer buf = new StringBuffer();
        while (true) {
            int i = in.read();
            if (i == -1 && buf.length() == 0) {
                throw new IOException("break");
            }
            if (i == -1 || i == '\n' || i == '\r') {
                return buf.toString();
            }
            buf.append((char) i);
        }
    }

}

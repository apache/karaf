package org.apache.felix.dm.shell;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;

public class EquinoxDMCommand extends DMCommand implements CommandProvider {
    public EquinoxDMCommand(BundleContext context) {
        super(context);
    }
    
    public void _dm(CommandInterpreter ci) {
        StringBuffer line = new StringBuffer("dm ");
        String arg = ci.nextArgument();
        while (arg != null) {
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(arg);
            arg = ci.nextArgument();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        PrintStream err = new PrintStream(errorBytes);
        super.execute(line.toString(), out, err);
        if (bytes.size() > 0) {
            ci.print(new String(bytes.toByteArray()));
        }
        if (errorBytes.size() > 0) {
            ci.print("Error:\n");
            ci.print(new String(errorBytes.toByteArray()));
        }
    }

    public String getHelp() {
        return "\t" + super.getUsage() + " - " + super.getShortDescription() + "\n";
    }
}

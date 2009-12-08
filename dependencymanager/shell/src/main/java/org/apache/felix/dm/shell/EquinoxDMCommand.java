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
        StringBuffer line = new StringBuffer("");
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
        super.execute(line.toString(), new PrintStream(bytes), new PrintStream(errorBytes));
        ci.print(new String(bytes.toByteArray()));
    }

    public String getHelp() {
        return "\t" + super.getUsage() + " - " + super.getShortDescription() + "\n";
    }
}

package org.apache.felix.karaf.shell.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "shell", name = "echo", description="Echoes or prints arguments to STDOUT")
public class EchoAction extends OsgiCommandSupport
{
    @Option(name = "-n", aliases = {}, description = "Do not print the trailing newline character", required = false, multiValued = false)
    private boolean noTrailingNewline = false;

    @Argument(index = 0, name = "arguments", description="Arguments to display separated by whitespaces", required = false, multiValued = true)
    private List<String> args;

    protected Object doExecute() throws Exception {
        if (args != null) {
            boolean first = true;
            for (String arg : args) {
                if (first) {
                    first = false;
                } else {
                    System.out.print(" ");
                }
                System.out.print(arg);
            }
        }

        if (!noTrailingNewline) {
            System.out.println();
        }

        return null;
    }
}
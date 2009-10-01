package org.apache.felix.gogo.runtime;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.command.CommandProcessor;

public class FelixCommandAdaptor {
    public static final String FELIX_COMMAND = "org.apache.felix.shell.Command";
    private final Object felixCommand;
    private Method execute;
    private String help;
    private String name;
    private String usage;

    public FelixCommandAdaptor(Object felixCommand) throws Exception {
        this.felixCommand = felixCommand;
        Class<?> c = felixCommand.getClass();
        Class<?>[] parms = { String.class, PrintStream.class, PrintStream.class};
        execute = c.getMethod("execute", parms);

        Method name = c.getMethod("getName", (Class[]) null);
        this.name = (String) name.invoke(felixCommand, (Object[]) null);

        Method help = c.getMethod("getShortDescription", (Class[]) null);
        this.help = (String) help.invoke(felixCommand, (Object[]) null);

        Method usage = c.getMethod("getUsage", (Class[]) null);
        this.usage = (String) usage.invoke(felixCommand, (Object[]) null);
    }

    public void _main(String[] argv) throws Exception {
        StringBuilder buf = new StringBuilder();
        for (String arg : argv) {
            if (buf.length() > 0)
                buf.append(' ');
            buf.append(arg);
        }
        
        try {
            Object[] args = { buf.toString(), System.out, System.err};
            execute.invoke(felixCommand, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception)
                throw (Exception) cause;
            throw e;
        }
    }

    public Dictionary<String, Object> getAttributes() {
        Dictionary<String, Object> dict = new Hashtable<String, Object>();
        dict.put(CommandProcessor.COMMAND_SCOPE, "felix");
        dict.put(CommandProcessor.COMMAND_FUNCTION, name);
        return dict;
    }

}

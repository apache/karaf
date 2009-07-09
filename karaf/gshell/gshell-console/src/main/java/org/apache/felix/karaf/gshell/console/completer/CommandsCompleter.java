package org.apache.felix.karaf.gshell.console.completer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.karaf.gshell.console.Completer;
import org.apache.felix.karaf.gshell.console.CompletableFunction;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleContext;
import org.osgi.service.command.CommandProcessor;

public class CommandsCompleter implements Completer {

    private final Map<ServiceReference, Completer> completers = new ConcurrentHashMap<ServiceReference, Completer>();

    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void register(ServiceReference reference) {
        Set<String> functions = getNames(reference);
        if (functions != null) {
            List<Completer> cl = new ArrayList<Completer>();
            cl.add(new StringsCompleter(functions));
            try {
                Object function = bundleContext.getService(reference);
                if (function instanceof CompletableFunction) {
                    List<Completer> fcl = ((CompletableFunction) function).getCompleters();
                    if (fcl != null) {
                        for (Completer c : fcl) {
                            cl.add(c == null ? NullCompleter.INSTANCE : c);
                        }
                    } else {
                        cl.add(NullCompleter.INSTANCE);
                    }
                } else {
                    cl.add(NullCompleter.INSTANCE);
                }
            } finally {
                bundleContext.ungetService(reference);
            }
            ArgumentCompleter c = new ArgumentCompleter(cl);
            completers.put(reference, c);
        }
    }

    public void unregister(ServiceReference reference) {
        completers.remove(reference);
    }

    private Set<String> getNames(ServiceReference reference) {
        Set<String> names = new HashSet<String>();
        Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
        Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
        if(scope != null && function != null)
        {
            if (function.getClass().isArray())
            {
                for (Object f : ((Object[]) function))
                {
                    names.add(scope + ":" + f.toString());
                }
            }
            else
            {
                names.add(scope + ":" + function.toString());
            }
            return names;
        }
        return null;
    }

    public int complete(String buffer, int cursor, List<String> candidates) {
        int res =  new AggregateCompleter(completers.values()).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }
}


package org.apache.felix.dependencymanager.shell;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.felix.dependencymanager.ServiceComponent;
import org.apache.felix.dependencymanager.ServiceComponentDependency;
import org.apache.felix.shell.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class DMCommand implements Command {
    private final BundleContext m_context;

    public DMCommand(BundleContext context) {
        m_context = context;
    }

    public void execute(String line, PrintStream out, PrintStream err) {
        // lookup all dependency manager service components
        try {
            ServiceReference[] references = m_context.getServiceReferences(ServiceComponent.class.getName(), null);
            // show their state
            if (references != null) {
                Arrays.sort(references, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        ServiceReference r1 = (ServiceReference) o1;
                        ServiceReference r2 = (ServiceReference) o2;
                        long id1 = r1.getBundle().getBundleId();
                        long id2 = r2.getBundle().getBundleId();
                        return id1 > id2 ? 1 : -1;
                    }});
                for (int i = 0; i < references.length; i++) {
                    ServiceReference ref = references[i];
                    ServiceComponent sc = (ServiceComponent) m_context.getService(ref);
                    if (sc != null) {
                        String name = sc.getName();
                        int state = sc.getState();
                        out.println("[" + ref.getBundle().getBundleId() + "] " + ref.getBundle().getSymbolicName() + " " + name + " " + ServiceComponent.STATE_NAMES[state]);
                        if (line.indexOf("deps") != -1) {
                            ServiceComponentDependency[] dependencies = sc.getComponentDependencies();
                            if (dependencies != null) {
                                for (int j = 0; j < dependencies.length; j++) {
                                    ServiceComponentDependency dep = dependencies[j];
                                    String depName = dep.getName();
                                    String depType = dep.getType();
                                    int depState = dep.getState();
                                    out.println(" " + depName + " " + depType + " " + ServiceComponentDependency.STATE_NAMES[depState]);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (InvalidSyntaxException e) {
            // very weird since I'm not specifying a filter
            e.printStackTrace(err);
        }
    }

    public String getName() {
        return "dm";
    }

    public String getShortDescription() {
        return "list dependency manager component diagnostics.";
    }

    public String getUsage() {
        return "dm [deps]";
    }

}

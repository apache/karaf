package org.apache.felix.ipojo.arch.gogo;

import java.io.PrintStream;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.osgi.service.command.Descriptor;

@Component(public_factory=false, immediate=true)
@Instantiate
@Provides(specifications=Arch.class)
public class Arch {
    
    @ServiceProperty(name="osgi.command.scope")
    String m_scope = "ipojo";
    
    @ServiceProperty(name="osgi.command.function")
    String[] m_function = new String[] {
        "instances",
        "instance",
        "factory",
        "factories",
        "handlers"
    };
    
    @Requires(optional=true)
    private Architecture[] m_archs;
    
    @Requires(optional=true)
    private Factory[] m_factories;
    
    @Requires(optional=true)
    private HandlerFactory[] m_handlers;
    
    @Descriptor(description="Display iPOJO instances")
    public void instances() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < m_archs.length; i++) {
            InstanceDescription instance = m_archs[i].getInstanceDescription();
            if (instance.getState() == ComponentInstance.VALID) {
                buffer.append("Instance " + instance.getName() + " -> valid \n");
            }
            if (instance.getState() == ComponentInstance.INVALID) {
                buffer.append("Instance " + instance.getName() + " -> invalid \n");
            }
            if (instance.getState() == ComponentInstance.STOPPED) {
                buffer.append("Instance " + instance.getName() + " -> stopped \n");
            }
        }
        
        if (buffer.length() == 0) {
            buffer.append("No instances \n");
        }
        
        System.out.println(buffer.toString());   
    }
    
    @Descriptor(description="Display the architecture of a specific instance")
    public void instance(@Descriptor(description="target instance name") String instance) {
        for (int i = 0; i < m_archs.length; i++) {
            InstanceDescription id = m_archs[i].getInstanceDescription();
            if (id.getName().equalsIgnoreCase(instance)) {
                System.out.println(id.getDescription());
                return;
            }
        }
        System.err.println("Instance " + instance + " not found");
    }
    
    @Descriptor(description="Display the information about a specific factory")
    public void factory(@Descriptor(description="target factory") String factory) {
        boolean found = false;
        PrintStream out = System.out;
        
        for (int i = 0; i < m_factories.length; i++) {
            if (m_factories[i].getName().equalsIgnoreCase(factory)) {
                // Skip a line if already found (factory name not necessary unique)
                if (found) {
                    out.println();
                }
                out.println(m_factories[i].getDescription());
                found = true;
            }
        }
        
        if (! found) {
            System.err.println("Factory " + factory + " not found");
        }
    }
    
    @Descriptor(description="Display iPOJO factories")
    public void factories() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < m_factories.length; i++) {
            if (m_factories[i].getMissingHandlers().size() == 0) {
                buffer.append("Factory " + m_factories[i].getName() + " (VALID) \n");
            } else {
                buffer.append("Factory " + m_factories[i].getName() + " (INVALID : " + m_factories[i].getMissingHandlers() + ") \n");
            }
        }
        
        if (buffer.length() == 0) {
            buffer.append("No factories \n");
        }
        
        System.out.println(buffer.toString());
    }
    
    @Descriptor(description="Display iPOJO handlers")
    public void handlers() {
        PrintStream out = System.out;
        for (int i = 0; i < m_handlers.length; i++) {
            String name = m_handlers[i].getHandlerName();
            if ("composite".equals(m_handlers[i].getType())) {
                name = name + " [composite]";
            }
            if (m_handlers[i].getMissingHandlers().size() == 0) {
                out.println("Handler " + name + " (VALID)");
            } else {
                out.println("Handler " + name + " (INVALID : " + m_handlers[i].getMissingHandlers() + ")");
            }
        }
    }

}

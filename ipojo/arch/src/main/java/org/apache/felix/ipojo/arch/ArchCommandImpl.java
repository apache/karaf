/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.arch;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.shell.Command;

/**
 * Implementation of the arch command printing the actual architecture.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ArchCommandImpl implements Command {

    /** List of arch services. */
    private Architecture[] m_archs;
    
    /** Factory services. */
    private Factory[] m_factories;
    
    /** Handler Factories. */
    private HandlerFactory[] m_handlers;

    /**
     * Get the command name.
     * @return the command name (arch)
     * @see org.apache.felix.shell.Command#getName()
     */
    public String getName() {
        return "arch";
    }

    /**
     * Gets help message.
     * @return the command usage.
     * @see org.apache.felix.shell.Command#getUsage()
     */
    public String getUsage() {
        return "arch [-factories] [-instances] [-handlers] [-factory factory_name] [-instance instance_name]";
    }

    /**
     * Gets a small description.
     * @return get a description.
     * @see org.apache.felix.shell.Command#getShortDescription()
     */
    public String getShortDescription() {
        return "Architecture command : display the architecture";
    }

    /**
     * Executes the arch command.
     * @param line : command line
     * @param out : the default output stream
     * @param err : the error output stream
     * @see org.apache.felix.shell.Command#execute(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void execute(String line, PrintStream out, PrintStream err) {
        String line2 = line.substring("arch".length()).trim();

        if (line2.equalsIgnoreCase("-instances") || line2.length() == 0) {
            printInstances(out);
            return;
        }

        if (line2.equalsIgnoreCase("-factories")) {
            printFactories(out);
            return;
        }

        if (line2.startsWith("-factory")) {
            String name = line2.substring("-factory".length()).trim();
            printFactory(name, out, err);
            return;
        }

        if (line2.startsWith("-instance")) {
            String name = line2.substring("-instance".length()).trim();
            printInstance(name, out, err);
            return;
        }

        if (line2.startsWith("-handlers")) {
            printHandlers(out);
            return;
        }
        
        if (line2.startsWith("-stats")) {
            printStats(out);
            return;
        }

        err.println(getUsage());
    }
    
    /**
     * Prints the statistics.
     * @param out the out
     */
    private void printStats(PrintStream out) {
        try {
            Field field = IPojoFactory.class.getDeclaredField("INSTANCE_NAME");
            field.setAccessible(true); // The field is not accessible.
            List names = (List) field.get(null);
            out.println("Number of living instances : " + names.size());
            out.println("Created instances : " + names);
        } catch (SecurityException e) {
            out.println("Cannot compute stats : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            out.println("Cannot compute stats : " + e.getMessage());
        } catch (IllegalAccessException e) {
            out.println("Cannot compute stats : " + e.getMessage());
        } catch (NoSuchFieldException e) {
            out.println("Cannot compute stats : " + e.getMessage());
        }
        
    }

    /**
     * Prints instance list.
     * @param out : default print stream
     */
    private void printInstances(PrintStream out) {
        for (int i = 0; i < m_archs.length; i++) {
            InstanceDescription instance = m_archs[i].getInstanceDescription();
            if (instance.getState() == ComponentInstance.VALID) {
                out.println("Instance " + instance.getName() + " -> valid");
            }
            if (instance.getState() == ComponentInstance.INVALID) {
                out.println("Instance " + instance.getName() + " -> invalid");
            }
            if (instance.getState() == ComponentInstance.STOPPED) {
                out.println("Instance " + instance.getName() + " -> stopped");
            }
        }
    }
    
    /**
     * Prints instance description.
     * @param name : instance name
     * @param out : default print stream
     * @param err : error print stream (if the instance is not found)
     */
    private void printInstance(String name, PrintStream out, PrintStream err) {
        for (int i = 0; i < m_archs.length; i++) {
            InstanceDescription instance = m_archs[i].getInstanceDescription();
            if (instance.getName().equalsIgnoreCase(name)) {
                out.println(instance.getDescription());
                return;
            }
        }
        err.println("Instance " + name + " not found");
    }
    
    /**
     * Prints factories.
     * @param out : output stream
     */
    private void printFactories(PrintStream out) {
        for (int i = 0; i < m_factories.length; i++) {
            if (m_factories[i].getMissingHandlers().size() == 0) {
                out.println("Factory " + m_factories[i].getName() + " (VALID)");
            } else {
                out.println("Factory " + m_factories[i].getName() + " (INVALID : " + m_factories[i].getMissingHandlers() + ")");
            }
        }
    }
    
    /**
     * Prints factory description.
     * @param name : factory name
     * @param out : default print stream
     * @param err : error print stream (if the factory is not found)
     */
    private void printFactory(String name, PrintStream out, PrintStream err) {
        boolean found = false;
        for (int i = 0; i < m_factories.length; i++) {
            if (m_factories[i].getName().equalsIgnoreCase(name)) {
                // Skip a line if already found
                if (found) {
                    out.println();
                }
                out.println(m_factories[i].getDescription());
                found = true;
            }
        }
        if (! found) {
            err.println("Factory " + name + " not found");
        }
    }
    
    /**
     * Prints the list of available handlers (and validity).
     * @param out : default print stream
     */
    private void printHandlers(PrintStream out) {
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

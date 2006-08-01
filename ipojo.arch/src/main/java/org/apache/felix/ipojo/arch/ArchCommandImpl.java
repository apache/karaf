/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.arch;


import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.DependencyDescription;
import org.apache.felix.ipojo.architecture.ProvidedServiceDescription;
import org.apache.felix.ipojo.architecture.State;
import org.ungoverned.osgi.service.shell.Command;


/**
 * Implementation of the arch command printing the actual architecture.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ArchCommandImpl implements Command {

	/**
	 * List of archi service
	 */
	private Architecture archiService[];

    /**
     * @see org.ungoverned.osgi.service.shell.Command#getName()
     */
    public String getName() {
        return "arch";
    }

    /**
     * @see org.ungoverned.osgi.service.shell.Command#getUsage()
     */
    public String getUsage() {
        return "arch -> Dispaly architecture information";
    }

    /**
     * @see org.ungoverned.osgi.service.shell.Command#getShortDescription()
     */
    public String getShortDescription() {
        return "Architecture command : display the architecture";
    }

    /**
     * @see org.ungoverned.osgi.service.shell.Command#execute(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void execute(String line, PrintStream out, PrintStream err) {
        synchronized(this) { 
        	for(int i=0; i < archiService.length; i++) {
        		ComponentDescription component = archiService[i].getComponentDescription();       
        		out.println("Component : " + component.getClassName() + " - " + State.printComponentState(component.getState()));
        		for(int j = 0; j < component.getDependencies().length; j++) {
        			DependencyDescription dd = component.getDependencies()[j];
        			out.println("\t Dependency : " + dd.getInterface() + " - " + State.printDependencyState(dd.getState()) + " - Optional : " + dd.isOptional() + " - Multiple : " + dd.isMultiple());
        			// getUsedServices :
        			HashMap hm = dd.getUsedServices();
        			Iterator it = hm.keySet().iterator();
        			while(it.hasNext()) {
        				String key = (String) it.next();
        				out.println("\t\t Used Service : " + key + " - " + hm.get(key));
        			}
        		}
        		for(int j=0;  j < component.getProvideServices().length; j++) {
        			ProvidedServiceDescription ps = component.getProvideServices()[j];
        			String spec = "";
        			for(int k = 0; k < ps.getServiceSpecification().length; k++) {
        				spec = spec + " " + ps.getServiceSpecification()[k];
        			}
        			out.println("\t Provides : " + spec + " - " + State.printProvidedServiceState(ps.getState()));
        			Enumeration e = ps.getProperties().propertyNames();
        			while(e.hasMoreElements()) {
        				Object key = e.nextElement();
        				out.println("\t\t Service Property : " + key.toString() + " = " + ps.getProperties().getProperty(key.toString()));  
        			}
        		}
        		out.println("\tCreated Instances : ");
        		for(int j=0;  j < component.getInstances().length; j++) {
        			out.println("\t\t" + component.getInstances()[j]);
        		}
        		
        		out.print("\n");
        	}
        }
        
    }
}
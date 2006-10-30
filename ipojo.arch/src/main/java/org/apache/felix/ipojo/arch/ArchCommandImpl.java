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

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
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
     * Return the String corresponding to a component state.
     * @param state : the state in int
     * @return : the string of the state (Stopped, Unresolved, Resolved) or "Unknown" if state is not revelant
     */
    private String getComponentState(int state) {
        switch(state) {
        case(0) :
            return "STOPPED";
        case(1) :
            return "INVALID";
        case(2) :
            return  "VALID";
        default :
            return "UNKNOWN";
        }
    }

    /**
     * @see org.ungoverned.osgi.service.shell.Command#execute(java.lang.String, java.io.PrintStream, java.io.PrintStream)
     */
    public void execute(String line, PrintStream out, PrintStream err) {
        synchronized(this) { 
        	for(int i=0; i < archiService.length; i++) {
        		ComponentDescription component = archiService[i].getComponentDescription();       
        		out.println("Component : " + component.getClassName() + " - " + getComponentState(component.getState()) + " from bundle " + component.getBundleId());
        		for(int j = 0; j < component.getHandlers().length; j++) {
        			HandlerDescription hd = component.getHandlers()[j];
        			String hn = hd.getHandlerName();
        			String hv = "valid";
        			if(!hd.isValid()) { hv = "invalid"; }
        			String hi = hd.getHandlerInfo();
        			out.println("Handler : " + hn + " : " + hv);
        			if(!hi.equals("")) { out.println(hi); }
        		}
        		
        		out.println("Created Instances of the POJO : ");
        		for(int j=0;  j < component.getInstances().length; j++) {
        			out.println("\t" + component.getInstances()[j]);
        		}
        		
        		out.print("\n");
        	}
        }
        
    }
}

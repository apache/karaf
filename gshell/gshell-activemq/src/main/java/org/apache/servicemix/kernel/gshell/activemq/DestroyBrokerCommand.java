/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.kernel.gshell.activemq;

import java.io.File;

import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * 
 *
 * @version $Rev$ $Date$
 */
@CommandComponent(id="activemq:destroy-broker", description="Destroys a broker instance.")
public class DestroyBrokerCommand
    extends OsgiCommandSupport
{
	
    @Option(name="-n", aliases={"--name"}, description="The name of the broker (defaults to localhost).")
    private String name="localhost";
 
    protected Object doExecute() throws Exception {
    	
    	try {
    		String name = getName();    		
    		File base = new File(System.getProperty("servicemix.base"));
    		File deploy = new File(base, "deploy");
			File configFile = new File(deploy,name+"-broker.xml");

			configFile.delete();
			
			io.out.println("");
			io.out.println("Default ActiveMQ Broker ("+name+") configuration file created at: "+configFile.getPath()+" removed.");
			io.out.println("");
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

        return 0;
    }
	

	public String getName() {
		if( name ==  null ) {
    		File base = new File(System.getProperty("servicemix.base"));
    		name = base.getName();
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

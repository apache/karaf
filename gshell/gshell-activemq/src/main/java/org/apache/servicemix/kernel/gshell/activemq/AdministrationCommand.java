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

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.activemq.console.formatter.CommandShellOutputFormatter;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.common.Arguments;

/**
 *
 * @version $Rev$ $Date$
 */
public class AdministrationCommand implements Command
{

	private String description;
	private String id;
	private org.apache.activemq.console.command.Command command;

	public Object execute(CommandContext context, Object... objArgs) throws Exception {
		String[] args = Arguments.toStringArray(objArgs);
		org.apache.activemq.console.CommandContext context2 = new org.apache.activemq.console.CommandContext();
		context2.setFormatter(new CommandShellOutputFormatter(context.getIO().outputStream));
		try {
			command.setCommandContext(context2);
			command.execute(new ArrayList<String>(Arrays.asList(args)));
		} catch (Throwable e) {
			Throwable cur = e;
			while( cur.getCause()!=null ) {
				cur = cur.getCause();
			}
			if( cur instanceof java.net.ConnectException ){
				context2.print(
						"\n"+
						"Could not connect to JMX server.  This command requires that the remote JMX server be enabled.\n"+
						"This is typically done by adding the following JVM arguments: \n" +
						"   -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.authenticate=false \n" +
						"   -Dcom.sun.management.jmxremote.ssl=false \n" +
						"\n" +
						"The connection error was: "+cur+"\n");
			} else {
				if( e instanceof Exception ) {
					throw (Exception)e;
				} else {
					throw new RuntimeException(e);
				}
				
			}
		}
		return SUCCESS;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(String id) {
		this.id = id;
	}

	public org.apache.activemq.console.command.Command getCommand() {
		return command;
	}

	public void setCommand(org.apache.activemq.console.command.Command command) {
		this.command = command;
	}
	
 
}

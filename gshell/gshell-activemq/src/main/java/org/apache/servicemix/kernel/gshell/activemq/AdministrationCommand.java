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
			command.execute(Arrays.asList(args));
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

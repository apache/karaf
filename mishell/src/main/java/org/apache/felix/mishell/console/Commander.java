/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.felix.mishell.console;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.script.ScriptContext;



public class Commander extends HashSet<Command> implements Command{
	private Logger log=Logger.getLogger(this.getClass().getName());
	public void executeCommand(String cmd, PrintStream out) throws Exception {
		String[] parsedCmd=cmd.split(" ");
		if (parsedCmd.length==0)throw new CommandNotFoundException();
		for (Command c: this) {
			if (c.getName().equals(parsedCmd[0])){
				log.finest("executing: "+c.getName());
				c.executeCommand(cmd, out);
				return;
			}
		}
		throw new CommandNotFoundException();

	}
	public String getName() {
		return "commander";
	}
	public Commander() {
	}
	public String getHelp() {
		return "mishell commander";
	}
}

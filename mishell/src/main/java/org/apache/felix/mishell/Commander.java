package org.apache.felix.mishell;

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

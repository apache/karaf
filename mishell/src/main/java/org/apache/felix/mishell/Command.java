package org.apache.felix.mishell;

import java.io.PrintStream;

public interface Command {
	public void executeCommand(String cmd, PrintStream out) throws Exception;
	public String getName();
	public String getHelp();
}

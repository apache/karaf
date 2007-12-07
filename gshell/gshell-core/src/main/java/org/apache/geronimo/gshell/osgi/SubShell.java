package org.apache.geronimo.gshell.osgi;

import org.apache.geronimo.gshell.branding.Branding;
import org.apache.geronimo.gshell.command.CommandContext;

public interface SubShell {
	
	public Object execute(Branding branding, CommandContext context, Object... args) throws Exception;
	
}

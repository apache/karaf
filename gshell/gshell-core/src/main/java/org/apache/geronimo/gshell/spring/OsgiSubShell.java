package org.apache.geronimo.gshell.spring;

import jline.Terminal;

import org.apache.geronimo.gshell.DefaultEnvironment;
import org.apache.geronimo.gshell.DefaultShell;
import org.apache.geronimo.gshell.DefaultShellInfo;
import org.apache.geronimo.gshell.DefaultVariables;
import org.apache.geronimo.gshell.ExitNotification;
import org.apache.geronimo.gshell.branding.Branding;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.osgi.SubShell;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.shell.InteractiveShell;
import org.apache.geronimo.gshell.shell.ShellInfo;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

public class OsgiSubShell implements SubShell {

	private CommandRegistry commandRegistry;
	private OsgiLayoutManager osgiLayoutManager;
	private Terminal terminal;

	public Object execute(Branding branding, CommandContext context, Object... args) throws Exception {
		ShellContext oldContext = ShellContext.get();
		ShellContext ctx = new ShellContext();
		ShellContext.set(ctx);
		try {
			ctx.setIo(context.getIO());
			ctx.setCommandRegistry(commandRegistry);
			ctx.setTerminal(terminal);
			ctx.setBranding(branding);
			ctx.setLayoutManager(osgiLayoutManager.createLayoutManagerForShell(branding.getName()));
			
			// Would be nice to copy the env variables.. but can't cause it has imutable values
			// that the DefaultEnvironment tries to overwrite.
			Environment environment = new DefaultEnvironment(ctx.getIo(), new DefaultVariables());
			ctx.setEnvironment(environment);
	        IOTargetSource.setIO(ctx.getIo());
	        EnvironmentTargetSource.setEnvironment(environment);
			InteractiveShell interactiveShell = createInteractiveShell(ctx);
			if( args!=null && args.length>0 ) {
				return interactiveShell.execute(args);
			} else {
				try {
					interactiveShell.run();
				} catch (ExitNotification e) {
					return null;
				}
			}
			return null;
		} finally {
			ShellContext.set(oldContext);
		}
	}

	private InteractiveShell createInteractiveShell(ShellContext ctx) throws InitializationException {
		CommandExecutor executor = createCommandExecutor(ctx);
		ctx.setCommandExecutor(executor);
		ShellInfo shellInfo = createShellInfo(ctx.getBranding());
		ctx.setShellInfo(shellInfo);
		return new DefaultShell(ctx.getShellInfo(), ctx.getBranding(), ctx.getCommandExecutor(), terminal, ctx.getEnvironment(), ctx.getIo());
	}

	private CommandExecutor createCommandExecutor(ShellContext ctx) {
		SpringCommandExecutor executor = new SpringCommandExecutor();
		executor.setEnv(ctx.getEnvironment());
		executor.setLayoutManager(ctx.getLayoutManager());
		SpringCommandLineBuilder commandLineBuilder = new SpringCommandLineBuilder();
		commandLineBuilder.setEnvironment(ctx.getEnvironment());
		commandLineBuilder.setExecutor(executor);
		executor.setCommandLineBuilder(commandLineBuilder);
		executor.setCommandRegistry(commandRegistry);
		executor.init();
		return executor;
	}

	private ShellInfo createShellInfo(Branding branding) throws InitializationException {
		DefaultShellInfo rc = new DefaultShellInfo(branding);
		rc.initialize();
		return rc;
	}

	public Terminal getTerminal() {
		return terminal;
	}

	public void setTerminal(Terminal terminal) {
		this.terminal = terminal;
	}

	public OsgiLayoutManager getOsgiLayoutManager() {
		return osgiLayoutManager;
	}

	public void setOsgiLayoutManager(OsgiLayoutManager osgiLayoutManager) {
		this.osgiLayoutManager = osgiLayoutManager;
	}

	public CommandRegistry getCommandRegistry() {
		return commandRegistry;
	}

	public void setCommandRegistry(CommandRegistry commandRegistry) {
		this.commandRegistry = commandRegistry;
	}


}

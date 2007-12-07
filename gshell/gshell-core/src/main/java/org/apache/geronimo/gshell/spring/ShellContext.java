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
package org.apache.geronimo.gshell.spring;

import jline.Terminal;

import org.apache.geronimo.gshell.branding.Branding;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.shell.ShellInfo;

public class ShellContext {
	
	static private final ThreadLocal<ShellContext> context = new ThreadLocal<ShellContext>();
	
	static public ShellContext get() {
		return context.get();
	}
	
	static public void set(ShellContext ctx) {
		context.set(ctx);
	}
	
	private CommandRegistry commandRegistry;
	private LayoutManager layoutManager;
	private Branding branding;
	private IO io;
	private Environment environment;
	private CommandExecutor commandExecutor;
	private Terminal terminal;
	private ShellInfo shellInfo;

	public CommandRegistry getCommandRegistry() {
		return commandRegistry;
	}

	public void setCommandRegistry(CommandRegistry commandRegistry) {
		this.commandRegistry = commandRegistry;
	}

	public LayoutManager getLayoutManager() {
		return layoutManager;
	}

	public void setLayoutManager(LayoutManager layoutManager) {
		this.layoutManager = layoutManager;
	}

	public Branding getBranding() {
		return branding;
	}

	public void setBranding(Branding branding) {
		this.branding = branding;
	}

	public void setIo(IO io) {
		this.io = io;
	}

	public IO getIo() {
		return io;
	}

	public void setCommandExecutor(CommandExecutor commandExecutor) {
		this.commandExecutor = commandExecutor;
	}
	public CommandExecutor getCommandExecutor() {
		return commandExecutor;
	}

	public void setTerminal(Terminal terminal) {
		this.terminal = terminal;
	}
	public Terminal getTerminal() {
		return terminal;
	}

	public void setShellInfo(ShellInfo shellInfo) {
		this.shellInfo = shellInfo;
	}
	public ShellInfo getShellInfo() {
		return shellInfo;
	}

	public void setEnvironment(Environment environment) {
		this.environment=environment;
	}

	public Environment getEnvironment() {
		return environment;
	}


	
	
}

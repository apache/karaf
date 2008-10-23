package org.apache.servicemix.kernel.gshell.core;

import java.util.Map;

import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.springframework.beans.factory.annotation.Autowired;

public class OsgiCommandRegistry {

    public static final String NAME = "name";

    private CommandRegistry commandRegistry;

    public OsgiCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void register(final Command command, Map<String, ?> properties) throws Exception {
        commandRegistry.registerCommand(command);
    }

    public void unregister(final Command command, Map<String, ?> properties) throws Exception {
        commandRegistry.removeCommand(command);
    }

}

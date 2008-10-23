package org.apache.servicemix.kernel.gshell.core;

import java.util.Map;

import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.springframework.beans.factory.annotation.Autowired;

public class OsgiCommandRegistry {

    public static final String NAME = "name";

    @Autowired
    private CommandRegistry commandRegistry;

    public void register(final Command command, Map<String, ?> properties) throws Exception {
        String name = (String) properties.get(NAME);
        commandRegistry.registerCommand(name, command);
    }

    public void unregister(final Command command, Map<String, ?> properties) throws Exception {
        String name = (String) properties.get(NAME);
        commandRegistry.removeCommand(name);
    }

}

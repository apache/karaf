package org.apache.servicemix.kernel.gshell.core;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.AliasRegistry;
import org.osgi.framework.BundleContext;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.registry.CommandRegistry;

public class CommandBundle implements BundleContextAware, InitializingBean, DisposableBean {

    @Autowired(required = false)
    private CommandRegistry commandRegistry;

    @Autowired(required = false)
    private AliasRegistry aliasRegistry;

    private BundleContext bundleContext;

    private Map<String,Command> commands;

    private Map<String,String> aliases;

    public CommandBundle() {
    }

    public Map<String, Command> getCommands() {
        return commands;
    }

    public void setCommands(final Map<String, Command> commands) {
        assert commands != null;

        this.commands = commands;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public void setAliases(final Map<String, String> aliases) {
        assert aliases != null;

        this.aliases = aliases;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void afterPropertiesSet() throws Exception {
        System.err.println("CommandBundle: init");
        if (commandRegistry != null) {
            System.err.println("Using the auto wired command registry");
            for (String name : commands.keySet()) {
                System.err.println("Registering command: " + name);
                commandRegistry.registerCommand(name, commands.get(name));
            }
        }
        if (aliasRegistry != null) {
            System.err.println("Using the auto wired alias registry");
            for (String name : aliases.keySet()) {
                aliasRegistry.registerAlias(name, aliases.get(name));
            }
        }
    }

    public void destroy() {
        System.err.println("CommandBundle: init");
    }

}

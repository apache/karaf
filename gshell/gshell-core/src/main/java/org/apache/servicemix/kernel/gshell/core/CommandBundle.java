package org.apache.servicemix.kernel.gshell.core;

import java.util.Map;
import java.util.Dictionary;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.registry.AliasRegistry;
import org.apache.geronimo.gshell.wisdom.command.CommandSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandBundle implements BundleContextAware, InitializingBean, DisposableBean, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private CommandRegistry commandRegistry;

    private AliasRegistry aliasRegistry;

    private BundleContext bundleContext;

    private List<Command> commands;

    private Map<String,String> aliases;

    private ApplicationContext applicationContext;

    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

    public CommandBundle() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(final List<Command> commands) {
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
        log.debug("Initializing command bundle");
        if (commandRegistry == null) {
            String[] names = applicationContext.getBeanNamesForType(CommandRegistry.class);
            if (names.length == 1) {
                commandRegistry = (CommandRegistry) applicationContext.getBean(names[0], CommandRegistry.class);
            }
        }
        if (aliasRegistry == null) {
            String[] names = applicationContext.getBeanNamesForType(AliasRegistry.class);
            if (names.length == 1) {
                aliasRegistry = (AliasRegistry) applicationContext.getBean(names[0], AliasRegistry.class);
            }
        }
        if (commandRegistry != null && aliasRegistry != null) {
            log.debug("Command bundle is using the auto wired command/alias registry");
            for (Command command : commands) {
                log.debug("Registering command: {}", command.getLocation());
                commandRegistry.registerCommand(command);
            }
            for (String name : aliases.keySet()) {
                log.debug("Registering alias: {}", name);
                aliasRegistry.registerAlias(name, aliases.get(name));
            }
        } else if (bundleContext != null) {
            if (aliases != null && aliases.size() > 0) {
                throw new Exception("Aliases are not supported in OSGi");
            }
            log.debug("Command bundle is using the OSGi registry");
            for (Command command : commands) {
                log.debug("Registering command: {}", command.getLocation());
                Dictionary props = new Properties();
                props.put(OsgiCommandRegistry.NAME, command.getLocation().getFullPath());
                registrations.add(bundleContext.registerService(Command.class.getName(), command, props));
            }
        } else {
            throw new Exception("Command bundle should be wired to the command/alias registry or be used in an OSGi context");
        }
    }

    public void destroy() {
        log.debug("Destroying command bundle");
        for (ServiceRegistration reg : registrations) {
            reg.unregister();
        }
    }

}

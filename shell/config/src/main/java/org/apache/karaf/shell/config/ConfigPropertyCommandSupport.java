package org.apache.karaf.shell.config;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.karaf.shell.commands.Option;

/**
 * Abstract class which commands that are related to property processing should extend.
 */
public abstract class ConfigPropertyCommandSupport extends ConfigCommandSupport {

    @Option(name = "-p", aliases = "--pid", description = "The configuration pid", required = false, multiValued = false)
    protected String pid;

    @Option(name = "-b", aliases = { "--bypass-storage" }, multiValued = false, required = false, description = "Do not store the configuration in a properties file, but feed it directly to ConfigAdmin")
    protected boolean bypassStorage;


    @SuppressWarnings("rawtypes")
    protected Object doExecute() throws Exception {
        Dictionary props = getEditedProps();
        if (props == null && pid == null) {
            System.err.println("No configuration is being edited--run the edit command first");
        } else {
            if (props == null) {
                props = new Properties();
            }
            propertyAction(props);
            if(requiresUpdate(pid)) {
                this.configRepository.update(pid, props, bypassStorage);
            }
        }
        return null;
    }

    /**
     * Perform an action on the properties.
     * @param props
     */
    @SuppressWarnings("rawtypes")
    protected abstract void propertyAction(Dictionary props);

    /**
     * Checks if the configuration requires to be updated.
     * The default behavior is to update if a valid pid has been passed to the method.
     * @param pid
     * @return
     */
    protected boolean requiresUpdate(String pid) {
        if (pid != null) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Retrieves confguration from the pid, if used or delegates to session from getting the configuration.
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected Dictionary getEditedProps() throws Exception {
        Dictionary props = this.configRepository.getConfigProperties(pid);
        return (props != null) ? props : super.getEditedProps();
    }
}

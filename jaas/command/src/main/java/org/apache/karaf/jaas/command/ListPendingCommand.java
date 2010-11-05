package org.apache.karaf.jaas.command;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.Queue;

/**
 * @author iocanel
 */

/**
 * Lists the commands the are in the command queue, for the active realm/module.
 *
 * @author iocanel
 */
@Command(scope = "jaas", name = "pending", description = "Lists the modification on the active realm/module.")
public class ListPendingCommand extends JaasCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);
        Queue<JaasCommandSupport> commandQueue = (Queue<JaasCommandSupport>) session.get(JAAS_CMDS);

        if (realm != null && entry != null) {
            String moduleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
            System.out.println(String.format("Jaas Realm:%s Jaas Module:%s", realm.getName(), moduleClass));

            if (commandQueue != null && !commandQueue.isEmpty()) {
                for (JaasCommandSupport command : commandQueue) {
                    System.out.println(command);
                }
            } else {
                System.err.println("No JAAS command¾ in queue.");
            }
        } else {
            System.err.println("No JAAS Realm / Module has been selected.");
        }
        return null;
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }
}
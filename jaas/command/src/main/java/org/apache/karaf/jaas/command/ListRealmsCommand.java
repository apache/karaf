package org.apache.karaf.jaas.command;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.List;

@Command(scope = "jaas", name = "list-realm", description = "Lists the modification on the active realm/module.")
public class ListRealmsCommand extends JaasCommandSupport {

    private static final String REALM_LIST_FORMAT = "%5s %-20s %-80s";

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }

    protected Object doExecute() throws Exception {
        List<JaasRealm> realms = getRealms();
        if (realms != null && realms.size() > 0) {
            System.out.println(String.format(REALM_LIST_FORMAT, "Index","Realm", "Module Class"));
            int index = 1;
            for (JaasRealm realm : realms) {
                String realmName = realm.getName();
                AppConfigurationEntry[] entries = realm.getEntries();

                if (entries != null && entries.length > 0) {
                    for (int i = 0; i < entries.length; i++) {
                        String moduleClass = (String) entries[i].getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
                        System.out.println(String.format(REALM_LIST_FORMAT, index++, realmName, moduleClass));
                    }
                } else {
                    System.out.println(String.format(REALM_LIST_FORMAT, realmName, "No module found for realm."));
                }
            }
        } else {
            System.err.println("No realm found");
        }
        return null;
    }

}

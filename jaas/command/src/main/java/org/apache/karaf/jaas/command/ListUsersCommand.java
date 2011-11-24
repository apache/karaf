package org.apache.karaf.jaas.command;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.UserPrincipal;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.List;

@Command(scope = "jaas", name = "list-user", description = "Lists the users of the active realm/module.")
public class ListUsersCommand extends JaasCommandSupport {

    private static final String OUTPUT_FORMAT = "%-20s %-20s";

    @Override
    protected Object doExecute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);

        if (realm == null || entry == null) {
            System.err.println("No JAAS Realm / Module has been selected.");
            return null;
        }

        BackingEngine engine = backingEngineService.get(entry);

        if (engine == null) {
            System.err.println(String.format("Failed to resolve backing engine for realm:%s and moudle:%s", realm.getName(), entry.getLoginModuleName()));
            return null;
        }

        return doExecute(engine);
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        List<UserPrincipal> users = engine.listUsers();
        System.out.println(String.format(OUTPUT_FORMAT, "User Name", "Role"));

        for (UserPrincipal user : users) {
            String userName = user.getName();
            List<RolePrincipal> roles = engine.listRoles(user);

            if (roles != null && roles.size() >= 1) {
                for (RolePrincipal role : roles) {
                    String roleName = role.getName();
                    System.out.println(String.format(OUTPUT_FORMAT, userName, roleName));
                }
            } else {
                System.out.println(String.format(OUTPUT_FORMAT, userName, ""));
            }

        }
        return null;
    }

}

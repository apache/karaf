package org.apache.karaf.jaas.command;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.config.impl.Config;
import org.apache.karaf.jaas.config.impl.Module;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.properties.PropertiesLoginModule;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.util.config.PropertiesLoader;
import org.osgi.framework.BundleContext;

import java.util.List;
import java.util.Properties;

@Command(scope = "jaas", name = "realm-add", description = "Add a realm")
@Service
public class RealmAddCommand extends JaasCommandSupport {

    @Reference
    private BundleContext context;

    @Argument(index = 0, name = "realmname", description = "Realm Name", required = true, multiValued = false)
    private String realmname;

    @Argument(index = 1, name = "loginModule", description = "Class Name of Login Module", required = true, multiValued = false)
    private String loginModule;

    @Argument(index = 2, name = "properties", description = "Pair of Properties (key value)", required = false, multiValued = true)
    private List<String> propertiesList;

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }

    @Override
    public Object execute() throws Exception {
        Module initialModule = ModuleAddCommand.createModuleFromCmdParameters(loginModule, propertiesList);
        if (initialModule == null) {
            // If we could not identify a Module we cannot create a realm => exit (sys.err was already written)
            return null;
        }
        // Create realm
        Config realm = new Config();
        realm.setName(realmname);
        realm.setModules(new Module[]{initialModule});
        realm.setBundleContext(context);

        context.registerService(JaasRealm.class, realm, null);
        return null;
    }

    public BundleContext getContext() {
        return context;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public String getRealmname() {
        return realmname;
    }

    public void setRealmname(String realmname) {
        this.realmname = realmname;
    }

    @Override
    public String toString() {
        return "RealmAddCommand{" +
            "realmname='" + realmname + '\'' +
            '}';
    }
}

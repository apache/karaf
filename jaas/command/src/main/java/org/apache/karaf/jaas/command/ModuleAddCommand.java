package org.apache.karaf.jaas.command;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.config.impl.Config;
import org.apache.karaf.jaas.config.impl.Module;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Command(scope = "jaas", name = "module-add", description = "Add a Login Module")
@Service
public class ModuleAddCommand extends JaasCommandSupport {

    @Argument(index = 0, name = "loginModule", description = "Class Name of Login Module", required = true, multiValued = false)
    private String loginModule;

    @Argument(index = 1, name = "properties", description = "Pair of Properties (key value)", required = false, multiValued = true)
    private List<String> propertiesList;

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }

    @Override
    public Object execute() throws Exception {
        // Fetch Realm
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);

        if (realm == null) {
            System.err.println("No JAAS Realm has been selected");
            return null;
        }
        if (!(realm instanceof Config)) {
            System.err.println("Selected JAAS Realm was not added via jaas:add-realm, only those are supported!");
            return null;
        }

        if (!checkIfClassExists(loginModule)) {
            System.err.println("Module class '" + loginModule + "' is unknown!");
            return null;
        }
        Module module = createModuleFromCmdParameters(loginModule, propertiesList);

        // Add the Login Module to the current Realm
        List<Module> modulesList = new ArrayList<>(Arrays.asList(((Config) realm).getModules()));
        modulesList.add(module);
        Module[] newModules = modulesList.toArray(new Module[]{});
        ((Config) realm).setModules(newModules);
        return null;
    }

    /**
     * Parses the Command Line Parameters given to create a valid Module and Properties from it.
     * @param loginModule Class Name of the login Module
     * @param propertiesList List of Properties interpreted as "key1 value1 key2 value2"
     * @return Module
     */
    static Module createModuleFromCmdParameters(String loginModule, List<String> propertiesList) {
        // Parse Properties
        if (propertiesList != null && propertiesList.size() > 0 && (propertiesList.size() % 2) == 1) {
            // Properties are uneven... bad
            System.err.println("Properties have to be given as \"key1 value1 key2 value2 ...\" but number of Arguments is uneven!");
            return null;
        }
        Properties properties = new Properties();
        if (propertiesList != null) {
            for (int i = 0; i < propertiesList.size(); i += 2) {
                properties.put(propertiesList.get(i), propertiesList.get(i + 1));
            }
        }
        // Assemble Login Module
        Module module = new Module();
        module.setClassName(loginModule);
        module.setFlags("required");
        module.setOptions(properties);
        return module;
    }

    public String getLoginModule() {
        return loginModule;
    }

    public void setLoginModule(String loginModule) {
        this.loginModule = loginModule;
    }

    public List<String> getPropertiesList() {
        return propertiesList;
    }

    public void setPropertiesList(List<String> propertiesList) {
        this.propertiesList = propertiesList;
    }

    private boolean checkIfClassExists(String loginModule) {
        try {
            this.getClass().getClassLoader().loadClass(loginModule);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

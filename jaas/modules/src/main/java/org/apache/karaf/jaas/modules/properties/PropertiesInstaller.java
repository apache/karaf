package org.apache.karaf.jaas.modules.properties;

import java.io.File;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.utils.properties.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesInstaller implements ArtifactInstaller {
    
    private final Logger LOGGER = LoggerFactory.getLogger(PropertiesInstaller.class);
    
    private String usersFileName;
    
    private File usersFile;
    
        
    PropertiesLoginModule propertiesLoginModule;

    
    public PropertiesInstaller(PropertiesLoginModule propertiesLoginModule, String usersFile) {
        this.propertiesLoginModule = propertiesLoginModule;
        this.usersFileName = usersFile;
    }

    public boolean canHandle(File artifact) {
        if (usersFile == null) {
            usersFile = new File(usersFileName);
        }
        return artifact.getName().endsWith(usersFile.getName());
    }

    public void install(File artifact) throws Exception {
        if (usersFile == null) {
            usersFile = new File(usersFileName);
        }
        Properties userProperties = new Properties(usersFile);
        this.propertiesLoginModule.encryptedPassword(userProperties);
    }

    public void update(File artifact) throws Exception {
        if (usersFile == null) {
            usersFile = new File(usersFileName);
        }
        Properties userProperties = new Properties(usersFile);
        this.propertiesLoginModule.encryptedPassword(userProperties);
    }

    public void uninstall(File artifact) throws Exception {
        LOGGER.warn("the users.properties was removed");
    }

}

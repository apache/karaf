package org.apache.karaf.dev.core.internal;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.wrap.ServiceConstants;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenConfigService {
	private final Logger logger = LoggerFactory.getLogger(BundleWatcherImpl.class);
	private final ConfigurationAdmin configurationAdmin;

	public MavenConfigService(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
	}

    public File getLocalRepository() {
        // Attempt to retrieve local repository location from MavenConfiguration
        MavenConfiguration configuration = retrieveMavenConfiguration();
        if (configuration != null) {
            MavenRepositoryURL localRepositoryURL = configuration.getLocalRepository();
            if (localRepositoryURL != null) {
                return localRepositoryURL.getFile().getAbsoluteFile();
            }
        }
        // If local repository not found assume default.
        String localRepo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        return new File(localRepo).getAbsoluteFile();
    }

    private MavenConfiguration retrieveMavenConfiguration() {
        MavenConfiguration mavenConfiguration = null;
        try {
            Configuration configuration = configurationAdmin.getConfiguration(ServiceConstants.PID);
            if (configuration != null) {
                @SuppressWarnings("rawtypes")
				Dictionary dictonary = configuration.getProperties();
                if (dictonary != null) {
                    DictionaryPropertyResolver resolver = new DictionaryPropertyResolver(dictonary);
                    mavenConfiguration = new MavenConfigurationImpl(resolver, ServiceConstants.PID);
                }
            }
        } catch (IOException e) {
            logger.error("Error retrieving maven configuration", e);
        }
        return mavenConfiguration;
    }
}

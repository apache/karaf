package org.apache.karaf.tooling.exam.container.internal;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.karaf.tooling.exam.options.KarafDistributionBaseConfigurationOption;
import org.apache.karaf.tooling.exam.options.KarafDistributionConfigurationOption;

/**
 * Extends the {@link KarafDistributionConfigurationOption} to add functionality to store those values also in a
 * distribution.info file in the distirbution.
 */
public class InternalKarafDistributionConfigurationOption extends KarafDistributionConfigurationOption {

    private static final String NAME = "name";
    private static final String KARAF_VERSION = "karafVersion";

    private File distributionInfo;

    public InternalKarafDistributionConfigurationOption(
            KarafDistributionBaseConfigurationOption distributionConfigurationOption, File distributionInfo) {
        super(distributionConfigurationOption);
        this.distributionInfo = distributionInfo;
    }

    @Override
    public String getKarafVersion() {
        String internalVersion = super.getKarafVersion();
        if (internalVersion != null && internalVersion.length() != 0) {
            return internalVersion;
        }
        if (!distributionInfo.exists()) {
            throw new IllegalStateException(
                "Either distribution.info or the property itself has to define a karaf version.");
        }
        String retrieveProperty = retrieveProperty(KARAF_VERSION);
        if (retrieveProperty == null || retrieveProperty.length() == 0) {
            throw new IllegalStateException(
                "Either distribution.info or the property itself has to define a karaf version.");
        }
        return retrieveProperty;
    }

    @Override
    public String getName() {
        String internalName = super.getName();
        if (internalName != null && internalName.length() != 0) {
            return internalName;
        }
        if (!distributionInfo.exists()) {
            throw new IllegalStateException(
                "Either distribution.info or the property itself has to define a name for the distribution..");
        }
        String retrieveProperty = retrieveProperty(NAME);
        if (retrieveProperty == null || retrieveProperty.length() == 0) {
            throw new IllegalStateException(
                "Either distribution.info or the property itself has to define a name for the distribution..");
        }
        return retrieveProperty;
    }

    private String retrieveProperty(String key) {
        try {
            FileInputStream fileInputStream = new FileInputStream(distributionInfo);
            try {
                Properties props = new Properties();
                props.load(fileInputStream);
                return props.getProperty(key);
            } finally {
                fileInputStream.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}

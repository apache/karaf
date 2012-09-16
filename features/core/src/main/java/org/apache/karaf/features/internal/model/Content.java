package org.apache.karaf.features.internal.model;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlTransient;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.ConfigFileInfo;

@XmlTransient
public class Content {

    protected List<Config> config;
    protected List<ConfigFile> configfile;
    protected List<Dependency> feature;
    protected List<Bundle> bundle;

    /**
     * Gets the value of the config property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the config property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConfig().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Config }
     */
    public List<Config> getConfig() {
        if (config == null) {
            config = new ArrayList<Config>();
        }
        return this.config;
    }

    /**
     * Gets the value of the configfile property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the configfile property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConfigfile().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link ConfigFile }
     */
    public List<ConfigFile> getConfigfile() {
        if (configfile == null) {
            configfile = new ArrayList<ConfigFile>();
        }
        return this.configfile;
    }

    /**
     * Gets the value of the feature property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the feature property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFeature().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Dependency }
     */
    public List<Dependency> getFeature() {
        if (feature == null) {
            feature = new ArrayList<Dependency>();
        }
        return this.feature;
    }

    /**
     * Gets the value of the bundle property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bundle property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBundle().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Bundle }
     */
    public List<Bundle> getBundle() {
        if (bundle == null) {
            bundle = new ArrayList<Bundle>();
        }
        return this.bundle;
    }

    public List<org.apache.karaf.features.Dependency> getDependencies() {
        return Collections.<org.apache.karaf.features.Dependency>unmodifiableList(getFeature());
    }

    public List<BundleInfo> getBundles() {
        return Collections.<BundleInfo>unmodifiableList(getBundle());
    }

    public Map<String, Map<String, String>> getConfigurations() {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        for (Config config : getConfig()) {
            String name = config.getName();
            StringReader propStream = new StringReader(config.getValue());
            Properties props = new Properties();
            try {
                props.load(propStream);
            } catch (IOException e) {
                //ignore??
            }
            interpolation(props);
            Map<String, String> propMap = new HashMap<String, String>();
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                propMap.put((String) entry.getKey(), (String) entry.getValue());
            }
            result.put(name, propMap);
        }
        return result;
    }

    public List<ConfigFileInfo> getConfigurationFiles() {
        return Collections.<ConfigFileInfo>unmodifiableList(getConfigfile());
    }

    protected void interpolation(Properties properties) {
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String val = properties.getProperty(key);
            Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(val);
            while (matcher.find()) {
                String rep = System.getProperty(matcher.group(1));
                if (rep != null) {
                    val = val.replace(matcher.group(0), rep);
                    matcher.reset(val);
                }
            }
            properties.put(key, val);
        }
    }
}

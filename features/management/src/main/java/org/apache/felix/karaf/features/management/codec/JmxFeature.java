/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.features.management.codec;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;

import org.apache.felix.karaf.features.Feature;
import org.apache.felix.karaf.features.management.FeaturesServiceMBean;

public class JmxFeature {

    /**
     * The CompositeType which represents a single feature
     */
    public final static CompositeType FEATURE;

    /**
     * The TabularType which represents a list of features
     */
    public final static TabularType FEATURE_TABLE;

    public final static CompositeType FEATURE_IDENTIFIER;

    public final static TabularType FEATURE_IDENTIFIER_TABLE;

    public final static CompositeType FEATURE_CONFIG_ELEMENT;

    public final static TabularType FEATURE_CONFIG_ELEMENT_TABLE;

    public final static CompositeType FEATURE_CONFIG;

    public final static TabularType FEATURE_CONFIG_TABLE;


    private final CompositeData data;

    public JmxFeature(Feature feature, boolean installed) {
        try {
            String[] itemNames = FeaturesServiceMBean.FEATURE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = feature.getName();
            itemValues[1] = feature.getVersion();
            itemValues[2] = getFeatureIdentifierTable(feature.getDependencies());
            itemValues[3] = feature.getBundles().toArray(new String[feature.getBundles().size()]);
            itemValues[4]  = getConfigTable(feature.getConfigurations());
            itemValues[5] = installed;
            data = new CompositeDataSupport(FEATURE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(Collection<JmxFeature> features) {
        TabularDataSupport table = new TabularDataSupport(FEATURE_TABLE);
        for (JmxFeature feature : features) {
            table.put(feature.asCompositeData());
        }
        return table;
    }

    static TabularData getFeatureIdentifierTable(List<Feature> features) throws OpenDataException {
        TabularDataSupport table = new TabularDataSupport(FEATURE_IDENTIFIER_TABLE);
        for (Feature feature : features) {
            String[] itemNames = new String[] { FeaturesServiceMBean.FEATURE_NAME, FeaturesServiceMBean.FEATURE_VERSION };
            Object[] itemValues = new Object[] { feature.getName(), feature.getVersion() };
            CompositeData ident = new CompositeDataSupport(FEATURE_IDENTIFIER, itemNames, itemValues);
            table.put(ident);
        }
        return table;
    }

    static TabularData getConfigTable(Map<String, Map<String, String>> configs) throws OpenDataException {
        TabularDataSupport table = new TabularDataSupport(FEATURE_CONFIG_TABLE);
        for (Map.Entry<String, Map<String, String>> entry : configs.entrySet()) {
            String[] itemNames = FeaturesServiceMBean.FEATURE_CONFIG;
            Object[] itemValues = new Object[2];
            itemValues[0] = entry.getKey();
            itemValues[1] = getConfigElementTable(entry.getValue());
            CompositeData config = new CompositeDataSupport(FEATURE_CONFIG, itemNames, itemValues);
            table.put(config);
        }
        return table;
    }

    static TabularData getConfigElementTable(Map<String, String> config) throws OpenDataException {
        TabularDataSupport table = new TabularDataSupport(FEATURE_CONFIG_ELEMENT_TABLE);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String[] itemNames = FeaturesServiceMBean.FEATURE_CONFIG_ELEMENT;
            Object[] itemValues = { entry.getKey(), entry.getValue() };
            CompositeData element = new CompositeDataSupport(FEATURE_CONFIG_ELEMENT, itemNames, itemValues);
            table.put(element);
        }
        return table;
    }


    static {
        FEATURE_IDENTIFIER = createFeatureIdentifierType();
        FEATURE_IDENTIFIER_TABLE = createFeatureIdentifierTableType();
        FEATURE_CONFIG_ELEMENT = createFeatureConfigElementType();
        FEATURE_CONFIG_ELEMENT_TABLE = createFeatureConfigElementTableType();
        FEATURE_CONFIG = createFeatureConfigType();
        FEATURE_CONFIG_TABLE = createFeatureConfigTableType();
        FEATURE = createFeatureType();
        FEATURE_TABLE = createFeatureTableType();
    }

    private static CompositeType createFeatureIdentifierType() {
        try {
            String description = "This type identify a Karaf features";
            String[] itemNames = FeaturesServiceMBean.FEATURE_IDENTIFIER;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;

            itemDescriptions[0] = "The id of the feature";
            itemDescriptions[1] = "The version of the feature";

            return new CompositeType("FeatureIdentifier", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build featureIdentifier type", e);
        }
    }

    private static TabularType createFeatureIdentifierTableType() {
        try {
            return new TabularType("Features", "The table of featureIdentifiers",
                    FEATURE_IDENTIFIER, new String[] { FeaturesServiceMBean.FEATURE_NAME, FeaturesServiceMBean.FEATURE_VERSION });
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build featureIdentifier table type", e);
        }
    }

    private static CompositeType createFeatureConfigElementType() {
        try {
            String description = "This type encapsulates Karaf feature config element";
            String[] itemNames = FeaturesServiceMBean.FEATURE_CONFIG_ELEMENT;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;

            itemDescriptions[0] = "The key";
            itemDescriptions[1] = "The value";

            return new CompositeType("ConfigElement", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build configElement type", e);
        }
    }

    private static TabularType createFeatureConfigElementTableType() {
        try {
            return new TabularType("ConfigElement", "The table of configurations elements",
                    FEATURE_CONFIG_ELEMENT, new String[] { FeaturesServiceMBean.FEATURE_CONFIG_ELEMENT_KEY});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build feature table type", e);
        }
    }

    private static CompositeType createFeatureConfigType() {
        try {
            String description = "This type encapsulates Karaf feature config";
            String[] itemNames = FeaturesServiceMBean.FEATURE_CONFIG;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = FEATURE_CONFIG_ELEMENT_TABLE;

            itemDescriptions[0] = "The PID of the config";
            itemDescriptions[1] = "The configuration elements";

            return new CompositeType("Config", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build configElement type", e);
        }
    }

    private static TabularType createFeatureConfigTableType() {
        try {
            return new TabularType("Features", "The table of configurations",
                    FEATURE_CONFIG, new String[] { FeaturesServiceMBean.FEATURE_CONFIG_PID});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build feature table type", e);
        }
    }

    private static CompositeType createFeatureType() {
        try {
            String description = "This type encapsulates Karaf features";
            String[] itemNames = FeaturesServiceMBean.FEATURE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = FEATURE_IDENTIFIER_TABLE;
            itemTypes[3] = new ArrayType(1, SimpleType.STRING);
            itemTypes[4] = FEATURE_CONFIG_TABLE;
            itemTypes[5] = SimpleType.BOOLEAN;

            itemDescriptions[0] = "The name of the feature";
            itemDescriptions[1] = "The version of the feature";
            itemDescriptions[2] = "The feature dependencies";
            itemDescriptions[3] = "The feature bundles";
            itemDescriptions[4] = "The feature configurations";
            itemDescriptions[5] = "Whether the feature is installed";

            return new CompositeType("Feature", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build feature type", e);
        }
    }

    private static TabularType createFeatureTableType() {
        try {
            return new TabularType("Features", "The table of all features",
                    FEATURE, new String[] { FeaturesServiceMBean.FEATURE_NAME, FeaturesServiceMBean.FEATURE_VERSION });
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build feature table type", e);
        }
    }

}

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
package org.apache.karaf.features.management.codec;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.management.FeaturesServiceMBean;

public class JmxRepository {

    public static final CompositeType REPOSITORY;

    public static final TabularType REPOSITORY_TABLE;

    private final CompositeData data;

    public JmxRepository(Repository repository) {
        try {
            String[] itemNames = FeaturesServiceMBean.REPOSITORY;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = repository.getName();
            itemValues[1] = repository.getURI().toString();
            itemValues[2] = toStringArray(repository.getRepositories());
            itemValues[3] = getFeatureIdentifierTable(Arrays.asList(repository.getFeatures()));
            itemValues[4] = repository.isBlacklisted();
            data = new CompositeDataSupport(REPOSITORY, itemNames, itemValues);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot form repository open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(Collection<JmxRepository> repositories) {
        TabularDataSupport table = new TabularDataSupport(REPOSITORY_TABLE);
        for (JmxRepository repository : repositories) {
            table.put(repository.asCompositeData());
        }
        return table;
    }

    private static String[] toStringArray(URI[] uris) {
        if (uris == null) {
            return null;
        }
        String[] res = new String[uris.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = uris[i].toString();
        }
        return res;
    }

    static TabularData getFeatureIdentifierTable(List<Feature> features) throws OpenDataException {
        TabularDataSupport table = new TabularDataSupport(JmxFeature.FEATURE_IDENTIFIER_TABLE);
        for (Feature feature : features) {
            String[] itemNames = new String[]{FeaturesServiceMBean.FEATURE_NAME, FeaturesServiceMBean.FEATURE_VERSION};
            Object[] itemValues = new Object[]{feature.getName(), feature.getVersion()};
            CompositeData ident = new CompositeDataSupport(JmxFeature.FEATURE_IDENTIFIER, itemNames, itemValues);
            table.put(ident);
        }
        return table;
    }

    static {
        REPOSITORY = createRepositoryType();
        REPOSITORY_TABLE = createRepositoryTableType();
    }

    private static CompositeType createRepositoryType() {
        try {
            String description = "This type identify a Karaf repository";
            String[] itemNames = FeaturesServiceMBean.REPOSITORY;
            OpenType<?>[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = new ArrayType<String>(1, SimpleType.STRING);
            itemTypes[3] = JmxFeature.FEATURE_IDENTIFIER_TABLE;
            itemTypes[4] = SimpleType.BOOLEAN;

            itemDescriptions[0] = "The name of the repository";
            itemDescriptions[1] = "The uri of the repository";
            itemDescriptions[2] = "The dependent repositories";
            itemDescriptions[3] = "The list of included features";
            itemDescriptions[4] = "Whether the repository is blacklisted";

            return new CompositeType("Repository", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build repository type", e);
        }
    }

    private static TabularType createRepositoryTableType() {
        try {
            return new TabularType("Features", "The table of repositories",
                    REPOSITORY, new String[]{FeaturesServiceMBean.REPOSITORY_URI});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build repository table type", e);
        }
    }

}

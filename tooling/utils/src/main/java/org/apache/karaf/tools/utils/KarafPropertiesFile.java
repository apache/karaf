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
package org.apache.karaf.tools.utils;

import org.apache.commons.io.FileUtils;
import org.apache.karaf.tools.utils.model.KarafPropertyEdit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class KarafPropertiesFile {

    private final SortedProperties properties;
    private final File propertyFile;

    public KarafPropertiesFile(File karafHome, String location) {
        this(homedPropFile(karafHome, location));
    }

    public KarafPropertiesFile(File propertyFile) {
        this.propertyFile = propertyFile;
        properties = new SortedProperties();
    }

    private static File homedPropFile(File karafHome, String location) {
        File propFile;
        if (location.startsWith("/")) {
            propFile = new File(karafHome + location);
        } else {
            propFile = new File(karafHome + "/" + location);
        }
        return propFile;
    }

    public void load() throws IOException {
        if (!propertyFile.exists()) {
            return;
        }
        properties.load(new FileInputStream(propertyFile));
    }

    public void put(String key, String value) {
        properties.put(key, value);
    }
    
    public void remove(String key) {
        properties.remove(key);
    }

    public void extend(String key, String value, boolean prepend) {
        if (properties.get(key) == null) {
            properties.put(key, value);
            return;
        } else if (prepend) {
            properties.put(key, JoinUtil.join(value, (String) properties.get(key)));
        } else {
            properties.put(key, JoinUtil.join((String) properties.get(key), value));
        }
    }

    public void apply(KarafPropertyEdit editSpec) {
        if ("extend".equals(editSpec.getOperation().getOperation())) {
            extend(editSpec.getKey(), editSpec.getValue(), editSpec.getOperation().isPrepend());
        } else if ("put".equals(editSpec.getOperation().getOperation())) {
            put(editSpec.getKey(), editSpec.getValue());
        } else if ("remove".equals(editSpec.getOperation().getOperation())) {
            remove(editSpec.getKey());  
        } else {
        
            throw new IllegalArgumentException("Operation must be 'extend' or 'put', not " + editSpec.getOperation());
        }
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public void store() throws IOException {
        store(propertyFile);
    }

    public void store(File destinationFile) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
            properties.store(outputStream, String.format("Modified by %s", getClass().getName()));
        }
    }

    public void replace(File source) {
        try {
            FileUtils.copyFile(source, propertyFile);
        } 
        catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to replace %s", propertyFile.getAbsolutePath()), e);
        }
    }

}

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
package org.apache.felix.karaf.admin.management.codec;

import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.felix.karaf.admin.management.AdminServiceMBean;
import org.apache.felix.karaf.admin.Instance;

public class JmxInstance {
    static final CompositeType INSTANCE;
    static final TabularType INSTANCE_TABLE;

    static {
        INSTANCE = createInstanceType();
        INSTANCE_TABLE = createInstanceTableType();
    }

    private final CompositeDataSupport data;

    private CompositeData asCompositeData() {
        return data;
    }

    public JmxInstance(Instance instance) {
        try {
            String[] itemNames = AdminServiceMBean.INSTANCE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = instance.getPid();
            itemValues[1] = instance.getName();
            itemValues[2] = instance.isRoot();
            itemValues[3] = instance.getPort();
            try {
                itemValues[4] = instance.getState();
            } catch (Exception e) {
                itemValues[4] = "Error";
            }
            itemValues[5] = instance.getLocation();

            data = new CompositeDataSupport(INSTANCE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot create instance open data", e);
        }
    }

    private static CompositeType createInstanceType() {
        try {
            String desc = "This type describes Karaf instances";
            String[] itemNames = AdminServiceMBean.INSTANCE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] descriptions = new String[itemNames.length];

            itemTypes[0] = SimpleType.INTEGER;
            descriptions[0] = "The Process ID of the instance or 0 if not running.";

            itemTypes[1] = SimpleType.STRING;
            descriptions[1] = "The name of the instance.";
            
            itemTypes[2] = SimpleType.BOOLEAN;
            descriptions[2] = "Whether the instance is root.";

            itemTypes[3] = SimpleType.INTEGER;
            descriptions[3] = "The SSH port that can be used to connect to the instance.";

            itemTypes[4] = SimpleType.STRING;
            descriptions[4] = "The state of the instance.";

            itemTypes[5] = SimpleType.STRING;
            descriptions[5] = "The location of the instance.";

            return new CompositeType("Instance", desc, itemNames, descriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build instance type", e);
        }
    }

    private static TabularType createInstanceTableType() {
        try {
            return new TabularType("Instances", "Table of all Karaf instances", INSTANCE,
                    new String[] {AdminServiceMBean.INSTANCE_NAME});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build instance table type", e);
        }
    }

    public static TabularData tableFrom(List<JmxInstance> instances) {
        TabularDataSupport table = new TabularDataSupport(INSTANCE_TABLE);
        for (JmxInstance instance : instances) {
            table.put(instance.asCompositeData());
        }
        return table;
    }

}

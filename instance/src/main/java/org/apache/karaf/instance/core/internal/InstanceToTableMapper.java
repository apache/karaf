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
package org.apache.karaf.instance.core.internal;

import java.util.List;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.instance.core.InstancesMBean;

public class InstanceToTableMapper {
    
    private InstanceToTableMapper() {
    }

    private static CompositeDataSupport mapInstance(Instance instance, CompositeType comp) throws OpenDataException {
        String state;
        try {
            state = instance.getState();
        } catch (Exception e) {
            state = "Error";
        }
        Object[] itemValues = new Object[] {instance.getPid(), instance.getName(), instance.isRoot(),
                                            instance.getSshPort(), instance.getSshHost(),
                                            instance.getRmiRegistryPort(), instance.getRmiRegistryHost(),
                                            instance.getRmiServerPort(), instance.getRmiServerHost(),
                                            state, instance.getLocation(),
                                            instance.getJavaOpts()};
        return new CompositeDataSupport(comp, InstancesMBean.INSTANCE, itemValues);
    }

    private static CompositeType createRowType() throws OpenDataException {
        String desc = "This type describes Karaf instance";
        OpenType<?>[] itemTypes = new OpenType[] {SimpleType.INTEGER, SimpleType.STRING, SimpleType.BOOLEAN,
                                                  SimpleType.INTEGER, SimpleType.STRING,
                                                  SimpleType.INTEGER, SimpleType.STRING,
                                                  SimpleType.INTEGER, SimpleType.STRING,
                                                  SimpleType.STRING, SimpleType.STRING, SimpleType.STRING};
        String[] descriptions = new String[] {"The Process ID of the instance or 0 if not running",
                                              "The name of the instance", "Whether the instance is root",
                                              "The SSH port that can be used to connect to the instance",
                                              "The host address where the SSH server is listening",
                                              "The RMI registry port that can be used to manage the instance",
                                              "The host address where the RMI registry is listening",
                                              "The RMI server port that can be used to manage the instance",
                                              "The host address where the RMI server is listening",
                                              "The state of the instance", "The location of the instance",
                                              "The Java options of the instance"};
        CompositeType comp = new CompositeType("Instances", desc, InstancesMBean.INSTANCE, descriptions, itemTypes);
        return comp;
    }

    public static TabularData tableFrom(List<Instance> instances) {
        try {
            CompositeType rowType = createRowType();
            TabularType tableType = new TabularType("Instances", "Table of all Karaf instances", rowType,
                                                    new String[] {InstancesMBean.INSTANCE_NAME});
            TabularDataSupport table = new TabularDataSupport(tableType);
            for (Instance instance : instances) {
                CompositeDataSupport row = mapInstance(instance, rowType);
                table.put(row);
            }
            return table;
        } catch (OpenDataException e) {
            throw new IllegalStateException("Error building instance table", e);
        }
    }
}

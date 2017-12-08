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
package org.apache.karaf.scr.management.codec;

import org.apache.karaf.scr.management.ServiceComponentRuntimeMBean;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public class JmxReference {

    /**
     * The CompositeType which represents a single reference
     */
    public final static CompositeType REFERENCE = createReferenceType();

    /**
     * The TabularType which represents a list of references
     */
    public final static TabularType REFERENCE_TABLE = createReferenceTableType();

    private final CompositeData data;

    public JmxReference(ReferenceDTO reference) {
        try {
            String[] itemNames = ServiceComponentRuntimeMBean.REFERENCE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = reference.name;
            itemValues[1] = reference.interfaceName;
            itemValues[2] = reference.cardinality;
            itemValues[3] = reference.policy;
            itemValues[4] = reference.policyOption;
            itemValues[5] = reference.target;
            itemValues[6] = reference.bind;
            itemValues[7] = reference.unbind;
            itemValues[8] = reference.updated;
            itemValues[9] = reference.field;
            itemValues[10] = reference.fieldOption;
            itemValues[11] = reference.scope;
            data = new CompositeDataSupport(REFERENCE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(ReferenceDTO[] references) {
        TabularDataSupport table = new TabularDataSupport(REFERENCE_TABLE);
        if (references != null) {
            for (ReferenceDTO reference : references) {
                table.put(new JmxReference(reference).asCompositeData());
            }
        }
        return table;
    }

    private static CompositeType createReferenceType() {
        try {
            String description = "This type encapsulates Scr references";
            String[] itemNames = ServiceComponentRuntimeMBean.REFERENCE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = SimpleType.STRING;
            itemTypes[3] = SimpleType.STRING;
            itemTypes[4] = SimpleType.STRING;
            itemTypes[5] = SimpleType.STRING;
            itemTypes[6] = SimpleType.STRING;
            itemTypes[7] = SimpleType.STRING;
            itemTypes[8] = SimpleType.STRING;
            itemTypes[9] = SimpleType.STRING;
            itemTypes[10] = SimpleType.STRING;
            itemTypes[11] = SimpleType.STRING;

            itemDescriptions[0] = "The name of the reference";
            itemDescriptions[1] = "The interface name of the reference";
            itemDescriptions[2] = "The cardinality of the reference";
            itemDescriptions[3] = "The policy of the reference";
            itemDescriptions[4] = "The policy option of the reference";
            itemDescriptions[5] = "The target";
            itemDescriptions[6] = "The bind";
            itemDescriptions[7] = "The unbind";
            itemDescriptions[8] = "The updated";
            itemDescriptions[9] = "The field";
            itemDescriptions[10] = "The field option";
            itemDescriptions[11] = "The scope";

            return new CompositeType("Reference", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build reference type", e);
        }
    }

    private static TabularType createReferenceTableType() {
        try {
            return new TabularType("References", "The table of all references",
                    REFERENCE,  new String[] {ServiceComponentRuntimeMBean.REFERENCE_NAME});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build references table type", e);
        }
    }


}

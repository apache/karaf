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
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

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

public class JmxSvcReference {

    /**
     * The CompositeType which represents a single reference
     */
    public final static CompositeType SVC_REFERENCE = createReferenceType();

    /**
     * The TabularType which represents a list of references
     */
    public final static TabularType SVC_REFERENCE_TABLE = createReferenceTableType();

    private final CompositeData data;
    //String[] COMPONENT_CONFIGURATION = { REFERENCE_NAME, REFERENCE_STATE, REFERENCE_CARDINALITY, REFERENCE_AVAILABILITY, REFERENCE_POLICY, REFERENCE_BOUND_SERVICES};

    public JmxSvcReference(SatisfiedReferenceDTO reference) {
        try {
            String[] itemNames = ServiceComponentRuntimeMBean.SVC_REFERENCE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = reference.name;
            itemValues[1] = reference.target;
            itemValues[2] = getBoundServices(reference.boundServices);
            data = new CompositeDataSupport(SVC_REFERENCE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public JmxSvcReference(UnsatisfiedReferenceDTO reference) {
        try {
            String[] itemNames = ServiceComponentRuntimeMBean.SVC_REFERENCE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = reference.name;
            itemValues[1] = reference.target;
            itemValues[2] = getBoundServices(reference.targetServices);
            data = new CompositeDataSupport(SVC_REFERENCE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(SatisfiedReferenceDTO[] references) {
        TabularDataSupport table = new TabularDataSupport(SVC_REFERENCE_TABLE);
        if (references != null) {
            for (SatisfiedReferenceDTO reference : references) {
                table.put(new JmxSvcReference(reference).asCompositeData());
            }
        }
        return table;
    }

    public static TabularData tableFrom(UnsatisfiedReferenceDTO[] references) {
        TabularDataSupport table = new TabularDataSupport(SVC_REFERENCE_TABLE);
        if (references != null) {
            for (UnsatisfiedReferenceDTO reference : references) {
                table.put(new JmxSvcReference(reference).asCompositeData());
            }
        }
        return table;
    }

    private static CompositeType createReferenceType() {
        try {
            String description = "This type encapsulates Scr references";
            String[] itemNames = ServiceComponentRuntimeMBean.SVC_REFERENCE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = new ArrayType<String>(1, SimpleType.LONG);

            itemDescriptions[0] = "The name of the reference";
            itemDescriptions[1] = "The target of the reference";
            itemDescriptions[2] = "The bound services";

            return new CompositeType("Reference", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build reference type", e);
        }
    }

    private static TabularType createReferenceTableType() {
        try {
            return new TabularType("References", "The table of all references",
                    SVC_REFERENCE,  new String[] {ServiceComponentRuntimeMBean.REFERENCE_NAME});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build references table type", e);
        }
    }


    /**
     * Returns The bound service ids.
     * @param references     The target {@link ServiceReferenceDTO}[].
     * @return
     */
    private static Long[] getBoundServices(ServiceReferenceDTO[] references) {
        if (references == null || references.length == 0) {
            return new Long[0];
        } else {
            Long[] ids = new Long[references.length];
            for (int i=0; i < references.length; i++) {
                ids[i] = references[i].id;
            }
            return ids;
        }
    }
}

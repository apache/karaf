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
package org.apache.karaf.management.mbeans.scr.codec;

import org.apache.felix.scr.Reference;
import org.apache.karaf.management.mbeans.scr.ScrServiceMBean;
import org.osgi.framework.Constants;

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
    //String[] COMPONENT = { REFERENCE_NAME, REFERENCE_STATE, REFERENCE_CARDINALITY, REFERENCE_AVAILABILITY, REFERENCE_POLICY, REFERENCE_BOUND_SERVICES};

    public JmxReference(Reference reference) {
        try {
            String[] itemNames = ScrServiceMBean.REFERENCE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = reference.getName();
            itemValues[1] = reference.isSatisfied();
            itemValues[2] = getCardinality(reference);
            itemValues[3] = getAvailability(reference);
            itemValues[4] = getPolicy(reference);
            itemValues[5] = getBoundServices(reference);
            data = new CompositeDataSupport(REFERENCE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(Reference... references) {
        TabularDataSupport table = new TabularDataSupport(REFERENCE_TABLE);
        for (Reference reference : references) {
            table.put(new JmxReference(reference).asCompositeData());
        }
        return table;
    }

    private static CompositeType createReferenceType() {
        try {
            String description = "This type encapsulates Scr references";
            String[] itemNames = ScrServiceMBean.REFERENCE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.BOOLEAN;
            itemTypes[2] = SimpleType.STRING;
            itemTypes[3] = SimpleType.STRING;
            itemTypes[4] = SimpleType.STRING;
            itemTypes[5] = new ArrayType(1, SimpleType.STRING);

            itemDescriptions[0] = "The name of the reference";
            itemDescriptions[1] = "The state of the reference";
            itemDescriptions[2] = "The cardinality of the reference";
            itemDescriptions[3] = "The availability of the reference";
            itemDescriptions[4] = "The policy of the reference";
            itemDescriptions[5] = "The bound services";

            return new CompositeType("Reference", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build reference type", e);
        }
    }

    private static TabularType createReferenceTableType() {
        try {
            return new TabularType("References", "The table of all references",
                    REFERENCE,  ScrServiceMBean.REFERENCE);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build references table type", e);
        }
    }


    /**
     * Returns a literal for the {@link Reference} cardinality.
     * @param reference     The target {@link Reference}.
     * @return              "Multiple" or "Single".
     */
    private static String getCardinality(Reference reference) {
        if (reference.isMultiple()) {
            return ScrServiceMBean.REFERENCE_CARDINALITY_MULTIPLE;
        } else {
            return ScrServiceMBean.REFERENCE_CARDINALITY_SINGLE;
        }
    }

    /**
     * Returns a literal for the {@link Reference} availability.
     * @param reference     The target {@link Reference}.
     * @return              "Mandatory" or "Optional".
     */
    private static String getAvailability(Reference reference) {
        if (reference.isOptional()) {
            return ScrServiceMBean.REFERENCE_AVAILABILITY_OPTIONAL;
        } else {
            return ScrServiceMBean.REFERENCE_AVAILABILITY_MANDATORY;
        }
    }

    /**
     * Returns a literal for the {@link Reference} policy.
     * @param reference     The target {@link Reference}.
     * @return              "Static" or "Dynamic".
     */
    private static String getPolicy(Reference reference) {
        if (reference.isStatic()) {
            return ScrServiceMBean.REFERENCE_POLICY_STATIC;
        } else {
            return ScrServiceMBean.REFERENCE_POLICY_DYNAMIC;
        }
    }

    /**
     * Returns The bound service ids.
     * @param reference     The target {@link Reference}.
     * @return
     */
    private static String[] getBoundServices(Reference reference) {
        if (reference.getBoundServiceReferences() == null || reference.getBoundServiceReferences().length == 0) {
            return new String[0];
        } else {
            String[] ids = new String[reference.getBoundServiceReferences().length];
            for (int i=0; i < reference.getBoundServiceReferences().length; i++) {
                ids[i] = String.valueOf(reference.getBoundServiceReferences()[i].getProperty(Constants.SERVICE_ID));
            }
            return ids;
        }
    }
}

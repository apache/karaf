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
package org.apache.karaf.features.internal.resolver;

import java.util.List;

import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

/**
 */
public final class FeatureNamespace extends Namespace {

    public static final String FEATURE_NAMESPACE = "karaf.feature";

    public static final String	CAPABILITY_VERSION_ATTRIBUTE	= "version";

    /**
     * The attribute value identifying the resource
     * {@link org.osgi.framework.namespace.IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE type} as an OSGi bundle.
     *
     * @see org.osgi.framework.namespace.IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE
     */
    public static final String	TYPE_FEATURE = "karaf.feature";

    public static String getName(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(FEATURE_NAMESPACE))
            {
                return cap.getAttributes().get(FEATURE_NAMESPACE).toString();
            }
        }
        return null;
    }

    public static Version getVersion(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(FEATURE_NAMESPACE))
            {
                return (Version)
                        cap.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE);
            }
        }
        return null;
    }


    private FeatureNamespace() {
    }
}

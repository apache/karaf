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

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;

/**
 */
public final class UriNamespace extends Namespace {

    public static final String URI_NAMESPACE = "karaf.uri";

    public static String getUri(Resource resource)
    {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(UriNamespace.URI_NAMESPACE))
            {
                return cap.getAttributes().get(UriNamespace.URI_NAMESPACE).toString();
            }
        }
        return null;
    }


    private UriNamespace() {
    }
}

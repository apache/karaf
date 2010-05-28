/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.framework.resolver;

import java.util.Comparator;
import org.apache.felix.framework.capabilityset.Capability;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class CandidateComparator implements Comparator<Capability>
{
    public int compare(Capability cap1, Capability cap2)
    {
        // First check resolved state, since resolved capabilities have priority
        // over unresolved ones. Compare in reverse order since we want to sort
        // in descending order.
        int c = 0;
        if (cap1.getModule().isResolved() && !cap2.getModule().isResolved())
        {
            c = -1;
        }
        else if (!cap1.getModule().isResolved() && cap2.getModule().isResolved())
        {
            c = 1;
        }

        // Compare module capabilities.
        if ((c == 0) && cap1.getNamespace().equals(Capability.MODULE_NAMESPACE))
        {
            c = ((Comparable) cap1.getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE)
                .getValue()).compareTo(cap2.getAttribute(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE)
                    .getValue());
            if (c == 0)
            {
                Version v1 = (cap1.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE) == null)
                    ? Version.emptyVersion
                    : (Version) cap1.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE).getValue();
                Version v2 = (cap2.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE) == null)
                    ? Version.emptyVersion
                    : (Version) cap2.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE).getValue();
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = v2.compareTo(v1);
            }
        }
        // Compare package capabilities.
        else if ((c == 0) && cap1.getNamespace().equals(Capability.PACKAGE_NAMESPACE))
        {
            c = ((Comparable) cap1.getAttribute(Capability.PACKAGE_ATTR).getValue())
                .compareTo(cap2.getAttribute(Capability.PACKAGE_ATTR).getValue());
            if (c == 0)
            {
                Version v1 = (cap1.getAttribute(Capability.VERSION_ATTR) == null)
                    ? Version.emptyVersion
                    : (Version) cap1.getAttribute(Capability.VERSION_ATTR).getValue();
                Version v2 = (cap2.getAttribute(Capability.VERSION_ATTR) == null)
                    ? Version.emptyVersion
                    : (Version) cap2.getAttribute(Capability.VERSION_ATTR).getValue();
                // Compare these in reverse order, since we want
                // highest version to have priority.
                c = v2.compareTo(v1);
            }
        }

        // Finally, compare module identity.
        if (c == 0)
        {
            if (cap1.getModule().getBundle().getBundleId() <
                cap2.getModule().getBundle().getBundleId())
            {
                c = -1;
            }
            else if (cap1.getModule().getBundle().getBundleId() >
                cap2.getModule().getBundle().getBundleId())
            {
                c = 1;
            }
        }

        return c;
    }
}
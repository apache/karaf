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
package org.apache.felix.eventadmin.impl.handler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.ServiceReference;

/**
 * This class implements a <tt>BlackList</tt> that removes references to unregistered
 * services automatically.
 * 
 * @see org.apache.felix.eventadmin.impl.handler.BlackList
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CleanBlackList implements BlackList
{
    // This set removes stale (i.e., unregistered) references on any call to contains
    private final Set m_blackList = Collections.synchronizedSet(new HashSet()
        {
            public boolean contains(final Object object)
            {
                for (Iterator iter = super.iterator(); iter.hasNext();)
                {
                    final ServiceReference ref = (ServiceReference) iter.next();

                    if (null == ref.getBundle())
                    {
                        iter.remove();
                    }
                }

                return super.contains(object);
            }
        });

    /**
     * Add a service to this blacklist.
     * 
     * @param ref The reference of the service that is blacklisted
     * 
     * @see org.apache.felix.eventadmin.impl.handler.BlackList#add(org.osgi.framework.ServiceReference)
     */
    public void add(final ServiceReference ref)
    {
        m_blackList.add(ref);
    }

    /**
     * Lookup whether a given service is blacklisted.
     * 
     * @param ref The reference of the service
     * 
     * @return <tt>true</tt> in case that the service reference has been blacklisted, 
     *      <tt>false</tt> otherwise.
     * 
     * @see org.apache.felix.eventadmin.impl.handler.BlackList#contains(org.osgi.framework.ServiceReference)
     */
    public boolean contains(final ServiceReference ref)
    {
        return m_blackList.contains(ref);
    }

}

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
package org.apache.felix.eventadmin.impl.adapter;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;

/**
 * Abstract base class for all adapters.
 * This class allows to exchange the event admin at runtime
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractAdapter
{
    private volatile EventAdmin m_admin;

    /**
     * The constructor of the adapter.
     *
     * @param admin The <tt>EventAdmin</tt> to use for posting events.
     */
    public AbstractAdapter(final EventAdmin admin)
    {
        update(admin);
    }

    public void update(final EventAdmin admin)
    {
        if (null == admin)
        {
            throw new NullPointerException("EventAdmin must not be null");
        }

        m_admin = admin;
    }

    protected EventAdmin getEventAdmin()
    {
        return m_admin;
    }

    public abstract void destroy(final BundleContext bundleContext);
}

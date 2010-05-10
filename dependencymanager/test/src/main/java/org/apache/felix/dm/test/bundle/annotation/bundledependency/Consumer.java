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
package org.apache.felix.dm.test.bundle.annotation.bundledependency;

import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.dependency.BundleDependency;
import org.apache.felix.dm.annotation.api.dependency.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.Bundle;

/**
 * Simple Consumer which has a BundleDependency dependency.
 */
@Service
public class Consumer
{
    protected volatile boolean m_added;
    protected volatile boolean m_removed;

    @ServiceDependency(filter="(test=consumer)")
    private volatile Sequencer m_sequencer;

    @BundleDependency(required = false, removed = "removed", filter = "(Bundle-SymbolicName=org.apache.felix.dependencymanager)")
    public void add(Bundle b)
    {
        if (b != null && b.getSymbolicName().equals("org.apache.felix.dependencymanager"))
        {
            m_added = true;
        }
    }

    protected void removed(Bundle b)
    {
        m_removed = true;
    }

    @Start
    public void start()
    {
        m_sequencer.step(1);
    }

    @Destroy
    public void destroy()
    {
        if (!m_added)
        {
            throw new IllegalStateException("Did not get DependencyManager bundle");
        }

        if (!m_removed)
        {
            throw new IllegalStateException("Did not remove DependencyManager bundle");
        }
        m_sequencer.step(2);
    }
}

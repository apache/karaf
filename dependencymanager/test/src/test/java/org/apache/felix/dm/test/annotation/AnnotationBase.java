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
package org.apache.felix.dm.test.annotation;

import org.apache.felix.dm.test.Base;
import org.apache.felix.dm.test.Ensure;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Base class for all annotation junit test classes. The class implements a Sequencer interface, 
 * which wraps an "Ensure" object. The Sequencer will be used directly by the tiny bundles, in order to
 * check if steps happen in the expected order. 
 */
public class AnnotationBase extends Base implements Sequencer
{
    /**
     * The object used to check if expected steps happen in the correct order.
     */
    protected Ensure m_ensure = new Ensure();

    /**
     * Helper method used to stop a given bundle.
     * @param symbolicName the symbolic name of the bundle to be stopped.
     * @param context the context of the bundle to be stopped.
     */
    protected void stopBundle(String symbolicName, BundleContext context)
    {
        // Stop the test.annotation bundle
        boolean found = false;
        for (Bundle b : context.getBundles())
        {
            if (b.getSymbolicName().equals(symbolicName))
            {
                try
                {
                    found = true;
                    b.stop();
                }
                catch (BundleException e)
                {
                    e.printStackTrace();
                }
            }
        }
        if (!found)
        {
            throw new IllegalStateException("bundle " + symbolicName + " not found");
        }
    }

    /**
     * Suspend the current thread for a while.
     * @param n the number of milliseconds to wait for.
     */
    protected void sleep(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
        }
    }

    // ----------------------- Sequencer interface ------------------------------------------

    /**
     * Crosses a given step number.
     */
    public void step(int step)
    {
        m_ensure.step(step);
    }

    /**
     * Waits for a given step to happen.
     */
    public void waitForStep(int nr, int timeout)
    {
        m_ensure.waitForStep(nr, timeout);
    }
}

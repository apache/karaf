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
package org.apache.felix.dm.test;

import java.io.File;
import java.net.URL;

import org.junit.After;
import org.osgi.framework.BundleContext;

/**
 * Base class for all test cases.
 */
public class Base
{
    /**
     * Always cleanup our bundle location file (because pax seems to forget to cleanup it)
     * @param context
     */
    @After  
    public void tearDown(BundleContext context)
    {
        try
        {
            File f = new File(new URL(context.getBundle().getLocation()).getPath());
            f.deleteOnExit();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}

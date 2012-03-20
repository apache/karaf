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
package org.apache.karaf.bundle.command;

import java.util.Arrays;
import java.util.Collections;

import org.apache.karaf.bundle.core.internal.BundleServiceImpl;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class ListServicesTest {

    private ListBundleServices listServices;

    @SuppressWarnings("unchecked")
    public ListServicesTest() {
        listServices = new ListBundleServices();
        BundleContext bundleContext = new TestBundleFactory().createBundleContext();
        listServices.setBundleContext(bundleContext);
        listServices.setBundleService(new BundleServiceImpl(bundleContext, Collections.EMPTY_LIST));
    }
    
    @Test
    public void listAllShort() throws Exception {
        System.out.println("listAllShort");
        listServices.doExecute();
    }

    
    @Test
    public void listAllLong() throws Exception {
        System.out.println("listAllLong");
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.doExecute();
    }

    @Test
    public void listAllLongServiceUse() throws Exception {
        System.out.println("listAllLongServicesUse");
        listServices.ids = Arrays.asList(new String[]{"1", "2"});
        listServices.inUse = true;
        listServices.doExecute();
    }


}

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
package org.apache.karaf.obr.core;

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;
import java.util.List;

/**
 * OBR MBean.
 */
public interface ObrMBean {

    List<String> getUrls();
    TabularData getBundles() throws MBeanException;

    void addUrl(String url) throws MBeanException;
    void removeUrl(String url);
    void refreshUrl(String url) throws MBeanException;

    void deployBundle(String bundle) throws MBeanException;
    void deployBundle(String bundle, boolean start, boolean deployOptional) throws MBeanException;
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.diagnostic.management;

import javax.management.MBeanException;

/**
 * Diagnostic MBean which allows to create dumps over JMX.
 */
public interface DiagnosticDumpMBean {

    /**
     * Creates dump over JMX.
     * 
     * @param name Name of the dump.
     * @throws MBeanException In case of any problems.
     */
    void createDump(String name) throws MBeanException;

    /**
     * Create dump with directory switch and name.
     * 
     * @param directory Should dump be created in directory.
     * @param name Name of the dump.
     * @param noThreadDump True to not include thread dump, false else.
     * @param noHeapDump True to not include heap dump, false else.
     * @throws MBeanException In case of any problems.
     */
    void createDump(boolean directory, String name, boolean noThreadDump, boolean noHeapDump) throws MBeanException;

}

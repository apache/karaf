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
package org.apache.karaf.management.internal;

import org.apache.karaf.management.JMXSecurityMBean;
import org.apache.karaf.management.KarafMBeanServerGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.KeyAlreadyExistsException;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JMXSecurityMBeanImpl extends StandardMBean implements JMXSecurityMBean {

    private static final Logger LOG = LoggerFactory.getLogger(JMXSecurityMBeanImpl.class);

    private MBeanServer mbeanServer;
    private KarafMBeanServerGuard guard;

    public JMXSecurityMBeanImpl() throws NotCompliantMBeanException {
        super(JMXSecurityMBean.class);
    }

    public boolean canInvoke(String objectName) throws Exception {
        return canInvoke((BulkRequestContext) null, objectName);
    }

    public boolean canInvoke(String objectName, String methodName) throws Exception {
        return canInvoke(null, objectName, methodName);
    }

    public boolean canInvoke(String objectName, String methodName, String[] argumentTypes) throws Exception {
        return canInvoke(null, objectName, methodName, argumentTypes);
    }

    private boolean canInvoke(BulkRequestContext context, String objectName) throws Exception {
        if (guard == null)
            return true;

        return guard.canInvoke(context, mbeanServer, new ObjectName(objectName));
    }

    private boolean canInvoke(BulkRequestContext context, String objectName, String methodName) throws Exception {
        if (guard == null)
            return true;

        return guard.canInvoke(context, mbeanServer, new ObjectName(objectName), methodName);
    }

    private boolean canInvoke(BulkRequestContext context, String objectName, String methodName, String[] argumentTypes) throws Exception {
        ObjectName on = new ObjectName(objectName);

        if (guard == null)
            return true;

        return guard.canInvoke(context, mbeanServer, on, methodName, argumentTypes);
    }

    public TabularData canInvoke(Map<String, List<String>> bulkQuery) throws Exception {
        TabularData table = new TabularDataSupport(CAN_INVOKE_TABULAR_TYPE);

        BulkRequestContext context = BulkRequestContext.newContext(guard.getConfigAdmin());

        for (Map.Entry<String, List<String>> entry : bulkQuery.entrySet()) {
            String objectName = entry.getKey();
            List<String> methods = entry.getValue();
            if (methods.size() == 0) {
                boolean res = canInvoke(context, objectName);
                CompositeData data = new CompositeDataSupport(CAN_INVOKE_RESULT_ROW_TYPE, CAN_INVOKE_RESULT_COLUMNS, new Object[]{ objectName, "", res });
                table.put(data);
            } else {
                for (String method : methods) {
                    List<String> argTypes = new ArrayList<>();
                    String name = parseMethodName(method, argTypes);

                    boolean res;
                    if (name.equals(method)) {
                        res = canInvoke(context, objectName, name);
                    } else {
                        res = canInvoke(context, objectName, name, argTypes.toArray(new String[]{}));
                    }
                    CompositeData data = new CompositeDataSupport(CAN_INVOKE_RESULT_ROW_TYPE, CAN_INVOKE_RESULT_COLUMNS, new Object[]{ objectName, method, res });
                    try {
                        table.put(data);
                    } catch (KeyAlreadyExistsException e) {
                        // KeyAlreadyExistsException can happen only when methods are not empty
                        LOG.warn("{} (objectName = \"{}\", method = \"{}\")", e, objectName, method);
                    }
                }
            }
        }

        return table;
    }

    private String parseMethodName(String method, List<String> argTypes) {
        method = method.trim();
        int index = method.indexOf('(');
        if (index < 0)
            return method;

        String args = method.substring(index + 1, method.length() - 1);
        for (String arg : args.split(",")) {
            argTypes.add(arg);
        }

        return method.substring(0, index);
    }

    public MBeanServer getMBeanServer() {
        return this.mbeanServer;
    }

    public void setMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    public KarafMBeanServerGuard getGuard() {
        return guard;
    }

    public void setGuard(KarafMBeanServerGuard guard) {
        this.guard = guard;
    }

}

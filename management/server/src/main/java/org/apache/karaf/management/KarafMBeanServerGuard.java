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
package org.apache.karaf.management;

import org.apache.karaf.management.internal.BulkRequestContext;
import org.apache.karaf.management.internal.EventAdminLogger;
import org.apache.karaf.management.internal.EventAdminMBeanServerWrapper;
import org.apache.karaf.management.internal.MBeanInvocationHandler;
import org.apache.karaf.service.guard.tools.ACLConfigurationParser;
import org.apache.karaf.util.jaas.JaasHelper;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.*;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafMBeanServerGuard implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KarafMBeanServerGuard.class);    

    private static final String JMX_ACL_PID_PREFIX = "jmx.acl";
    
    private static final String JMX_ACL_WHITELIST = "jmx.acl.whitelist";
    
    private static final String JMX_ACL_DETAILED_MESSAGE = "jmx.acl.detailed.message";

    private static final String JMX_OBJECTNAME_PROPERTY_WILDCARD = "_";

    private static final Comparator<String[]> WILDCARD_PID_COMPARATOR = new WildcardPidComparator();

    private static final String INVOKE = "invoke";

    private static final String[] INVOKE_SIG = new String[] {ObjectName.class.getName(), String.class.getName(), Object[].class.getName(), String[].class.getName()};

    private ConfigurationAdmin configAdmin;
    private EventAdminLogger logger;

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public EventAdminLogger getLogger() {
        return logger;
    }

    public void setLogger(EventAdminLogger logger) {
        this.logger = logger;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getParameterTypes().length == 0)
            return null;

        if (!ObjectName.class.isAssignableFrom(method.getParameterTypes()[0]))
            return null;

        MBeanServer mbs = (MBeanServer) proxy;
        if (mbs != null && Proxy.getInvocationHandler(mbs) instanceof MBeanInvocationHandler) {
            mbs = ((MBeanInvocationHandler) Proxy.getInvocationHandler(mbs)).getDelegate();
        }
        if (mbs instanceof EventAdminMBeanServerWrapper) {
            mbs = ((EventAdminMBeanServerWrapper) mbs).getDelegate();
        }

        ObjectName objectName = (ObjectName) args[0];
        switch (method.getName()) {
            case "getAttribute":
                handleGetAttribute(mbs, objectName, (String) args[1]);
                break;
            case "getAttributes":
                handleGetAttributes(mbs, objectName, (String[]) args[1]);
                break;
            case "setAttribute":
                handleSetAttribute(mbs, objectName, (Attribute) args[1]);
                break;
            case "setAttributes":
                handleSetAttributes(mbs, objectName, (AttributeList) args[1]);
                break;
            case "invoke":
                handleInvoke(mbs, objectName, (String) args[1], (Object[]) args[2], (String[]) args[3]);
                break;
        }

        return null;
    }

    /**
     * Return whether there is any method that the current user can invoke.
     *
     * @param mbeanServer The MBeanServer where the object is registered.
     * @param objectName The ObjectName to check.
     * @return {@code True} if there is a method on the object that can be invoked, {@code false} else.
     * @throws JMException If the invocation fails.
     * @throws IOException If the invocation fails.
     */
    public boolean canInvoke(MBeanServer mbeanServer, ObjectName objectName) throws JMException, IOException {
        return canInvoke(null, mbeanServer, objectName);
    }

    /**
     * Return whether there is any method that the current user can invoke.
     *
     * @param context {@link BulkRequestContext} for optimized ConfigAdmin access, may be <code>null</code>.
     * @param mbeanServer The MBeanServer where the object is registered.
     * @param objectName The ObjectName to check.
     * @return {@code True} if there is a method on the object that can be invoked, {@code false} else.
     * @throws JMException If the invocation fails.
     * @throws IOException If the invocation fails.
     */
    public boolean canInvoke(BulkRequestContext context, MBeanServer mbeanServer, ObjectName objectName) throws JMException, IOException {
        MBeanInfo info = mbeanServer.getMBeanInfo(objectName);

        for (MBeanOperationInfo operation : info.getOperations()) {
            List<String> sig = new ArrayList<>();
            for (MBeanParameterInfo param : operation.getSignature()) {
                sig.add(param.getType());
            }
            if (canInvoke(context, objectName, operation.getName(), sig.toArray(new String[] {}))) {
                return true;
            }
        }

        for (MBeanAttributeInfo attr : info.getAttributes()) {
            if (attr.isReadable()) {
                if (canInvoke(context, objectName, attr.isIs() ? "is" : "get" + attr.getName(), new String[] {}))
                    return true;
            }
            if (attr.isWritable()) {
                if (canInvoke(context, objectName, "set" + attr.getName(), new String[]{attr.getType()}))
                    return true;
            }
        }

        return false;
    }

    /**
     * Return whether there is any overload of the specified method that can be invoked by the current user.
     *
     * @param mbeanServer The MBeanServer where the object is registered.
     * @param objectName The MBean ObjectName.
     * @param methodName The name of the method.
     * @return {@code True} if there is an overload of the method that can be invoked by the current user.
     * @throws JMException If the invocation fails.
     * @throws IOException If the invocation fails.
     */
    public boolean canInvoke(MBeanServer mbeanServer, ObjectName objectName, String methodName) throws JMException, IOException {
        return canInvoke(null, mbeanServer, objectName, methodName);
    }

    /**
     * Return whether there is any overload of the specified method that can be invoked by the current user.
     *
     * @param context {@link BulkRequestContext} for optimized ConfigAdmin access, may be <code>null</code>.
     * @param mbeanServer The MBeanServer where the object is registered.
     * @param objectName The MBean ObjectName.
     * @param methodName The name of the method.
     * @return {@code True} if there is an overload of the method that can be invoked by the current user.
     * @throws JMException If the invocation fails.
     * @throws IOException If the invocation fails.
     */
    public boolean canInvoke(BulkRequestContext context, MBeanServer mbeanServer, ObjectName objectName, String methodName) throws JMException, IOException {
        methodName = methodName.trim();
        MBeanInfo info = mbeanServer.getMBeanInfo(objectName);

        for (MBeanOperationInfo op : info.getOperations()) {
            if (!methodName.equals(op.getName())) {
                continue;
            }

            List<String> sig = new ArrayList<>();
            for (MBeanParameterInfo param : op.getSignature()) {
                sig.add(param.getType());
            }
            if (canInvoke(context, objectName, op.getName(), sig.toArray(new String[] {}))) {
                return true;
            }
        }

        for (MBeanAttributeInfo attr : info.getAttributes()) {
            String attrName = attr.getName();
            if (methodName.equals("is" + attrName) || methodName.equals("get" + attrName)) {
                return canInvoke(context, objectName, methodName, new String[] {});
            }
            if (methodName.equals("set" + attrName)) {
                return canInvoke(context, objectName, methodName, new String[] { attr.getType() });
            }
        }

        return false;
    }

    /**
     * Return true if the method on the MBean with the specified signature can be invoked.
     *
     * @param mbeanServer The MBeanServer where the object is registered.
     * @param objectName The MBean ObjectName.
     * @param methodName The name of the method.
     * @param signature The signature of the method.
     * @return {@code True} if the method can be invoked, {@code false} else. Note that if a method name or signature
     *      is provided that does not exist on the MBean, the behaviour of this method is undefined. In other words,
     *      if you ask whether a method that does not exist can be invoked, the method may return {@code true} but
     *      actually invoking that method will obviously not work.
     * @throws IOException If the invocation fails.
     */
    public boolean canInvoke(MBeanServer mbeanServer, ObjectName objectName, String methodName, String[] signature) throws IOException {
        return canInvoke(null, mbeanServer, objectName, methodName, signature);
    }

    /**
     * Return true if the method on the MBean with the specified signature can be invoked.
     *
     * @param context {@link BulkRequestContext} for optimized ConfigAdmin access, may be <code>null</code>.
     * @param mbeanServer The MBeanServer where the object is registered.
     * @param objectName The MBean ObjectName.
     * @param methodName The name of the method.
     * @param signature The signature of the method.
     * @return {@code True} if the method can be invoked, {@code false} else. Note that if a method name or signature
     *      is provided that does not exist on the MBean, the behaviour of this method is undefined. In other words,
     *      if you ask whether a method that does not exist can be invoked, the method may return {@code true} but
     *      actually invoking that method will obviously not work.
     * @throws IOException If the invocation fails.
     */
    public boolean canInvoke(BulkRequestContext context, MBeanServer mbeanServer, ObjectName objectName, String methodName, String[] signature) throws IOException {
        // no checking done on the MBeanServer of whether the method actually exists...
        return canInvoke(context, objectName, methodName, signature);
    }

    private boolean canInvoke(BulkRequestContext context, ObjectName objectName, String methodName, String[] signature) throws IOException {
        if (context == null) {
            context = BulkRequestContext.newContext(configAdmin);
        }
        if (canBypassRBAC(context, objectName, methodName)) {
            return true;
        }
        for (String role : getRequiredRoles(context, objectName, methodName, signature)) {
            if (JaasHelper.currentUserHasRole(context.getPrincipals(), role))
                return true;
        }

        return false;
    }

    private void handleGetAttribute(MBeanServer proxy, ObjectName objectName, String attributeName) throws JMException, IOException {
        MBeanInfo info = proxy.getMBeanInfo(objectName);
        String prefix = null;
        for (MBeanAttributeInfo attr : info.getAttributes()) {
            if (attr.getName().equals(attributeName)) {
                prefix = attr.isIs() ? "is" : "get";
            }
        }
        if (prefix == null) {
            LOG.debug("Attribute " + attributeName + " can not be found for MBean " + objectName.toString());
        } else {
            handleInvoke(null, objectName, prefix + attributeName, new Object[]{}, new String[]{});
        }
    }

    private void handleGetAttributes(MBeanServer proxy, ObjectName objectName, String[] attributeNames) throws JMException, IOException {
        for (String attr : attributeNames) {
            handleGetAttribute(proxy, objectName, attr);
        }
    }

    private void handleSetAttribute(MBeanServer proxy, ObjectName objectName, Attribute attribute) throws JMException, IOException {
        String dataType = null;
        MBeanInfo info = proxy.getMBeanInfo(objectName);
        for (MBeanAttributeInfo attr : info.getAttributes()) {
            if (attr.getName().equals(attribute.getName())) {
                dataType = attr.getType();
                break;
            }
        }

        if (dataType == null)
            throw new IllegalStateException("Attribute data type can not be found");

        handleInvoke(null, objectName, "set" + attribute.getName(), new Object[]{ attribute.getValue() }, new String[]{ dataType });
    }

    private void handleSetAttributes(MBeanServer proxy, ObjectName objectName, AttributeList attributes) throws JMException, IOException {
        for (Attribute attr : attributes.asList()) {
            handleSetAttribute(proxy, objectName, attr);
        }
    }
    
    private boolean canBypassRBAC(BulkRequestContext context, ObjectName objectName, String operationName) {
        List<String> allBypassObjectName = new ArrayList<>();

        List<Dictionary<String, Object>> configs = context.getWhitelistProperties();
        for (Dictionary<String, Object> config : configs) {
            Enumeration<String> keys = config.keys();
            while (keys.hasMoreElements()) {
                String element = keys.nextElement();
                allBypassObjectName.add(element);
            }
        }

        for (String pid : iterateDownPids(getNameSegments(objectName))) {
            if (!pid.equals("jmx.acl"))  {
                for (String bypassObjectName : allBypassObjectName) {
                    String objectNameAndMethod[] = bypassObjectName.split(";");
                    if (objectNameAndMethod.length > 1) {
                        //check both the ObjectName and MethodName
                        if (bypassObjectName.equals(pid.substring("jmx.acl.".length()) 
                            + ";" + operationName)) {
                            return true;
                        }
                    } else {
                        if (bypassObjectName.equals(pid.substring("jmx.acl.".length()))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    void handleInvoke(MBeanServer mbs, ObjectName objectName, String operationName, Object[] params, String[] signature) throws IOException, InstanceNotFoundException {
        handleInvoke(mbs, null, objectName, operationName, params, signature);
    }

    void handleInvoke(MBeanServer mbs, BulkRequestContext context, ObjectName objectName, String operationName, Object[] params, String[] signature) throws IOException, InstanceNotFoundException {
        if (mbs != null && mbs.isInstanceOf(objectName, "javax.management.loading.MLet")
            && ("addUrl".equals(operationName) || "getMBeansFromURL".equals(operationName))) {
            SecurityException se = new SecurityException(operationName + " is not allowed to be invoked");
            if (logger != null) {
                logger.log(INVOKE, INVOKE_SIG, null, se, objectName, operationName, signature, params);
            }
            throw se;
        }

        if (context == null) {
            context = BulkRequestContext.newContext(configAdmin);
        }
        if (canBypassRBAC(context, objectName, operationName)) {
            return;
        }
        for (String role : getRequiredRoles(context, objectName, operationName, params, signature)) {
            if (JaasHelper.currentUserHasRole(role))
                return;
        }
        if (Boolean.valueOf(System.getProperty(JMX_ACL_DETAILED_MESSAGE, "false"))) {
            printDetailedMessage(context, objectName, operationName, params, signature);
        }
        SecurityException se = new SecurityException("Insufficient roles/credentials for operation");
        if (logger != null) {
            logger.log(INVOKE, INVOKE_SIG, null, se, objectName, operationName, signature, params);
        }
        throw se;
    }

    private void printDetailedMessage(BulkRequestContext context, ObjectName objectName,
                                      String operationName, Object[] params, String[] signature) throws IOException {
        StringBuilder expectedRoles = new StringBuilder();
        for (String role : getRequiredRoles(context, objectName, operationName, params, signature)) {
            if (expectedRoles.length() != 0) {
                expectedRoles.append(", ").append(role);
            } else {
                expectedRoles = new StringBuilder(role);
            }
        }
        StringBuilder currentRoles = new StringBuilder();
        for (Principal p : context.getPrincipals()) {
            if (!p.getClass().getName().endsWith("RolePrincipal")) {
                continue;
            }
            if (currentRoles.length() != 0) {
                currentRoles.append(", ").append(p.getName());
            } else {
                currentRoles = new StringBuilder(p.getName());
            }
        }
        String matchedPid = null;
        for (String pid : iterateDownPids(getNameSegments(objectName))) {
            String generalPid = getGeneralPid(context.getAllPids(), pid);
            if (generalPid.length() > 0) {
                Dictionary<String, Object> config = context.getConfiguration(generalPid);
                List<String> roles = new ArrayList<>();
                ACLConfigurationParser.Specificity s = ACLConfigurationParser.getRolesForInvocation(operationName, params, signature, config, roles);
                if (s != ACLConfigurationParser.Specificity.NO_MATCH) {
                    matchedPid = generalPid;
                    break;
                }
            }
        }
        if (matchedPid == null) {
            //can't find the matched PID, use the most specific one
            matchedPid = iterateDownPids(getNameSegments(objectName)).get(0);
        }
        LOG.debug("The current roles are \'" + currentRoles 
                  + "\', however the expected roles are \'"
                  + expectedRoles 
                  + "\'. To make the call pass RBAC check, please add current role into entry \'"
                  + operationName + "\' of file "
                  + matchedPid + ".cfg"
                  );
    }

    List<String> getRequiredRoles(ObjectName objectName, String methodName, String[] signature) throws IOException {
        return getRequiredRoles(BulkRequestContext.newContext(configAdmin), objectName, methodName, null, signature);
    }

    List<String> getRequiredRoles(BulkRequestContext context, ObjectName objectName, String methodName, String[] signature) throws IOException {
        return getRequiredRoles(context, objectName, methodName, null, signature);
    }

    List<String> getRequiredRoles(ObjectName objectName, String methodName, Object[] params, String[] signature) throws IOException {
        return getRequiredRoles(BulkRequestContext.newContext(configAdmin), objectName, methodName, params, signature);
    }

    List<String> getRequiredRoles(BulkRequestContext context, ObjectName objectName, String methodName, Object[] params, String[] signature) throws IOException {
        for (String pid : iterateDownPids(getNameSegments(objectName))) {
            String generalPid = getGeneralPid(context.getAllPids(), pid);
            if (generalPid.length() > 0) {
                Dictionary<String, Object> config = context.getConfiguration(generalPid);
                List<String> roles = new ArrayList<>();
                ACLConfigurationParser.Specificity s = ACLConfigurationParser.getRolesForInvocation(methodName, params, signature, config, roles);
                if (s != ACLConfigurationParser.Specificity.NO_MATCH) {
                    return roles;
                }
            }
        }
        return Collections.emptyList();
    }

    private String getGeneralPid(List<String> allPids, String pid) {
        String[] pidStrArray = pid.split(Pattern.quote("."));
        Set<String[]> rets = new TreeSet<>(WILDCARD_PID_COMPARATOR);
        for (String id : allPids) {
            String[] idStrArray = id.split(Pattern.quote("."));
            if (idStrArray.length == pidStrArray.length) {
                boolean match = true;
                for (int i = 0; i < idStrArray.length; i++) {
                    if (idStrArray[i].equals(JMX_OBJECTNAME_PROPERTY_WILDCARD) 
                        || idStrArray[i].equals(pidStrArray[i])) {
                        continue;
                    } else {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    rets.add(idStrArray);
                }
            }
        }

        Iterator<String[]> it = rets.iterator();
        if (!it.hasNext()) {
            return "";
        } else {
            StringBuilder buffer = new StringBuilder();
            for (String segment : it.next()) {
                if (buffer.length() > 0) {
                    buffer.append(".");
                }
                buffer.append(segment);
            }
            return buffer.toString();
        }
    }

    private List<String> getNameSegments(ObjectName objectName) {
        List<String> segments = new ArrayList<>();
        segments.add(objectName.getDomain());
        // TODO can an ObjectName property contain a comma as key or value ?
        // TODO support quoting as described in http://docs.oracle.com/javaee/1.4/api/javax/management/ObjectName.html
        for (String s : objectName.getKeyPropertyListString().split("[,]")) {
            int index = s.indexOf('=');
            if (index < 0) {
                continue;
            }
            String key = objectName.getKeyProperty(s.substring(0, index));
            if (s.substring(0, index).equals("type")) {
                segments.add(1, key);
            } else {
                segments.add(key);
            }
        }
        
        return segments;
    }

    /**
     * Given a list of segments, return a list of PIDs that are searched in this order.
     * For example, given the following segments: org.foo, bar, test
     * the following list of PIDs will be generated (in this order):
     *      jmx.acl.org.foo.bar.test
     *      jmx.acl.org.foo.bar
     *      jmx.acl.org.foo
     *      jmx.acl
     * The order is used as a search order, in which the most specific PID is searched first.
     *
     * @param segments the ObjectName segments.
     * @return the PIDs corresponding with the ObjectName in the above order.
     */
    private List<String> iterateDownPids(List<String> segments) {
        List<String> res = new ArrayList<>();
        for (int i = segments.size(); i > 0; i--) {
            StringBuilder sb = new StringBuilder();
            sb.append(JMX_ACL_PID_PREFIX);
            for (int j = 0; j < i; j++) {
                sb.append('.');
                sb.append(segments.get(j));
            }
            res.add(sb.toString());
        }
        res.add(JMX_ACL_PID_PREFIX); // this is the top PID (aka jmx.acl)
        return res;
    }

    /**
     * <code>nulls</code>-last comparator of PIDs split to segments. {@link #JMX_OBJECTNAME_PROPERTY_WILDCARD}
     * in a segment makes the PID more generic, thus - with lower priority.
     */
    private static class WildcardPidComparator implements Comparator<String[]> {
        @Override
        public int compare(String[] o1, String[] o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            if (o1.length != o2.length) {
                // not necessary - not called with PIDs of different segment count
                return o1.length - o2.length;
            }
            for (int n = 0; n < o1.length; n++) {
                if (o1[n].equals(o2[n])) {
                    continue;
                }
                if (o1[n].equals(JMX_OBJECTNAME_PROPERTY_WILDCARD)) {
                    return 1;
                }
                if (o2[n].equals(JMX_OBJECTNAME_PROPERTY_WILDCARD)) {
                    return -1;
                }
                return o1[n].compareTo(o2[n]);
            }
            return 0;
        }
    }

}

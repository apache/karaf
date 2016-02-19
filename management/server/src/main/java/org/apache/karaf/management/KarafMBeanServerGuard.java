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

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.security.auth.Subject;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.management.internal.BulkRequestContext;
import org.apache.karaf.management.tools.ACLConfigurationParser;
import org.osgi.service.cm.ConfigurationAdmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafMBeanServerGuard implements InvocationHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(KarafMBeanServerGuard.class);

    private static final String JMX_ACL_PID_PREFIX = "jmx.acl";
    
    private static final String JMX_ACL_WHITELIST = "jmx.acl.whitelist";
    
    private static final String ROLE_WILDCARD = "*";
    
    private static final String JMX_OBJECTNAME_PROPERTY_WILDCARD = "_";

    private static final Comparator<String[]> WILDCARD_PID_COMPARATOR = new WildcardPidComparator();

    private ConfigurationAdmin configAdmin;

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getParameterTypes().length == 0)
            return null;

        if (!ObjectName.class.isAssignableFrom(method.getParameterTypes()[0]))
            return null;

        ObjectName objectName = (ObjectName) args[0];
        if ("getAttribute".equals(method.getName())) {
            handleGetAttribute((MBeanServer) proxy, objectName, (String) args[1]);
        } else if ("getAttributes".equals(method.getName())) {
            handleGetAttributes((MBeanServer) proxy, objectName, (String[]) args[1]);
        } else if ("setAttribute".equals(method.getName())) {
            handleSetAttribute((MBeanServer) proxy, objectName, (Attribute) args[1]);
        } else if ("setAttributes".equals(method.getName())) {
            handleSetAttributes((MBeanServer) proxy, objectName, (AttributeList) args[1]);
        } else if ("invoke".equals(method.getName())) {
            handleInvoke(objectName, (String) args[1], (Object[]) args[2], (String[]) args[3]);
        }

        return null;
    }

    /**
     * Returns whether there is any method that the current user can invoke.
     *
     * @param mbeanServer the MBeanServer where the object is registered.
     * @param objectName the ObjectName to check.
     * @return {@code true} if there is a method on the object that can be invoked, {@code false} else.
     * @throws JMException
     * @throws IOException
     */
    public boolean canInvoke(MBeanServer mbeanServer, ObjectName objectName) throws JMException, IOException {
        return canInvoke(null, mbeanServer, objectName);
    }

    /**
     * Returns whether there is any method that the current user can invoke.
     *
     * @param context {@link BulkRequestContext} for optimized ConfigAdmin access, may be <code>null</code>
     * @param mbeanServer the MBeanServer where the object is registered.
     * @param objectName the ObjectName to check.
     * @return {@code true} if there is a method on the object that can be invoked, {@code false} else.
     * @throws JMException
     * @throws IOException
     */
    public boolean canInvoke(BulkRequestContext context, MBeanServer mbeanServer, ObjectName objectName) throws JMException, IOException {
        MBeanInfo info = mbeanServer.getMBeanInfo(objectName);

        for (MBeanOperationInfo operation : info.getOperations()) {
            List<String> sig = new ArrayList<String>();
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
     * Returns whether there is any overload of the specified method that can be invoked by the current user.
     *
     * @param mbeanServer the MBeanServer where the object is registered.
     * @param objectName the MBean ObjectName.
     * @param methodName the name of the method.
     * @return {@code true} if there is an overload of the method that can be invoked by the current user.
     * @throws JMException
     * @throws IOException
     */
    public boolean canInvoke(MBeanServer mbeanServer, ObjectName objectName, String methodName) throws JMException, IOException {
        return canInvoke(null, mbeanServer, objectName, methodName);
    }

    /**
     * Returns whether there is any overload of the specified method that can be invoked by the current user.
     *
     * @param context {@link BulkRequestContext} for optimized ConfigAdmin access, may be <code>null</code>
     * @param mbeanServer the MBeanServer where the object is registered.
     * @param objectName the MBean ObjectName.
     * @param methodName the name of the method.
     * @return {@code true} if there is an overload of the method that can be invoked by the current user.
     * @throws JMException
     * @throws IOException
     */
    public boolean canInvoke(BulkRequestContext context, MBeanServer mbeanServer, ObjectName objectName, String methodName) throws JMException, IOException {
        methodName = methodName.trim();
        MBeanInfo info = mbeanServer.getMBeanInfo(objectName);

        for (MBeanOperationInfo op : info.getOperations()) {
            if (!methodName.equals(op.getName())) {
                continue;
            }

            List<String> sig = new ArrayList<String>();
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
     * Returns true if the method on the MBean with the specified signature can be invoked.
     *
     * @param mbeanServer the MBeanServer where the object is registered.
     * @param objectName the MBean ObjectName.
     * @param methodName the name of the method.
     * @param signature the signature of the method.
     * @return {@code true} if the method can be invoked, {@code false} else. Note that if a method name or signature
     *      is provided that does not exist on the MBean, the behaviour of this method is undefined. In other words,
     *      if you ask whether a method that does not exist can be invoked, the method may return {@code true} but
     *      actually invoking that method will obviously not work.
     * @throws IOException
     */
    public boolean canInvoke(MBeanServer mbeanServer, ObjectName objectName, String methodName, String[] signature) throws IOException {
        return canInvoke(null, mbeanServer, objectName, methodName, signature);
    }

    /**
     * Returns true if the method on the MBean with the specified signature can be invoked.
     *
     * @param context {@link BulkRequestContext} for optimized ConfigAdmin access, may be <code>null</code>
     * @param mbeanServer the MBeanServer where the object is registered.
     * @param objectName the MBean ObjectName.
     * @param methodName the name of the method.
     * @param signature the signature of the method.
     * @return {@code true} if the method can be invoked, {@code false} else. Note that if a method name or signature
     *      is provided that does not exist on the MBean, the behaviour of this method is undefined. In other words,
     *      if you ask whether a method that does not exist can be invoked, the method may return {@code true} but
     *      actually invoking that method will obviously not work.
     * @throws IOException
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
            if (currentUserHasRole(role))
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
        List<String> allBypassObjectName = new ArrayList<String>();

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

    void handleInvoke(ObjectName objectName, String operationName, Object[] params, String[] signature) throws IOException {
        handleInvoke(null, objectName, operationName, params, signature);
    }

    void handleInvoke(BulkRequestContext context, ObjectName objectName, String operationName, Object[] params, String[] signature) throws IOException {
        if (context == null) {
            context = BulkRequestContext.newContext(configAdmin);
        }
        if (canBypassRBAC(context, objectName, operationName)) {
            return;
        }
        for (String role : getRequiredRoles(context, objectName, operationName, params, signature)) {
            if (currentUserHasRole(role))
                return;
        }
        throw new SecurityException("Insufficient roles/credentials for operation");
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
                List<String> roles = new ArrayList<String>();
                ACLConfigurationParser.Specificity s = ACLConfigurationParser.getRolesForInvocation(methodName, params, signature, config, roles);
                if (s != ACLConfigurationParser.Specificity.NO_MATCH) {
                    return roles;
                }
            }
        }
        return Collections.emptyList();
    }
    
    private String getGeneralPid(List<String> allPids, String pid) {
        String ret = "";
        String[] pidStrArray = pid.split(Pattern.quote("."));
        Set<String[]> rets = new TreeSet<String[]>(WILDCARD_PID_COMPARATOR);
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
        List<String> segments = new ArrayList<String>();
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
        List<String> res = new ArrayList<String>();
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

    static boolean currentUserHasRole(String requestedRole) {
        if (ROLE_WILDCARD.equals(requestedRole)) {
            return true;
        }
        
        String clazz;
        String role;
        int index = requestedRole.indexOf(':');
        if (index > 0) {
            clazz = requestedRole.substring(0, index);
            role = requestedRole.substring(index + 1);
        } else {
            clazz = RolePrincipal.class.getName();
            role = requestedRole;
        }

        AccessControlContext acc = AccessController.getContext();
        if (acc == null) {
            return false;
        }
        Subject subject = Subject.getSubject(acc);

        if (subject == null) {
            return false;
        }

        for (Principal p : subject.getPrincipals()) {
            if (clazz.equals(p.getClass().getName()) && role.equals(p.getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * <code>nulls</code>-last comparator of PIDs split to segments. {@link #JMX_OBJECTNAME_PROPERTY_WILDCARD}
     * in a segment makes the PID more generic, thus - with lower prioroty.
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

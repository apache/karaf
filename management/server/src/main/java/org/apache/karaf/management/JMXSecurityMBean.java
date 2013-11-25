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

import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;

/**
 * Security MBean. This MBean can be used to find out whether the currently logged user can access certain MBeans
 * or invoke operations on these MBeans. It can be used when building client-facing consoles to ensure that only
 * operations appropriate for the current user are presented.<p/>
 * This MBean does not actually invoke any operations on the given objects, it only checks permissions.
 */
public interface JMXSecurityMBean {

    /**
     * The Tabular Type returned by the {@link #canInvoke(Map)} operation. The rows consist of
     * {@link #CAN_INVOKE_RESULT_ROW_TYPE} entries.
     * It has a composite key composed by the "ObjectName" and "Method" columns.
     */
    static final TabularType CAN_INVOKE_TABULAR_TYPE = SecurityMBeanOpenTypeInitializer.TABULAR_TYPE;

    /**
     * A row as returned by the {@link #CAN_INVOKE_TABULAR_TYPE}. The columns of the row are defined
     * by {@link #CAN_INVOKE_RESULT_COLUMNS}
     */
    static final CompositeType CAN_INVOKE_RESULT_ROW_TYPE = SecurityMBeanOpenTypeInitializer.ROW_TYPE;

    /**
     * The columns contained in a {@link #CAN_INVOKE_RESULT_ROW_TYPE}. The data types for these columns are
     * as follows:
     * <ul>
     *     <li>"ObjectName": {@link SimpleType#STRING}</li>
     *     <li>"Method": {@link SimpleType#STRING}</li>
     *     <li>"CanInvoke": {@link SimpleType#BOOLEAN}</li>
     * </ul>
     */
    static final String[] CAN_INVOKE_RESULT_COLUMNS = SecurityMBeanOpenTypeInitializer.COLUMNS;

    /**
     * Checks whether the current user can invoke any methods on a JMX MBean.
     *
     * @param objectName the Object Name of the JMX MBean.
     * @return {@code true} if there is at least one method on the MBean that the user can invoke, {@code false} else.
     * @throws Exception
     */
    boolean canInvoke(String objectName) throws Exception;

    /**
     * Checks whether the current user can invoke overload of the given method.
     *
     * @param objectName the Object Name of the JMX MBean.
     * @param methodName the name of the method to check.
     * @return {@code true} if there is an overload of the specified method that the user can invoke, {@code false} else.
     * @throws Exception
     */
    boolean canInvoke(String objectName, String methodName) throws Exception;

    /**
     * Checks whether the current user can invoke the given method.
     *
     * @param objectName the Object Name of the JMX MBean.
     * @param methodName the name of the method to check.
     * @param argumentTypes the argument types of the method.
     * @return {@code true} if the user is allowed to invoke the method, or any of the methods with the given name if
     * {@code null} is used for the arguments. There may still be certain values that the user does not have permissions
     * to pass to the method.
     * @throws Exception
     */
    boolean canInvoke(String objectName, String methodName, String[] argumentTypes) throws Exception;

    /**
     * Bulk operation to check whether the current user can access the requested MBeans or invoke the requested
     * methods.
     *
     * @param bulkQuery a map of Object Name to requested operations. Operations can be specified with or without
     *                  argument types. An operation without arguments matches any overloaded method with this
     *                  name. If an empty list is provided for the operation names, a check is done whether the
     *                  current user can invoke <em>any</em> operation on the MBean.<p/>
     *                  Example:
     *                  <pre>{@code
     *                  Map<String, List<String>> query = new HashMap<>();
     *                  String objectName = "org.acme:type=SomeMBean";
     *                  query.put(objectName, Arrays.asList(
     *                      "testMethod(long,java.lang.String)", // check this testMethod
     *                      "otherMethod"));                     // check any overload of otherMethod
     *                  query.put("org.acme:type=SomeOtherMBean",
     *                      Collections.<String>emptyList());    // check any method of SomeOtherMBean
     *                  TabularData result = mb.canInvoke(query);
     *                  }</pre>
     * @return A Tabular Data object with the result. This object conforms the structure as defined in {@link #CAN_INVOKE_TABULAR_TYPE}
     * @throws Exception
     */
    TabularData canInvoke(Map<String, List<String>> bulkQuery) throws Exception;

    // a member class is used to initialize final fields, as this needs to do some exception handling...
    static class SecurityMBeanOpenTypeInitializer {

        private static final String[] COLUMNS = new String[]{ "ObjectName", "Method", "CanInvoke" };
        private static final CompositeType ROW_TYPE;

        static {
            try {
                ROW_TYPE = new CompositeType("CanInvokeRowType",
                        "The rows of a CanInvokeTabularType table.",
                        COLUMNS,
                        new String[]{
                            "The ObjectName of the checked MBean.",
                            "The Method to check. This can be either a bare method name which means 'any method with this name' "
                                + "or any specific overload such as foo(java.lang.String). If an empty String is returned this means"
                                + " 'any' method.",
                            "true if the method or MBean can potentially be invoked by the current user."
                        },
                        new OpenType[] { SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN });
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

        private static final TabularType TABULAR_TYPE;
        static {
            try {
                TABULAR_TYPE = new TabularType("CanInvokeTabularType", "Result of canInvoke() bulk operation", ROW_TYPE,
                        new String[] { "ObjectName", "Method" });
            } catch (OpenDataException e) {
                throw new RuntimeException(e);
            }
        }

    }

}

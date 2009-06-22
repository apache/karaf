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
package org.osgi.framework.boot;

import java.util.Properties;

/**
 * This interface should be implemented by framework implementations when their
 * main object is created. It allows a configurator to set the properties and
 * launch the framework.
 *
 * @author aqute
 */
public interface SystemBundle
{
    /**
     * The name of a Security Manager class with public empty constructor. A
     * valid value is also true, this means that the framework should
     * instantiate its own security manager. If not set, security could be
     * defined by a parent framework or there is no security. This can be
     * detected by looking if there is a security manager set
     */
    String SECURITY = "org.osgi.framework.security";

    /**
     * A valid file path in the file system to a directory that exists. The
     * framework is free to use this directory as it sees fit. This area can not
     * be shared with anything else. If this property is not set, the framework
     * should use a file area from the parent bundle. If it is not embedded, it
     * must use a reasonable platform default.
     */
    String STORAGE = "org.osgi.framework.storage";

    /*
     * A list of paths (separated by path separator) that point to additional
     * directories to search for platform specific libraries
     */ String LIBRARIES = "org.osgi.framework.libraries";
    /*
     * The command to give a file executable permission. This is necessary in
     * some environments for running shared libraries.
     */ String EXECPERMISSION = "org.osgi.framework.command.execpermission";

    /*
     * Points to a directory with certificates. ###??? Keystore? Certificate
     * format?
     */ String ROOT_CERTIFICATES = "org.osgi.framework.root.certificates";

    /*
     * Set by the configurator but the framework should provide a reasonable
     * default.
     */ String WINDOWSYSTEM = "org.osgi.framework.windowsystem";

    /**
     * Configure this framework with the given properties. These properties can
     * contain framework specific properties or of the general kind defined in
     * the specification or in this interface.
     *
     * @param properties The properties. This properties can be backed by another
     *                   properties, it can there not be assumed that it contains all
     *                   keys. Use it only through the getProperty methods. This parameter may be null.
     */
    void init(Properties configuration);

    /**
     * Wait until the framework is completely finished.
     * <p/>
     * This method will return if the framework is stopped and has cleaned up
     * all the framework resources.
     *
     * @param timeout Maximum number of milliseconds to wait until the framework is finished. Specifying a zero will wait indefinitely.
     */

    void join(long timeout) throws InterruptedException;
}

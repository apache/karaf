/*
 * Copyright (c) OSGi Alliance (2010). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Framework Start Level Package Version 1.0.
 * 
 * <p>
 * The Framework Start Level package allows management agents to manage a start
 * level assigned to each bundle and the active start level of the Framework.
 * This package is a replacement for the now deprecated
 * {@code org.osgi.service.startlevel} package.
 * 
 * <p>
 * A start level is defined to be a state of execution in which the Framework
 * exists. Start level values are defined as unsigned integers with 0 (zero)
 * being the state where the Framework is not launched. Progressively higher
 * integral values represent progressively higher start levels. For example, 2
 * is a higher start level than 1.
 * 
 * <p>
 * {@code AdminPermission} is required to modify start level information.
 * 
 * <p>
 * Start Level support in the Framework includes the ability to modify the
 * active start level of the Framework and to assign a specific start level to a
 * bundle. The beginning start level of a Framework is specified via the
 * {@link org.osgi.framework.Constants#FRAMEWORK_BEGINNING_STARTLEVEL} framework
 * property when configuring a framework.
 * 
 * <p>
 * When the Framework is first started it must be at start level zero. In this
 * state, no bundles are running. This is the initial state of the Framework
 * before it is launched. When the Framework is launched, the Framework will
 * enter start level one and all bundles which are assigned to start level one
 * and whose autostart setting indicates the bundle should be started are
 * started as described in the {@link org.osgi.framework.Bundle#start(int)}
 * method. The Framework will continue to increase the start level, starting
 * bundles at each start level, until the Framework has reached a beginning
 * start level. At this point the Framework has completed starting bundles and
 * will then fire a Framework event of type
 * {@link org.osgi.framework.FrameworkEvent#STARTED} to announce it has
 * completed its launch.
 * 
 * <p>
 * Within a start level, bundles may be started in an order defined by the
 * Framework implementation. This may be something like ascending
 * {@link org.osgi.framework.Bundle#getBundleId()} order or an order based upon
 * dependencies between bundles. A similar but reversed order may be used when
 * stopping bundles within a start level.
 * 
 * <p>
 * The Framework Start Level package can be used by management bundles to alter
 * the active start level of the framework.
 * 
 * <p>
 * Bundles wishing to use this package must list the package in the
 * Import-Package header of the bundle's manifest. For example:
 * 
 * <pre>
 * Import-Package: org.osgi.framework.startlevel; version=&quot;[1.0,2.0)&quot;
 * </pre>
 * 
 * @version $Id: 270a001c55674ef419794fa4574472b09130af9e $
 */
package org.osgi.framework.startlevel;


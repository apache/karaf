/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.servicebinder.architecture;

/**
 * The ServiceBinderListener interface must be implemented by any subclass
 * of the GenericActivator if it wishes to receive notifications about
 * changes in the architecture
 *
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public interface ServiceBinderListener extends java.util.EventListener
{
    /*
    * Method called when an instance changes its state
    */
    void instanceReferenceChanged(InstanceChangeEvent evt);

    /*
    * Method called when a dependency changes its state
    */
    void dependencyChanged(DependencyChangeEvent evt);

}

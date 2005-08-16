/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.osgi.moduleloader.search;

import org.apache.osgi.moduleloader.Module;

/**
 * <p>
 * This interface represents the policy for selecting a specific export
 * target from multiple <i>compatible</i> candidate export targets when
 * the <tt>ImportSearchPolicy</tt> is trying to resolve an import target
 * for a given module. A concrete implementation of this interface is
 * required to create an instance of <tt>ImportSearchPolicy</tt>.
 * </p>
 * @see org.apache.osgi.moduleloader.search.ImportSearchPolicy
**/
public interface SelectionPolicy
{
    /**
     * Selects a single module to resolve the specified import
     * from the array of compatible candidate modules.
     * @param module the module that is importing the target.
     * @param identifier the identifier of the import target.
     * @param version the version number of the import target.
     * @param candidates array of compatible candidate modules from which to choose.
     * @param compatPolicy the compatibility policy that is being used.
     * @return the selected module or <tt>null</tt> if no module
     *         can be selected.
    **/
    public Module select(
        Module module, Object identifier, Object version, Module[] candidates,
        CompatibilityPolicy compatPolicy);
}
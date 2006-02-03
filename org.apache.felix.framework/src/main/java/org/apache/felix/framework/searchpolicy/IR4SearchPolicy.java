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
package org.apache.felix.framework.searchpolicy;

import org.apache.felix.moduleloader.ISearchPolicy;

/**
 * <p>
 * This interface extends the search policy interface with specific
 * methods related to implementing an R4 search policy. For the most
 * part, this interface is really implementation specific and should
 * not be used by any except the R4 search policy implementation or
 * those that are very sure they know what they are doing. For example,
 * just blindly calling the various setter methods will likely not
 * provide the desired results.
 * </p>
**/
public interface IR4SearchPolicy extends ISearchPolicy
{
    public R4Export[] getExports();
    public void setExports(R4Export[] exports);

    public R4Import[] getImports();
    public void setImports(R4Import[] imports);

    public R4Import[] getDynamicImports();
    public void setDynamicImports(R4Import[] imports);

    public R4Library[] getLibraries();
    public void setLibraries(R4Library[] libraries);

    public R4Wire[] getWires();
    public void setWires(R4Wire[] wires);

    public boolean isResolved();
    public void setResolved(boolean resolved);
    public void resolve() throws ResolveException;

    public void addResolverListener(ResolveListener l);
    public void removeResolverListener(ResolveListener l);
}
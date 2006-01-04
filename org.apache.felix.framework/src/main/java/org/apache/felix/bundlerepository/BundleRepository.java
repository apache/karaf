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
package org.apache.felix.bundlerepository;

import java.io.PrintStream;

/**
 * This interface defines a simple bundle repository service.
**/
public interface BundleRepository
{
    /**
     * Get URL list of repositories.
     * @return a space separated list of URLs to use or <tt>null</tt>
     *         to refresh the cached list of bundles.
    **/
    public String[] getRepositoryURLs();

    /**
     * Set URL list of repositories.
     * @param url a space separated list of URLs to use or <tt>null</tt>
     *        to refresh the cached list of bundles.
    **/
    public void setRepositoryURLs(String[] urls);

    /**
     * Returns an array of the bundle symbolic names available
     * in the repository.
     * @return An arry of available bundle symbolic names.
    **/
    public BundleRecord[] getBundleRecords();

    /**
     * Get the specified bundle record from the repository.
     * @param i the bundle record index to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public BundleRecord[] getBundleRecords(String symname);

    /**
     * Get bundle record for the bundle with the specified name
     * and version from the repository.
     * @param name the bundle record name to retrieve.
     * @param version three-interger array of the version associated with
     *        the name to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public BundleRecord getBundleRecord(String symname, int[] version);

    /**
     * Deploys the bundle in the repository that corresponds to
     * the specified update location. The action taken depends on
     * whether the specified bundle is already installed in the local
     * framework. If the bundle is already installed, then this
     * method will attempt to update it. If the bundle is not already
     * installed, then this method will attempt to install it.
     * @param out the stream to use for informational messages.
     * @param err the stream to use for error messages.
     * @param symname the symbolic name of the bundle to deploy.
     * @param isResolve a flag to indicates whether dependencies should
     *        should be resolved.
     * @param isStart a flag to indicate whether installed bundles should
     *        be started.
     * @return <tt>true</tt> if successful, <tt>false</tt> otherwise.
    **/
    public boolean deployBundle(
        PrintStream out, PrintStream err, String symname, int[] version,
        boolean isResolve, boolean isStart);

    /**
     * Returns an array containing all bundle records in the
     * repository that resolve the transitive closure of the
     * passed in array of package declarations.
     * @param pkgs an array of package declarations to resolve.
     * @return an array containing all bundle records in the
     *         repository that resolve the transitive closure of
     *         the passed in array of package declarations.
     * @throws ResolveException if any packages in the transitive
     *         closure of packages cannot be resolved.
    **/
    public BundleRecord[] resolvePackages(IPackage[] pkgs)
        throws ResolveException;
}
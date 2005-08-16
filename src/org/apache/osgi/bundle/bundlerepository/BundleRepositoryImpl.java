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
package org.apache.osgi.bundle.bundlerepository;

import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.osgi.service.bundlerepository.*;
import org.osgi.framework.*;

public class BundleRepositoryImpl implements BundleRepository
{
    private BundleContext m_context = null;
    private RepositoryState m_repo = null;

    public BundleRepositoryImpl(BundleContext context)
    {
        m_context = context;
        m_repo = new RepositoryState(m_context);
    }

    public String[] getRepositoryURLs()
    {
        return m_repo.getURLs();
    }

    public synchronized void setRepositoryURLs(String[] urls)
    {
        m_repo.setURLs(urls);
    }

    /**
     * Get the number of bundles available in the repository.
     * @return the number of available bundles.
    **/
    public synchronized BundleRecord[] getBundleRecords()
    {
        return m_repo.getRecords();
    }

    /**
     * Get the specified bundle record from the repository.
     * @param i the bundle record index to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public synchronized BundleRecord[] getBundleRecords(String symName)
    {
        return m_repo.getRecords(symName);
    }

    /**
     * Get bundle record for the bundle with the specified name
     * and version from the repository.
     * @param name the bundle record name to retrieve.
     * @param version three-interger array of the version associated with
     *        the name to retrieve.
     * @return the associated bundle record or <tt>null</tt>.
    **/
    public synchronized BundleRecord getBundleRecord(String symName, int[] version)
    {
        return m_repo.getRecord(symName, version);
    }

    public boolean deployBundle(
        PrintStream out, PrintStream err, String symName, int[] version,
        boolean isResolve, boolean isStart)
    {
        // List to hold bundles that need to be started.
        List startList = null;

        // Get the bundle record of the remote bundle to be deployed.
        BundleRecord targetRecord = m_repo.getRecord(symName, version);
        if (targetRecord == null)
        {
            err.println("No such bundle in repository.");
            return false;
        }

        // Create an editable snapshot of the current set of
        // locally installed bundles.
        LocalState localState = new LocalState(m_context);

        // If the precise bundle is already deployed, then we are done.
        if (localState.findBundle(symName, version) != null)
        {
            return true;
        }

        // Create the transitive closure all bundles that must be
        // deployed as a result of deploying the target bundle;
        // use a list because this will keep everything in order.
        BundleRecord[] deployRecords = null;
        // If the resolve flag is set, then get its imports to
        // calculate the transitive closure of its dependencies.
        if (isResolve)
        {
//            Package[] imports = (Package[])
//                targetRecord.getAttribute(BundleRecord.IMPORT_PACKAGE);
            Filter[] reqs = (Filter[])
                targetRecord.getAttribute("requirement");
            try
            {
                deployRecords = m_repo.resolvePackages(localState, reqs);
            }
            catch (ResolveException ex)
            {
                err.println("Resolve error: " + ex.getPackage());
                return false;
            }
        }

        // Add the target bundle since it will not be
        // included in the array of records to deploy.
        if (deployRecords == null)
        {
            deployRecords = new BundleRecord[] { targetRecord };
        }
        else
        {
            // Create a new array containing the target and put it first,
            // since the array will be process in reverse order.
            BundleRecord[] newRecs = new BundleRecord[deployRecords.length + 1];
            newRecs[0] = targetRecord;
            System.arraycopy(deployRecords, 0, newRecs, 1, deployRecords.length);
            deployRecords = newRecs;
        }

        // Now iterate through all bundles in the deploy list
        // in reverse order and deploy each; the order is not
        // so important, but by reversing them at least the
        // dependencies will be printed first and perhaps it
        // will avoid some ordering issues when we are starting
        // bundles.
        for (int i = 0; i < deployRecords.length; i++)
        {
            LocalState.LocalBundleRecord updateRecord =
                localState.findUpdatableBundle(deployRecords[i]);
            if (updateRecord != null)
            {
// TODO: Should check to make sure that update bundle isn't already the
// correct version.
                // Modify our copy of the local state to reflect
                // that the bundle is now updated.
                localState.update(updateRecord, deployRecords[i]);

                // Print out an "updating" message.
                if (deployRecords[i] != targetRecord)
                {
                    out.print("Updating dependency: ");
                }
                else
                {
                    out.print("Updating: ");
                }
                out.println(Util.getBundleName(updateRecord.getBundle()));

                // Actually perform the update.
                try
                {
                    URL url = new URL(
                        (String) deployRecords[i].getAttribute(BundleRecord.BUNDLE_URL));
                    updateRecord.getBundle().update(url.openStream());

                    // If necessary, save the updated bundle to be
                    // started later.
                    if (isStart)
                    {
                        if (startList == null)
                        {
                            startList = new ArrayList();
                        }
                        startList.add(updateRecord.getBundle());
                    }
                }
                catch (Exception ex)
                {
                    err.println("Update error: " + Util.getBundleName(updateRecord.getBundle()));
                    ex.printStackTrace(err);
                    return false;
                }
            }
            else
            {
                // Print out an "installing" message.
                if (deployRecords[i] != targetRecord)
                {
                    out.print("Installing dependency: ");
                }
                else
                {
                    out.print("Installing: ");
                }
                out.println(deployRecords[i].getAttribute(BundleRecord.BUNDLE_NAME));

                try
                {
                    // Actually perform the install, but do not use the actual
                    // bundle JAR URL for the bundle location, since this will
                    // limit OBR's ability to manipulate bundle versions. Instead,
                    // use a unique timestamp as the bundle location.
                    URL url = new URL(
                        (String) deployRecords[i].getAttribute(BundleRecord.BUNDLE_URL));
                    Bundle bundle = m_context.installBundle(
                        "obr://"
                        + deployRecords[i].getAttribute(BundleRecord.BUNDLE_NAME)
                        + "/" + System.currentTimeMillis(),
                        url.openStream());

                    // If necessary, save the installed bundle to be
                    // started later.
                    if (isStart)
                    {
                        if (startList == null)
                        {
                            startList = new ArrayList();
                        }
                        startList.add(bundle);
                    }
                }
                catch (Exception ex)
                {
                    err.println("Install error: "
                        + deployRecords[i].getAttribute(BundleRecord.BUNDLE_NAME));
                    ex.printStackTrace(err);
                    return false;
                }
            }
        }

        // If necessary, start bundles after installing them all.
        if (isStart)
        {
            for (int i = 0; (startList != null) && (i < startList.size()); i++)
            {
                Bundle bundle = (Bundle) startList.get(i);
                try
                {
                    bundle.start();
                }
                catch (BundleException ex)
                {
                    err.println("Update error: " + Util.getBundleName(bundle));
                    ex.printStackTrace();
                }
            }
        }

        return true;
    }

    public BundleRecord[] resolvePackages(IPackage[] pkgs)
        throws ResolveException
    {
// TODO: FIX
//        return m_repo.resolvePackages(new LocalState(m_context), pkgs);
        return null;
    }
}
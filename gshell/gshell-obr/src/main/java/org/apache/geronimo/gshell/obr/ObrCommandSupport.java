package org.apache.geronimo.gshell.obr;

import java.io.PrintWriter;

import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Nov 29, 2007
 * Time: 4:51:56 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ObrCommandSupport extends OsgiCommandSupport {

    protected static final char VERSION_DELIM = ',';

    protected Object doExecute() throws Exception {
        // Get repository admin service.
        ServiceReference ref = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        if (ref == null) {
            io.out.println("RepositoryAdmin service is unavailable.");
            return null;
        }
        try {
            RepositoryAdmin admin = (RepositoryAdmin) getBundleContext().getService(ref);
            if (admin == null) {
                io.out.println("RepositoryAdmin service is unavailable.");
                return null;
            }

            doExecute(admin);
        }
        finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

    protected abstract void doExecute(RepositoryAdmin admin) throws Exception;

    protected Resource[] searchRepository(RepositoryAdmin admin, String targetId, String targetVersion)
    {
        // Try to see if the targetId is a bundle ID.
        try
        {
            Bundle bundle = getBundleContext().getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        }
        catch (NumberFormatException ex)
        {
            // It was not a number, so ignore.
        }

        // The targetId may be a bundle name or a bundle symbolic name,
        // so create the appropriate LDAP query.
        StringBuffer sb = new StringBuffer("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null)
        {
            sb.insert(0, "(&");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return admin.discoverResources(sb.toString());
    }

    public Resource selectNewestVersion(Resource[] resources)
    {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            if (i == 0)
            {
                idx = 0;
                v = resources[i].getVersion();
            }
            else
            {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0)
                {
                    idx = i;
                    v = vtmp;
                }
            }
        }
        return (idx < 0) ? null : resources[idx];
    }

    protected String[] getTarget(String bundle) {
        String[] target;
        int idx = bundle.indexOf(VERSION_DELIM);
        if (idx > 0) {
            target = new String[] { bundle.substring(0, idx), bundle.substring(idx) };
        }
        else
        {
            target = new String[] { bundle, null };
        }
        return target;
    }

    protected void printUnderline(PrintWriter out, int length)
    {
        for (int i = 0; i < length; i++)
        {
            out.print('-');
        }
        out.println("");
    }
}

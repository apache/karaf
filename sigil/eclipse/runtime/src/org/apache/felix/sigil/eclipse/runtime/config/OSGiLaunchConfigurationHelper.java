package org.apache.felix.sigil.eclipse.runtime.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.felix.sigil.common.runtime.BundleForm;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;

public class OSGiLaunchConfigurationHelper
{

    public static URL toURL(String loc) throws MalformedURLException
    {
        URL url = null;
        try
        {
            url = new URL(loc);
        }
        catch (MalformedURLException e)
        {
            IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(loc));
            if ( f.exists() ) {
                url = f.getLocation().toFile().toURL();
            }
            else {
                throw new MalformedURLException("Unknown file " + loc );
            }
        }
        return url;
    }

    public static BundleForm getBundleForm(ILaunchConfiguration config) throws CoreException
    {
        String loc = config.getAttribute(OSGiLaunchConfigurationConstants.FORM_FILE_LOCATION, (String) null);
        try
        {
            URL url = OSGiLaunchConfigurationHelper.toURL(loc);
            SigilCore.log("Resolving " + url);
            return BundleForm.resolve(url);
        }
        catch (Exception e)
        {
            throw SigilCore.newCoreException("Failed to parse bundle form file", e);
        }
    }

}

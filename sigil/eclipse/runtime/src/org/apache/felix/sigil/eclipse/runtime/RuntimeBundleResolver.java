package org.apache.felix.sigil.eclipse.runtime;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.runtime.BundleForm.ResolutionContext;
import org.apache.felix.sigil.common.runtime.BundleForm.Resolver;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.runtime.source.SigilSourcePathProvider;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IBundleResolver;
import org.apache.felix.sigil.repository.IRepositoryManager;
import org.apache.felix.sigil.repository.IResolution;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.apache.felix.sigil.repository.ResolutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.osgi.framework.Version;

public class RuntimeBundleResolver implements ResolutionContext
{

    public class SigilBundleResolver implements Resolver
    {
        public URI[] resolve(URI base) throws URISyntaxException
        {
            ArrayList<URI> uris = new ArrayList<URI>(1);
            
            IBundleResolver resolver = manager.getBundleResolver();
            IRequiredBundle element = ModelElementFactory.getInstance().newModelElement(IRequiredBundle.class);
            String[] parts = base.getSchemeSpecificPart().split(":");
            switch( parts.length ) {
                case 2:
                    Version v = Version.parseVersion(parts[1]);
                    element.setVersions(new VersionRange(false, v, v, false));
                    // fall through on purpose
                case 1:
                    element.setSymbolicName(parts[0]);
                    break;
                default:
                    throw new URISyntaxException(base.toString(), "Unexpected number of parts: " + parts.length);
            }
            try
            {
                ResolutionConfig config = new ResolutionConfig(ResolutionConfig.IGNORE_ERRORS);
                IResolution resolution = resolver.resolve(element, config, null);
                if ( resolution.getBundles().isEmpty() ) {
                    SigilCore.error( "Failed to resolve bundle for " + base );
                }
                for ( ISigilBundle b : resolution.getBundles() ) {
                    ISigilProjectModel p = b.getAncestor(ISigilProjectModel.class);
                    if ( p != null ) {
                        uris.add(p.findBundleLocation().toFile().toURI());
                        SigilCore.log("Adding project source to source path " + p.getName());
                        SigilSourcePathProvider.addProjectSource(launchConfig, p);
                    }
                    else {
                        b.synchronize(null);
                        uris.add( b.getLocation().toFile().toURI() );
                    }
                }
            }
            catch (ResolutionException e)
            {
                SigilCore.error("Failed to resolve " + base, e);
            }
            catch (IOException e)
            {
                SigilCore.error("Failed to synchronize " + base, e);
            }
            catch (CoreException e)
            {
                SigilCore.error("Failed to access " + base, e);
            }
            SigilCore.log( "Resolved " + uris );
            return uris.toArray(new URI[uris.size()]);
        }
    }
    
    private final IRepositoryManager manager;
    private final ILaunchConfiguration launchConfig;
    
    public RuntimeBundleResolver(IRepositoryManager manager, ILaunchConfiguration launchConfig) {
        this.manager = manager;
        this.launchConfig = launchConfig;
    }

    public Resolver findResolver(URI uri)
    {
        SigilCore.log( "Finding resolver for " + uri.getScheme() );
        if ( "sigil".equals( uri.getScheme() ) ) {
            SigilCore.log( "Found resolver for " + uri.getScheme() );
            return new SigilBundleResolver();
        }
        return null;
    }
    
}

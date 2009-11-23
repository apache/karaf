package org.apache.felix.sigil.eclipse.runtime.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.runtime.config.OSGiLaunchConfigurationConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardSourcePathProvider;

public class SigilSourcePathProvider extends StandardSourcePathProvider
{
    private static HashMap<ILaunchConfiguration, Set<IRuntimeClasspathEntry>> dynamicRuntime = new HashMap<ILaunchConfiguration, Set<IRuntimeClasspathEntry>>();

    public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] entries,
        ILaunchConfiguration configuration) throws CoreException
    {
        ArrayList<IRuntimeClasspathEntry> all = new ArrayList<IRuntimeClasspathEntry>(entries.length);
        
        all.addAll( workbenchSourcePath(entries, configuration) );
//        all.addAll( launchOverridePath( configuration ) );
//        all.addAll( newtonSourcePath( configuration ) );
        Set<IRuntimeClasspathEntry> dynamic = dynamicRuntime.get( configuration );
        if ( dynamic != null ) {
            all.addAll( dynamic );
        }
        
        return (IRuntimeClasspathEntry[]) all
                .toArray(new IRuntimeClasspathEntry[all.size()]);
    }

    private List<IRuntimeClasspathEntry> workbenchSourcePath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration) throws CoreException {
        return Arrays.asList( super.resolveClasspath(entries, configuration) );
    }
    
    private Collection<IRuntimeClasspathEntry> launchOverridePath(ILaunchConfiguration configuration) throws CoreException {
        ArrayList<IRuntimeClasspathEntry> overrides = new ArrayList<IRuntimeClasspathEntry>();
        
        if ( configuration.getAttribute( OSGiLaunchConfigurationConstants.AUTOMATIC_ADD, true) ) {
            for ( ISigilProjectModel n : SigilCore.getRoot().getProjects() ) {
                overrides.add( JavaRuntime.newProjectRuntimeClasspathEntry(n.getJavaModel()));
            }           
        }
        
        return overrides;
    }

    public static void addProjectSource(ILaunchConfiguration config, ISigilProjectModel project) {
        Set<IRuntimeClasspathEntry> dynamic = dynamicRuntime.get( config );
        
        if ( dynamic == null ) {
            dynamic = new HashSet<IRuntimeClasspathEntry>();
            dynamicRuntime.put( config, dynamic );
        }
        
        IJavaProject javaProject = project.getJavaModel();
        IRuntimeClasspathEntry cp = JavaRuntime.newProjectRuntimeClasspathEntry(javaProject);
        
        dynamic.add( cp );
    }    
    
//    private List<IRuntimeClasspathEntry> newtonSourcePath( ILaunchConfiguration configuration ) throws CoreException {
//        List<IRuntimeClasspathEntry> all = new ArrayList<IRuntimeClasspathEntry>();
//        
////        Collection<IPath> jars = findNewtonJars( configuration );
//        
////        IPath source = findSourcePath( configuration );
//        
////        for ( IPath jar : jars ) {
////            IRuntimeClasspathEntry cp = JavaRuntime.newArchiveRuntimeClasspathEntry(jar);
////            
////            if ( source != null ) {
////                cp.setSourceAttachmentPath( source );
////                cp.setSourceAttachmentRootPath( findSrcDir( source ) );
////            }
////            cp.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
////            
////            all.add( cp );
////        }
//                
//        return all;
//    }
    
//    @SuppressWarnings("unchecked")
//    private IPath findSrcDir( IPath sourceZip ) throws CoreException {
//        ZipFile zip = null;
//        
//        IPath path = null;
//        
//        try {
//            File file = sourceZip.toFile();
//            if (file.exists() && file.isFile() ) {
//                zip = new ZipFile(file);
//                
//                for ( Enumeration e = zip.entries(); e.hasMoreElements(); ) {
//                    ZipEntry entry = (ZipEntry) e.nextElement();
//                    if ( entry.getName().endsWith( "src/" ) ); {
//                        path = new Path( entry.getName() );
//                        break;
//                    }
//                }
//            } // else return null;
//        }
//        catch (ZipException e) {
//            throw SigilCore.newCoreException("Failed to open source zip:" + sourceZip.toFile(), e);
//        }
//        catch (IOException e) {
//            throw SigilCore.newCoreException("Failed to open source zip" + sourceZip.toFile(), e);
//        }
//        finally {
//            if ( zip != null ) {
//                try {
//                    zip.close();
//                }
//                catch (IOException e) {
//                    SigilCore.error( "Failed to close src zip", e);
//                }
//            }
//        }
//        
//        return path;
//    }
    
//    private Collection<IPath> findNewtonJars( ILaunchConfiguration configuration ) throws CoreException {
//        ArrayList<IPath> paths = new ArrayList<IPath>();
//        
//        INewtonInstall install = NewtonLaunchConfigurationHelper.getNewtonInstall(configuration);
//        
//        for ( IPath path : install.getType().getDefaultBundleLocations() ) {
//            paths.add(path);
//        }
//        
//        return paths;
//    }       
    
//    private IPath findSourcePath( ILaunchConfiguration configuration ) throws CoreException {
//        INewtonInstall install = NewtonLaunchConfigurationHelper.getNewtonInstall(configuration);
//        return install.getType().getSourceLocation();
//    }    
}

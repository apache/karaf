/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.cauldron.sigil.ui.wizard.project;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cauldron.sigil.SigilCore;
import org.cauldron.sigil.model.eclipse.ISigilBundle;
import org.cauldron.sigil.model.project.ISigilProjectModel;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.osgi.framework.Version;

/**
 * @author dave
 *
 */
public class SigilProjectWizardSecondPage extends JavaCapabilityConfigurationPage {
    
    private SigilProjectWizardFirstPage firstPage;
    private IProject currentProject;
    private URI currentProjectLocation;
    private boolean created;
    
    public SigilProjectWizardSecondPage(SigilProjectWizardFirstPage firstPage) {
        this.firstPage = firstPage;
    }

    @Override
    public void setVisible(boolean visible) {
    	super.setVisible(visible);
        if (visible) {
            changeToNewProject();
        } else {
            removeProject();
        }
    }
    
    @Override
    protected boolean useNewSourcePage() {
        return true;
    }
    
    protected void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
    	changeToNewProject();
        updateProject(monitor);
    }
    
    private void changeToNewProject() {
    	if ( !created ) {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            
            IWorkspaceRunnable op= new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    try {
                        updateProject(monitor);
                    } catch (InterruptedException e) {
                        throw new OperationCanceledException(e.getMessage());
                    }
                }
            };
            
            try {
                workspace.run(op, Job.getJobManager().createProgressGroup());
                setErrorMessage(null);
                setPageComplete(true);
                created = true;
            }
            catch (CoreException e) {
            	SigilCore.error("Failed to run workspace job", e);
            }        
    	}
    }
    
    private void removeProject() {
        if (currentProject == null || !currentProject.exists()) {
            return;
        }
        
        IWorkspaceRunnable op= new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                doRemoveProject(monitor);
            }
        };
    
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        
        try {
            workspace.run(op, Job.getJobManager().createProgressGroup());
        }
        catch (CoreException e) {
        	SigilCore.error("Failed to run workspace job", e);
        }
        finally {
        	created = false;
        }
    }
    
    private void updateProject(IProgressMonitor monitor) throws CoreException, InterruptedException {
        currentProject = firstPage.getProjectHandle();
        currentProjectLocation= getProjectLocationURI();
        
        String description = firstPage.getDescription();
        Version projectVersion = firstPage.getVersion();
        String vendor = firstPage.getVendor();
        String name = firstPage.getName();
        
        createProject( currentProject, currentProjectLocation, monitor);

        IPath src = createSourcePath();
        
        IPath output = getOutputLocation();
        
        if ( output.segmentCount() == 0 ) {
        	output = new Path( currentProject.getName() ).append( "build" ).append( "classes" );
        }
        
        IClasspathEntry[] entries = getProjectClassPath(src);
        
    	SigilCore.makeSigilProject(currentProject, monitor);
    	
        init(JavaCore.create(currentProject), output.makeRelative(), entries, false);

        configureJavaProject(new SubProgressMonitor(monitor, 3));
        
        configureSigilProject( currentProject, description, projectVersion, vendor, name, src, monitor );
    }
    
	private IPath createSourcePath() throws CoreException {
        IPath projectPath = currentProject.getFullPath();
        IPath src = new Path( "src" );
        IFolder f = currentProject.getFolder( src );
        if ( !f.getLocation().toFile().exists() ) {
        	f.create(true, true, null);
        }
        
        return projectPath.append(src);
	}

	final void doRemoveProject(IProgressMonitor monitor) throws CoreException {
        final boolean noProgressMonitor= (currentProjectLocation == null); // inside workspace
        
        if (monitor == null || noProgressMonitor) {
            monitor= new NullProgressMonitor();
        }
        monitor.beginTask("Remove project", 3); 
        try {
            try {
                boolean removeContent= currentProject.isSynchronized(IResource.DEPTH_INFINITE);
                currentProject.delete(removeContent, false, new SubProgressMonitor(monitor, 2));
                
            } finally {
            }
        } finally {
            monitor.done();
            currentProject= null;
        }        
    }
        
    private IClasspathEntry[] getProjectClassPath(IPath src) throws CoreException {
        List<IClasspathEntry> cpEntries= new ArrayList<IClasspathEntry>();
        cpEntries.add(JavaCore.newSourceEntry(src));
        cpEntries.addAll(Arrays.asList(getDefaultClasspathEntry()));
        cpEntries.add(JavaCore.newContainerEntry(new Path(SigilCore.CLASSPATH_CONTAINER_PATH)));
        IClasspathEntry[] entries= cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
        
        return entries;
    }
    
    private IClasspathEntry[] getDefaultClasspathEntry() {
        IClasspathEntry[] defaultJRELibrary= PreferenceConstants.getDefaultJRELibrary();
        /*String compliance= firstPage.getCompilerCompliance();
        IPath jreContainerPath= new Path(JavaRuntime.JRE_CONTAINER);
        if (compliance == null || defaultJRELibrary.length > 1 || !jreContainerPath.isPrefixOf(defaultJRELibrary[0].getPath())) {
            // use default
            return defaultJRELibrary;
        }
        IVMInstall inst= firstPage.getJVM();
        if (inst != null) {
            IPath newPath= jreContainerPath.append(inst.getVMInstallType().getId()).append(inst.getName());
            return new IClasspathEntry[] { JavaCore.newContainerEntry(newPath) };
        }*/
        return defaultJRELibrary;
    }
    
    
    private void configureSigilProject( IProject project, String description, Version projectVersion, String vendorName, String bundleName, IPath src, IProgressMonitor monitor ) throws CoreException {
        ISigilProjectModel sigil = SigilCore.create(project);
        IClasspathEntry cp = JavaCore.newSourceEntry(src);
        String encodedClasspath = sigil.getJavaModel().encodeClasspathEntry(cp );
        
        ISigilBundle bundle = sigil.getBundle();
        bundle.addClasspathEntry(encodedClasspath);
        
        if(description != null) {
        	bundle.getBundleInfo().setDescription(description);
        }
		if(projectVersion != null) {
        	bundle.setVersion(projectVersion);
        }
		if(vendorName != null) {
			bundle.getBundleInfo().setVendor(vendorName);
		}
		if(bundleName != null) {
			bundle.getBundleInfo().setName(bundleName);
		}
        sigil.save(monitor);
    }
    
    
    private URI getProjectLocationURI() throws CoreException {
        if (firstPage.isInWorkspace()) {
            return null;
        }
        return firstPage.getLocationURI();
    }
    
    @Override
    public boolean isPageComplete() {
    	boolean result = super.isPageComplete();
    	return result;
    }
    
    protected void performCancel() {
    	if(currentProject != null) {
    		removeProject();
    	}
    }
}

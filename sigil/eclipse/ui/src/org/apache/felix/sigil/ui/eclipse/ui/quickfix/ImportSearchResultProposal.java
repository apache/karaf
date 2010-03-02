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

package org.apache.felix.sigil.ui.eclipse.ui.quickfix;


import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionRangeBoundingRule;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.search.ISearchResult;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.CodeStyleConfiguration;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Version;


public class ImportSearchResultProposal implements IJavaCompletionProposal
{

    private ISigilProjectModel project;
    private ICompilationUnit fCompilationUnit;
    private final ISearchResult result;
    private final Shell shell;


    public ImportSearchResultProposal( Shell shell, ISearchResult result, Name node, ISigilProjectModel project )
    {
        this.shell = shell;
        this.result = result;
        this.project = project;
        if ( node != null )
        {
            CompilationUnit cu = ( CompilationUnit ) ASTNodes.getParent( node, ASTNode.COMPILATION_UNIT );
            this.fCompilationUnit = ( ICompilationUnit ) cu.getJavaElement();
        }
    }


    public int getRelevance()
    {
        return 100;
    }


    public void apply( IDocument document )
    {
        IPackageExport e = result.getExport();
        if ( result.getExport() == null )
        {
            if ( MessageDialog.openQuestion( shell, "Modify " + result.getProvider().getBundleInfo().getSymbolicName(),
                result.getPackageName() + " is not exported. Do you want to export it now?" ) )
            {
                final IPackageExport pe = ModelElementFactory.getInstance().newModelElement( IPackageExport.class );
                pe.setPackageName( result.getPackageName() );
                //e.setVersion(version)
                final ISigilProjectModel mod = result.getProvider().getAncestor( ISigilProjectModel.class );
                if ( mod == null )
                {
                    throw new IllegalStateException( "Attempt to modify binary package export" );
                }
                WorkspaceModifyOperation op = new WorkspaceModifyOperation()
                {
                    @Override
                    protected void execute( IProgressMonitor monitor ) throws CoreException
                    {
                        mod.getBundle().getBundleInfo().addExport( pe );
                        mod.save( null );
                    }
                };

                SigilUI.runWorkspaceOperation( op, null );
                e = pe;
            }
        }

        final IPackageImport i = ModelElementFactory.getInstance().newModelElement( IPackageImport.class );
        i.setPackageName( e.getPackageName() );
        IPreferenceStore store = SigilCore.getDefault().getPreferenceStore();
        VersionRangeBoundingRule lowerBoundRule = VersionRangeBoundingRule.valueOf( store
            .getString( SigilCore.DEFAULT_VERSION_LOWER_BOUND ) );
        VersionRangeBoundingRule upperBoundRule = VersionRangeBoundingRule.valueOf( store
            .getString( SigilCore.DEFAULT_VERSION_UPPER_BOUND ) );

        Version version = e.getVersion();
        VersionRange selectedVersions = VersionRange.newInstance( version, lowerBoundRule, upperBoundRule );
        i.setVersions( selectedVersions );

        WorkspaceModifyOperation op = new WorkspaceModifyOperation()
        {
            @Override
            protected void execute( IProgressMonitor monitor ) throws CoreException
            {
                project.getBundle().getBundleInfo().addImport( i );
                project.save( null );
            }
        };

        SigilUI.runWorkspaceOperation( op, null );
        addSourceImport();
    }


    private void addSourceImport()
    {
        // add import
        try
        {
            ImportRewrite rewrite = CodeStyleConfiguration.createImportRewrite( fCompilationUnit, true );
            rewrite.addImport( result.getClassName() );
            JavaModelUtil.applyEdit( fCompilationUnit, rewrite.rewriteImports( null ), false, null );
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Failed to add import", e );
        }
    }


    public String getAdditionalProposalInfo()
    {
        return null;
    }


    public IContextInformation getContextInformation()
    {
        // TODO Auto-generated method stub
        return null;
    }


    public String getDisplayString()
    {
        String type = result.getClassName();
        String loc = result.getExport() == null ? " from " + result.getProvider().getBundleInfo().getSymbolicName()
            : " version " + result.getExport().getVersion();
        return "Import " + type + loc;
    }


    public Image getImage()
    {
        // TODO Auto-generated method stub
        return null;
    }


    public Point getSelection( IDocument document )
    {
        return null;
    }
}

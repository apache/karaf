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


import java.util.ArrayList;
import java.util.HashMap;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.search.ISearchResult;
import org.apache.felix.sigil.search.SigilSearch;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;


@SuppressWarnings("restriction")
public class ImportQuickFixProcessor implements IQuickFixProcessor
{

    private static final Object SYNC_FLAG = new Object();


    public boolean hasCorrections( ICompilationUnit unit, int problemId )
    {
        switch ( problemId )
        {
            case IProblem.ImportNotFound:
            case IProblem.ForbiddenReference:
            case IProblem.NotVisibleType:
            case IProblem.UndefinedType:
                return true;
            default:
                return false;
        }
    }


    public IJavaCompletionProposal[] getCorrections( IInvocationContext context, IProblemLocation[] locations )
        throws CoreException
    {
        try
        {
            HashMap<Object, IJavaCompletionProposal> results = new HashMap<Object, IJavaCompletionProposal>();

            ISigilProjectModel project = findProject( context );

            if ( project != null )
            {
                for ( int i = 0; i < locations.length; i++ )
                {
                    switch ( locations[i].getProblemId() )
                    {
                        case IProblem.ForbiddenReference:
                            handleImportNotFound( project, context, locations[i], results );
                            break;
                        case IProblem.ImportNotFound:
                            handleImportNotFound( project, context, locations[i], results );
                            break;
                        case IProblem.IsClassPathCorrect:
                            handleIsClassPathCorrect( project, context, locations[i], results );
                            break;
                        case IProblem.UndefinedType:
                            handleUndefinedType( project, context, locations[i], results );
                            break;
                        case IProblem.UndefinedName:
                            handleUndefinedName( project, context, locations[i], results );
                            break;
                    }
                }
            }

            return ( IJavaCompletionProposal[] ) results.values().toArray( new IJavaCompletionProposal[results.size()] );
        }
        catch ( RuntimeException e )
        {
            e.printStackTrace();
            throw e;
        }
    }


    private void handleUndefinedName( ISigilProjectModel project, IInvocationContext context, IProblemLocation problem,
        HashMap<Object, IJavaCompletionProposal> results )
    {
        Name node = findNode( context, problem );

        if ( node == null )
        {
            return;
        }
        addSearchResults( node, project, results );
    }


    private void handleIsClassPathCorrect( ISigilProjectModel project, final IInvocationContext context,
        IProblemLocation problemLocation, final HashMap<Object, IJavaCompletionProposal> results )
    {
        for ( final String type : problemLocation.getProblemArguments() )
        {
            final String iPackage = type.substring( 0, type.lastIndexOf( "." ) );

            for ( IPackageExport pe : JavaHelper.findExportsForPackage( project, iPackage ) )
            {
                results.put( type, new ImportPackageProposal( pe, project ) );
            }
        }

        if ( !results.containsKey( SYNC_FLAG ) )
        {
            //results.put( SYNC_FLAG, null);
        }
    }


    private void handleUndefinedType( ISigilProjectModel project, IInvocationContext context, IProblemLocation problem,
        HashMap<Object, IJavaCompletionProposal> results ) throws CoreException
    {
        Name node = findNode( context, problem );

        if ( node == null )
        {
            return;
        }
        addSearchResults( node, project, results );
    }


    private void handleImportNotFound( ISigilProjectModel project, final IInvocationContext context,
        IProblemLocation location, final HashMap<Object, IJavaCompletionProposal> results ) throws CoreException
    {
        ASTNode selectedNode = location.getCoveringNode( context.getASTRoot() );
        if ( selectedNode == null )
            return;

        if ( selectedNode instanceof ClassInstanceCreation )
        {
            ClassInstanceCreation c = ( ClassInstanceCreation ) selectedNode;
            Type t = c.getType();
            Name node = findName( t );
            if ( node != null )
            {
                addSearchResults( node, project, results );
            }
        }
        else
        {
            for ( final String iPackage : readPackage( selectedNode, location ) )
            {
                if ( !results.containsKey( iPackage ) )
                {
                    for ( IPackageExport pe : JavaHelper.findExportsForPackage( project, iPackage ) )
                    {
                        results.put( iPackage, new ImportPackageProposal( pe, project ) );
                    }
                }
            }
        }
    }


    private void addSearchResults( Name node, ISigilProjectModel project,
        HashMap<Object, IJavaCompletionProposal> results )
    {
        for ( ISearchResult result : SigilSearch.findProviders( node.getFullyQualifiedName(), project, null ) )
        {
            if ( project.getBundle().findImport( result.getPackageName() ) == null )
            {
                String type = result.getPackageName() + "." + node.getFullyQualifiedName();
                results.put( type, new ImportSearchResultProposal( SigilUI.getActiveWorkbenchShell(), result, node,
                    project ) );
            }
        }
    }


    private Name findName( Type t )
    {
        if ( t.isSimpleType() )
        {
            SimpleType st = ( SimpleType ) t;
            return st.getName();
        }
        else if ( t.isArrayType() )
        {
            ArrayType at = ( ArrayType ) t;
            return findName( at.getElementType() );
        }
        else
        {
            return null;
        }
    }


    private Name findNode( IInvocationContext context, IProblemLocation problem )
    {
        ASTNode selectedNode = problem.getCoveringNode( context.getASTRoot() );
        if ( selectedNode == null )
        {
            return null;
        }

        while ( selectedNode.getLocationInParent() == QualifiedName.NAME_PROPERTY )
        {
            selectedNode = selectedNode.getParent();
        }

        Name node = null;

        if ( selectedNode instanceof Type )
        {
            node = findName( ( Type ) selectedNode );
        }
        else if ( selectedNode instanceof Name )
        {
            node = ( Name ) selectedNode;
        }

        return node;
    }


    private ISigilProjectModel findProject( IInvocationContext context ) throws CoreException
    {
        IProject project = context.getCompilationUnit().getJavaProject().getProject();
        if ( project.hasNature( SigilCore.NATURE_ID ) )
        {
            return SigilCore.create( project );
        }
        else
        {
            return null;
        }
    }


    private String[] readPackage( ASTNode selectedNode, IProblemLocation location )
    {
        ArrayList<String> packages = new ArrayList<String>();

        ImportDeclaration id = ( ImportDeclaration ) ASTNodes.getParent( selectedNode, ASTNode.IMPORT_DECLARATION );

        if ( id == null )
        {
            MethodInvocation m = ( MethodInvocation ) ASTNodes.getParent( selectedNode, ASTNode.METHOD_INVOCATION );

            if ( m != null )
            {
                packages.add( readPackage( m ) );
                while ( m.getExpression() != null && m.getExpression() instanceof MethodInvocation )
                {
                    m = ( MethodInvocation ) m.getExpression();
                    packages.add( readPackage( m ) );
                }
            }
        }
        else
        {
            if ( id.isOnDemand() )
            {
                packages.add( id.getName().toString() );
            }
            else
            {
                String iStr = id.getName().toString();
                packages.add( iStr.substring( 0, iStr.lastIndexOf( "." ) ) );
            }
        }

        return packages.toArray( new String[packages.size()] );
    }


    private String readPackage( MethodInvocation m )
    {
        return m.resolveMethodBinding().getDeclaringClass().getPackage().getName();
    }
}

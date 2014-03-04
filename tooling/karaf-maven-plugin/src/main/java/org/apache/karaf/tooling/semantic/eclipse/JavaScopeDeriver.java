/*******************************************************************************
 * Copyright (c) 2012, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.apache.karaf.tooling.semantic.eclipse;

import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ScopeContext;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ScopeDeriver;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.util.artifact.JavaScopes;


/**
 * A scope deriver for use with {@link ConflictResolver} that supports the scopes from {@link JavaScopes}.
 */
public final class JavaScopeDeriver
    extends ScopeDeriver
{

    /**
     * Creates a new instance of this scope deriver.
     */
    public JavaScopeDeriver()
    {
    }

    @Override
    public void deriveScope( ScopeContext context )
        throws RepositoryException
    {
        context.setDerivedScope( getDerivedScope( context.getParentScope(), context.getChildScope() ) );
    }

    private String getDerivedScope( String parentScope, String childScope )
    {
        String derivedScope;

        if ( JavaScopes.SYSTEM.equals( childScope ) || JavaScopes.TEST.equals( childScope ) )
        {
            derivedScope = childScope;
        }
        else if ( parentScope == null || parentScope.length() <= 0 || JavaScopes.COMPILE.equals( parentScope ) )
        {
            derivedScope = childScope;
        }
        else if ( JavaScopes.TEST.equals( parentScope ) || JavaScopes.RUNTIME.equals( parentScope ) )
        {
            derivedScope = parentScope;
        }
        else if ( JavaScopes.SYSTEM.equals( parentScope ) || JavaScopes.PROVIDED.equals( parentScope ) )
        {
            derivedScope = JavaScopes.PROVIDED;
        }
        else
        {
            derivedScope = JavaScopes.RUNTIME;
        }

        return derivedScope;
    }

}

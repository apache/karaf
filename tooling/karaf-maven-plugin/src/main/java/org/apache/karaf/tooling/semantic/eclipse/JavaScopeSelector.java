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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ConflictContext;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ConflictItem;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ScopeSelector;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.util.artifact.JavaScopes;


/**
 * A scope selector for use with {@link ConflictResolver} that supports the scopes from {@link JavaScopes}. In general,
 * this selector picks the widest scope present among conflicting dependencies where e.g. "compile" is wider than
 * "runtime" which is wider than "test". If however a direct dependency is involved, its scope is selected.
 */
public final class JavaScopeSelector
    extends ScopeSelector
{

    /**
     * Creates a new instance of this scope selector.
     */
    public JavaScopeSelector()
    {
    }

    @Override
    public void selectScope( ConflictContext context )
        throws RepositoryException
    {
        String scope = context.getWinner().getDependency().getScope();
        if ( !JavaScopes.SYSTEM.equals( scope ) )
        {
            scope = chooseEffectiveScope( context.getItems() );
        }
        context.setScope( scope );
    }

    private String chooseEffectiveScope( Collection<ConflictItem> items )
    {
        Set<String> scopes = new HashSet<String>();
        for ( ConflictItem item : items )
        {
            if ( item.getDepth() <= 1 )
            {
                return item.getDependency().getScope();
            }
            scopes.addAll( item.getScopes() );
        }
        return chooseEffectiveScope( scopes );
    }

    private String chooseEffectiveScope( Set<String> scopes )
    {
        if ( scopes.size() > 1 )
        {
            scopes.remove( JavaScopes.SYSTEM );
        }

        String effectiveScope = "";

        if ( scopes.size() == 1 )
        {
            effectiveScope = scopes.iterator().next();
        }
        else if ( scopes.contains( JavaScopes.COMPILE ) )
        {
            effectiveScope = JavaScopes.COMPILE;
        }
        else if ( scopes.contains( JavaScopes.RUNTIME ) )
        {
            effectiveScope = JavaScopes.RUNTIME;
        }
        else if ( scopes.contains( JavaScopes.PROVIDED ) )
        {
            effectiveScope = JavaScopes.PROVIDED;
        }
        else if ( scopes.contains( JavaScopes.TEST ) )
        {
            effectiveScope = JavaScopes.TEST;
        }

        return effectiveScope;
    }

}

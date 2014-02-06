/*******************************************************************************
 * Copyright (c) 2013 Sonatype, Inc.
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

import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ConflictContext;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.ConflictItem;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver.OptionalitySelector;
import org.sonatype.aether.RepositoryException;


/**
 * An optionality selector for use with {@link ConflictResolver}. In general, this selector only marks a dependency as
 * optional if all its occurrences are optional. If however a direct dependency is involved, its optional flag is
 * selected.
 */
public final class SimpleOptionalitySelector
    extends OptionalitySelector
{

    /**
     * Creates a new instance of this scope selector.
     */
    public SimpleOptionalitySelector()
    {
    }

    @Override
    public void selectOptionality( ConflictContext context )
        throws RepositoryException
    {
        boolean optional = chooseEffectiveOptionality( context.getItems() );
        context.setOptional( optional );
    }

    private boolean chooseEffectiveOptionality( Collection<ConflictItem> items )
    {
        boolean optional = true;
        for ( ConflictItem item : items )
        {
            if ( item.getDepth() <= 1 )
            {
                return item.getDependency().isOptional();
            }
            if ( ( item.getOptionalities() & ConflictItem.OPTIONAL_FALSE ) != 0 )
            {
                optional = false;
            }
        }
        return optional;
    }

}

/*
 * $Header: /cvshome/build/org.osgi.framework/src/org/osgi/framework/FrameworkUtil.java,v 1.1 2005/07/14 20:32:46 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.framework;


import java.lang.reflect.Constructor;


/**
 * Framework Utility class.
 * 
 * <p>
 * This class contains utility methods which access Framework functions that may
 * be useful to bundles.
 * 
 * @version $Revision: 1.1 $
 * @since 1.3
 */
public class FrameworkUtil 
{
    private static final Class[] CONST_ARGS = new Class[] { String.class };
    private static final String FILTER_IMPL_FQCN = "org.osgi.framework.filterImplFQCN";
    private static final String FILTER_IMPL_DEFAULT = "org.apache.felix.framework.FilterImpl";
    private static Class filterImplClass;


    /**
	 * Creates a <code>Filter</code> object. This <code>Filter</code> object
	 * may be used to match a <code>ServiceReference</code> object or a
	 * <code>Dictionary</code> object.
	 * 
	 * <p>
	 * If the filter cannot be parsed, an {@link InvalidSyntaxException} will be
	 * thrown with a human readable message where the filter became unparsable.
	 * 
	 * @param filter The filter string.
	 * @return A <code>Filter</code> object encapsulating the filter string.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *            invalid filter string that cannot be parsed.
	 * @throws NullPointerException If <code>filter</code> is null.
	 * 
	 * @see Filter
	 */
	public static Filter createFilter( String filter ) throws InvalidSyntaxException 
    {
        if ( filterImplClass == null )
        {
            String fqcn = System.getProperty( FILTER_IMPL_FQCN );
            if ( fqcn == null )
            {
                fqcn = FILTER_IMPL_DEFAULT;
            }
            
            try
            {
                filterImplClass = Class.forName( fqcn );
            }
            catch ( ClassNotFoundException e )
            {
                throw new RuntimeException( "Failed to load filter implementation class: " + fqcn );
            }
        }
        
        Constructor constructor;
        try
        {
            constructor = filterImplClass.getConstructor( CONST_ARGS );
            Filter instance = ( Filter ) constructor.newInstance( new Object[] { filter } );
            return instance;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Failed to instantiate filter using implementation class: " 
                + filterImplClass.getName() );
        }
	}
}

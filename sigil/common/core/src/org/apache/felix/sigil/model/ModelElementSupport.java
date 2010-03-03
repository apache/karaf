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

package org.apache.felix.sigil.model;


import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.apache.felix.sigil.model.annotations.Required;


public class ModelElementSupport implements Serializable
{

    private static final Logger log = Logger.getLogger( ModelElementSupport.class.getName() );

    private static final long serialVersionUID = 1L;

    private static final PropertyAdapter[] EMPTY_PROPS = new PropertyAdapter[]
        {};
    private static final IModelElement[] EMPTY_ELEMENTS = new IModelElement[]
        {};
    private static final Object[] ZERO_ARGS = new Object[]
        {};
    private static final Class<?>[] ZERO_PARAMS = new Class[]
        {};

    private static final WeakHashMap<Class<?>, SoftReference<ChildAdapter[]>> adapterCache = new WeakHashMap<Class<?>, SoftReference<ChildAdapter[]>>();;
    private static final WeakHashMap<Class<?>, SoftReference<PropertyAdapter[]>> propertyCache = new WeakHashMap<Class<?>, SoftReference<PropertyAdapter[]>>();;

    private IModelElement target;

    private transient SoftReference<PropertyAdapter[]> propertyReference;
    private transient SoftReference<ChildAdapter[]> childrenReference;
    private transient SoftReference<Set<String>> propertyNameReference;


    public ModelElementSupport( IModelElement target )
    {
        this.target = target;
    }


    public void setProperty( String name, Object value ) throws NoSuchMethodException
    {
        PropertyAdapter p = findProperty( name, value );
        if ( p == null )
        {
            throw new NoSuchMethodException( "No such property " + name + " for type " + target.getClass() );
        }
        invoke( target, p.getWriteMethod(), value );
    }


    public void addProperty( String name, Object value ) throws NoSuchMethodException
    {
        PropertyAdapter p = findProperty( name, value );
        if ( p == null )
        {
            throw new NoSuchMethodException( "No such property " + name + " for type " + target.getClass() );
        }
        invoke( target, p.getAddMethod(), value );
    }


    public void removeProperty( String name, Object value ) throws NoSuchMethodException
    {
        PropertyAdapter p = findProperty( name, value );
        if ( p == null )
        {
            throw new NoSuchMethodException( "No such property " + name + " for type " + target.getClass() );
        }
        invoke( target, p.getRemoveMethod(), value );
    }


    public Object getProperty( String name ) throws NoSuchMethodException
    {
        PropertyAdapter p = findProperty( name, null );
        if ( p == null )
        {
            throw new NoSuchMethodException( "No such property " + name + " for type " + target.getClass() );
        }
        return invoke( target, p.getReadMethod(), ZERO_ARGS );
    }


    public Set<String> getPropertyNames()
    {
        Set<String> names = propertyNameReference == null ? null : propertyNameReference.get();

        if ( names == null )
        {
            names = new HashSet<String>();

            PropertyAdapter[] props = cachedProps( target.getClass() );
            for ( PropertyAdapter prop : props )
            {
                names.add( prop.getName() );
            }

            propertyNameReference = new SoftReference<Set<String>>( names );
        }

        return names;
    }


    public Object getDefaultPropertyValue( String name )
    {
        try
        {
            Method m = target.getClass().getMethod( makeDefaultPropertyValue( name ), ZERO_PARAMS );
            return invoke( target, m, ZERO_ARGS );
        }
        catch ( SecurityException e )
        {
            throw new UndeclaredThrowableException( e );
        }
        catch ( NoSuchMethodException e )
        {
            // fine no default
            return null;
        }
    }


    public Class<?> getPropertyType( String name ) throws NoSuchMethodException
    {
        PropertyAdapter p = findProperty( name, null );
        if ( p == null )
        {
            throw new NoSuchMethodException( "No such property " + name + " for type " + target.getClass() );
        }
        return p.getPropertyType();
    }


    @SuppressWarnings("unchecked")
    public <T extends IModelElement> T[] childrenOfType( Class<T> type )
    {
        ChildAdapter[] adapters = cachedAdapters();

        if ( adapters.length == 0 )
        {
            // return (T[]) EMPTY_ELEMENTS;
            return ( ( T[] ) Array.newInstance( type, 0 ) );
        }

        ArrayList<T> elements = new ArrayList<T>();

        for ( ChildAdapter adapter : adapters )
        {
            Collection<? extends IModelElement> val = adapter.members( target );

            for ( IModelElement e : val )
            {
                if ( type.isInstance( e ) )
                {
                    elements.add( ( T ) e );
                }
            }
        }

        //return elements.toArray( (T[]) EMPTY_ELEMENTS );
        return elements.toArray( ( T[] ) Array.newInstance( type, elements.size() ) );
    }


    public IModelElement[] children()
    {
        ChildAdapter[] adapters = cachedAdapters();

        if ( adapters.length == 0 )
        {
            return EMPTY_ELEMENTS;
        }

        ArrayList<IModelElement> elements = new ArrayList<IModelElement>();

        for ( ChildAdapter adapter : adapters )
        {
            elements.addAll( adapter.members( target ) );
        }

        return elements.toArray( EMPTY_ELEMENTS );
    }


    public boolean addChild( IModelElement element ) throws InvalidModelException
    {
        if ( element.getParent() == null )
        {
            ChildAdapter[] adapters = cachedAdapters();

            if ( adapters.length > 0 )
            {
                for ( ChildAdapter adapter : adapters )
                {
                    if ( adapter.add( target, element ) )
                    {
                        element.setParent( target );
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public boolean removeChild( IModelElement element )
    {
        if ( element.getParent() == target )
        {
            ChildAdapter[] adapters = cachedAdapters();

            if ( adapters.length > 0 )
            {
                for ( ChildAdapter adapter : adapters )
                {
                    if ( adapter.remove( target, element ) )
                    {
                        element.setParent( null );
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public Set<Class<? extends IModelElement>> getChildrenTypes( boolean required )
    {
        ChildAdapter[] adapters = cachedAdapters();

        if ( adapters.length == 0 )
        {
            return Collections.emptySet();
        }

        HashSet<Class<? extends IModelElement>> types = new HashSet<Class<? extends IModelElement>>();

        for ( ChildAdapter adapter : adapters )
        {
            if ( adapter.isRequired() == required )
            {
                Class<? extends IModelElement> type = adapter.getType();

                if ( type != null )
                {
                    types.add( type );
                }
            }
        }

        return types;
    }


    private PropertyAdapter findProperty( String name, Object value )
    {
        PropertyAdapter[] props = propertyReference == null ? null : propertyReference.get();

        if ( props == null )
        {
            props = cachedProps( target.getClass() );
            propertyReference = new SoftReference<PropertyAdapter[]>( props );
        }

        for ( PropertyAdapter prop : props )
        {
            if ( prop.getName().equals( name )
                && ( value == null || prop.getRawType().isAssignableFrom( value.getClass() ) ) )
            {
                return prop;
            }
        }

        return null;
    }


    private static synchronized PropertyAdapter[] cachedProps( Class<? extends IModelElement> type )
    {
        SoftReference<PropertyAdapter[]> ref = propertyCache.get( type );

        PropertyAdapter[] props = ref == null ? null : ref.get();

        if ( props == null )
        {
            props = loadProps( type );
            propertyCache.put( type, new SoftReference<PropertyAdapter[]>( props ) );
        }

        return props;
    }


    private static PropertyAdapter[] loadProps( Class<? extends IModelElement> type )
    {
        ArrayList<PropertyAdapter> props = new ArrayList<PropertyAdapter>();
        for ( Method m : type.getMethods() )
        {
            if ( isValidProperty( m ) )
            {
                try
                {
                    PropertyAdapter p = new PropertyAdapter( m, type );
                    props.add( p );
                }
                catch ( NoSuchMethodException e )
                {
                    // fine not a bean method
                    log.finer( "Invalid bean property method " + m + ": " + e.getMessage() );
                }
            }
        }

        return props.toArray( EMPTY_PROPS );
    }


    private static boolean isValidProperty( Method m )
    {
        return m.getName().startsWith( "get" ) && m.getParameterTypes().length == 0
            && !m.getDeclaringClass().equals( Object.class )
            && !IModelElement.class.isAssignableFrom( m.getReturnType() );
    }


    private static String makeDefaultPropertyValue( String name )
    {
        return "getDefault" + capitalise( name );
    }


    private static String capitalise( String name )
    {
        return Character.toUpperCase( name.charAt( 0 ) ) + name.substring( 1 );
    }


    private static String decapitalise( String substring )
    {
        return Character.toLowerCase( substring.charAt( 0 ) ) + substring.substring( 1 );
    }


    private ChildAdapter[] cachedAdapters()
    {
        ChildAdapter[] adapters = childrenReference == null ? null : childrenReference.get();

        if ( adapters == null )
        {
            adapters = loadAdapters( target );
            childrenReference = new SoftReference<ChildAdapter[]>( adapters );
        }

        return adapters;
    }


    private static synchronized ChildAdapter[] loadAdapters( IModelElement target )
    {
        Class<? extends IModelElement> type = target.getClass();
        SoftReference<ChildAdapter[]> ref = adapterCache.get( type );

        ChildAdapter[] adapters = ref == null ? null : ref.get();

        if ( adapters == null )
        {
            adapters = buildAdapters( type );
            adapterCache.put( type, new SoftReference<ChildAdapter[]>( adapters ) );
        }

        return adapters;
    }


    private static ChildAdapter[] buildAdapters( Class<? extends IModelElement> type )
    {
        ArrayList<ChildAdapter> adapters = new ArrayList<ChildAdapter>();

        for ( Method m : type.getMethods() )
        {
            ChildAdapter adapter = null;

            if ( isValidGetProperty( m ) )
            {
                adapter = buildGetAdapter( m );
            }
            else if ( isValidSetProperty( m ) )
            {
                adapter = buildSetAdapter( m );
            }
            else if ( isValidAddProperty( m ) )
            {
                adapter = buildAddAdapter( m );
            }
            else if ( isValidRemoveProperty( m ) )
            {
                adapter = buildRemoveAdapter( m );
            }

            if ( adapter != null )
            {
                adapters.add( adapter );
            }
        }

        return adapters.toArray( new ChildAdapter[adapters.size()] );
    }


    private static ChildAdapter buildGetAdapter( Method m )
    {
        if ( IModelElement.class.isAssignableFrom( m.getReturnType() ) )
        {
            return new GetPropertyAdapter( m );
        }
        else if ( Collection.class.isAssignableFrom( m.getReturnType() ) )
        {
            return new GetCollectionAdapter( m );
        }
        else if ( isModelArray( m.getReturnType() ) )
        {
            return new GetArrayAdapter( m );
        }
        else
        {
            return null;
        }
    }


    private static ChildAdapter buildSetAdapter( Method m )
    {
        if ( IModelElement.class.isAssignableFrom( m.getParameterTypes()[0] ) )
        {
            return new SetPropertyAdapter( m );
        }
        else
        {
            return null;
        }
    }


    private static ChildAdapter buildAddAdapter( Method m )
    {
        if ( IModelElement.class.isAssignableFrom( m.getParameterTypes()[0] ) )
        {
            return new AddPropertyAdapter( m );
        }
        else
        {
            return null;
        }
    }


    private static ChildAdapter buildRemoveAdapter( Method m )
    {
        if ( IModelElement.class.isAssignableFrom( m.getParameterTypes()[0] ) )
        {
            return new RemovePropertyAdapter( m );
        }
        else
        {
            return null;
        }
    }


    private static boolean isValidRemoveProperty( Method m )
    {
        return m.getParameterTypes().length == 1 && m.getName().startsWith( "remove" )
            && !isDeclared( ICompoundModelElement.class, m );
    }


    private static boolean isValidAddProperty( Method m )
    {
        return m.getParameterTypes().length == 1 && m.getName().startsWith( "add" )
            && !isDeclared( ICompoundModelElement.class, m );
    }


    private static boolean isDeclared( Class<? extends IModelElement> element, Method m )
    {
        try
        {
            element.getMethod( m.getName(), m.getParameterTypes() );
            return true;
        }
        catch ( SecurityException e )
        {
            throw new UndeclaredThrowableException( e );
        }
        catch ( NoSuchMethodException e )
        {
            return false;
        }
    }


    private static boolean isValidSetProperty( Method m )
    {
        return m.getParameterTypes().length == 1 && m.getName().startsWith( "set" )
            && !isDeclared( IModelElement.class, m );
    }


    private static boolean isValidGetProperty( Method m )
    {
        return m.getParameterTypes().length == 0 && m.getName().startsWith( "get" )
            && !isDeclared( IModelElement.class, m ) && !isDeclared( ICompoundModelElement.class, m );
    }


    private static Object invoke( Object target, Method m, Object... args )
    {
        try
        {
            return m.invoke( target, args );
        }
        catch ( IllegalArgumentException e )
        {
            // this should already have been tested
            throw new IllegalStateException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new UndeclaredThrowableException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new UndeclaredThrowableException( e.getCause() );
        }
    }

    private static class PropertyAdapter
    {

        String prop;
        String name;
        Method g;
        Method s;
        Method a;
        Method r;
        Class<?> propertyType;


        public PropertyAdapter( Method g, Class<?> type ) throws SecurityException, NoSuchMethodException
        {
            if ( g.getReturnType().isArray() || Iterable.class.isAssignableFrom( g.getReturnType() ) )
            {
                prop = g.getName().substring( 3 );
                // remove trailing s - as in addWibble, removeWibble, getWibbles
                prop = prop.substring( 0, prop.length() - 1 );
                name = decapitalise( prop );
                a = find( "add", prop, g.getReturnType(), type );
                propertyType = a.getParameterTypes()[0];
                r = find( "remove", prop, g.getReturnType(), type );
                if ( r.getParameterTypes()[0] != propertyType )
                {
                    throw new NoSuchMethodException( "Add remove property method types do not match" );
                }
                propertyType = Array.newInstance( propertyType, 0 ).getClass();
            }
            else
            {
                prop = g.getName().substring( 3 );
                name = decapitalise( prop );
                propertyType = g.getReturnType();
                s = find( "set", prop, propertyType, type );
            }

            this.g = g;
        }


        public Class<?> getRawType()
        {
            return propertyType.isArray() ? propertyType.getComponentType() : propertyType;
        }


        public Class<?> getPropertyType()
        {
            return propertyType;
        }


        public Method getReadMethod()
        {
            return g;
        }


        public Method getAddMethod() throws NoSuchMethodException
        {
            if ( a == null )
            {
                throw new NoSuchMethodException( "No such method add" + prop );
            }

            return a;
        }


        public Method getRemoveMethod() throws NoSuchMethodException
        {
            if ( r == null )
            {
                throw new NoSuchMethodException( "No such method remove" + prop );
            }

            return r;
        }


        public Method getWriteMethod() throws NoSuchMethodException
        {
            if ( s == null )
            {
                throw new NoSuchMethodException( "No such method set" + prop );
            }

            return s;
        }


        @Override
        public String toString()
        {
            return "PropertyAdapter[" + name + "]";
        }


        private Method find( String prefix, String prop, Class<?> returnType, Class<?> type ) throws SecurityException,
            NoSuchMethodException
        {
            String methodName = prefix + prop;

            if ( returnType.isArray() )
            {
                Class<?> t = returnType.getComponentType();
                return type.getMethod( methodName, new Class[]
                    { t } );
            }
            else if ( Iterable.class.isAssignableFrom( returnType ) )
            {
                Method f = null;
                for ( Method m : type.getMethods() )
                {
                    if ( m.getParameterTypes().length == 1 && m.getName().equals( methodName )
                        && !IModelElement.class.isAssignableFrom( m.getParameterTypes()[0] ) )
                    {
                        if ( f == null )
                        {
                            f = m;
                        }
                        else
                        {
                            throw new NoSuchMethodException( "Found duplicate " + methodName );
                        }
                    }
                }
                if ( f == null )
                {
                    throw new NoSuchMethodException( "No such method " + methodName );
                }

                return f;
            }
            else
            {
                return type.getMethod( methodName, new Class[]
                    { returnType } );
            }
        }


        public String getName()
        {
            return name;
        }

    }

    private static abstract class ChildAdapter
    {
        Method m;


        ChildAdapter( Method m )
        {
            this.m = m;
        }


        public boolean isRequired()
        {
            return m.isAnnotationPresent( Required.class );
        }


        boolean add( Object target, IModelElement element )
        {
            return false;
        }


        abstract Class<? extends IModelElement> getType();


        boolean remove( Object target, IModelElement element )
        {
            return false;
        }


        Collection<? extends IModelElement> members( Object target )
        {
            return Collections.emptyList();
        }


        @Override
        public String toString()
        {
            return "ChildAdapter[ " + m.getName() + "]";
        }
    }

    private static class GetPropertyAdapter extends ChildAdapter
    {
        GetPropertyAdapter( Method m )
        {
            super( m );
        }


        @Override
        Collection<? extends IModelElement> members( Object target )
        {
            IModelElement member = ( IModelElement ) invoke( target, m, ZERO_ARGS );
            if ( member == null )
            {
                return Collections.emptyList();
            }
            else
            {
                return Collections.<IModelElement> singleton( member );
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        Class<? extends IModelElement> getType()
        {
            return ( Class<? extends IModelElement> ) m.getReturnType();
        }
    }

    private static class GetCollectionAdapter extends ChildAdapter
    {
        public GetCollectionAdapter( Method m )
        {
            super( m );
        }


        @SuppressWarnings("unchecked")
        @Override
        Collection<? extends IModelElement> members( Object target )
        {
            Collection members = ( Collection ) invoke( target, m, ZERO_ARGS );
            if ( members == null )
            {
                return Collections.emptyList();
            }
            else
            {
                ArrayList<IModelElement> safe = new ArrayList<IModelElement>( members.size() );
                for ( Object o : members )
                {
                    if ( o instanceof IModelElement )
                    {
                        safe.add( ( IModelElement ) o );
                    }
                }
                return safe;
            }
        }


        @Override
        Class<? extends IModelElement> getType()
        {
            // impossible to get type of a collection as erasure removes generics info
            return null;
        }

    }

    private static class GetArrayAdapter extends ChildAdapter
    {
        public GetArrayAdapter( Method m )
        {
            super( m );
        }


        @Override
        Collection<? extends IModelElement> members( Object target )
        {
            IModelElement[] array = ( IModelElement[] ) invoke( target, m, ZERO_ARGS );
            if ( array == null || array.length == 0 )
            {
                return Collections.emptyList();
            }
            else
            {
                return ( Collection<? extends IModelElement> ) Arrays.asList( array );
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        Class<? extends IModelElement> getType()
        {
            return ( Class<? extends IModelElement> ) m.getReturnType().getComponentType();
        }
    }

    private static class SetPropertyAdapter extends ChildAdapter
    {
        public SetPropertyAdapter( Method m )
        {
            super( m );
        }


        @Override
        boolean add( Object target, IModelElement element )
        {
            if ( m.getParameterTypes()[0].isAssignableFrom( element.getClass() ) )
            {
                invoke( target, m, new Object[]
                    { element } );
                return true;
            }
            else
            {
                return false;
            }
        }


        @Override
        boolean remove( Object target, IModelElement element )
        {
            if ( m.getParameterTypes()[0].isAssignableFrom( element.getClass() ) )
            {
                invoke( target, m, new Object[]
                    { null } );
                return true;
            }
            else
            {
                return false;
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        Class<? extends IModelElement> getType()
        {
            return ( Class<? extends IModelElement> ) m.getParameterTypes()[0];
        }
    }

    private static class AddPropertyAdapter extends ChildAdapter
    {
        public AddPropertyAdapter( Method m )
        {
            super( m );
        }


        @Override
        boolean add( Object target, IModelElement element )
        {
            if ( m.getParameterTypes()[0].isAssignableFrom( element.getClass() ) )
            {
                invoke( target, m, new Object[]
                    { element } );
                return true;
            }
            else
            {
                return false;
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        Class<? extends IModelElement> getType()
        {
            return ( Class<? extends IModelElement> ) m.getParameterTypes()[0];
        }
    }

    private static class RemovePropertyAdapter extends ChildAdapter
    {

        public RemovePropertyAdapter( Method m )
        {
            super( m );
        }


        @Override
        boolean remove( Object target, IModelElement element )
        {
            if ( m.getParameterTypes()[0].isAssignableFrom( element.getClass() ) )
            {
                invoke( target, m, new Object[]
                    { element } );
                return true;
            }
            else
            {
                return false;
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        Class<? extends IModelElement> getType()
        {
            return ( Class<? extends IModelElement> ) m.getParameterTypes()[0];
        }
    }


    private static boolean isModelArray( Class<?> returnType )
    {
        return returnType.isArray() && IModelElement.class.isAssignableFrom( returnType.getComponentType() );
    }
}

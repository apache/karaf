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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public abstract class AbstractModelElement implements IModelElement
{

    private static final long serialVersionUID = 1L;

    private IModelElement parent;

    private String description;
    private transient Map<Object, Object> meta;
    private Map<Serializable, Serializable> serializedMeta;
    private OverrideOptions override;

    protected final ModelElementSupport support;


    public AbstractModelElement( String description )
    {
        support = new ModelElementSupport( this );
        this.description = description.intern();
    }


    public String getElementDescription()
    {
        return description;
    }


    public Map<Object, Object> getMeta()
    {
        return meta == null ? Collections.emptyMap() : Collections.unmodifiableMap(meta);
    }


    public void setMeta( Map<Object, Object> meta )
    {
        this.meta = meta;
    }


    @Override
    public AbstractModelElement clone()
    {
        try
        {
            AbstractModelElement clone = ( AbstractModelElement ) super.clone();

            if ( meta != null ) {
                clone.meta = new HashMap<Object, Object>( meta );
            }

            return clone;
        }
        catch ( CloneNotSupportedException e )
        {
            // can't happen but make compiler happy
            throw new IllegalStateException( e );
        }
    }


    @SuppressWarnings("unchecked")
    public <T extends IModelElement> T getAncestor( Class<T> type )
    {
        IModelElement parent = this.parent;

        while ( parent != null )
        {
            if ( type.isInstance( parent ) )
            {
                return ( T ) parent;
            }
            parent = parent.getParent();
        }

        return null;
    }


    public IModelElement getParent()
    {
        return parent;
    }


    public void setParent( IModelElement parent )
    {
        if ( parent != null )
        {
            if ( this.parent != null && this.parent != parent )
            {
                throw new IllegalStateException( "Parent already installed" );
            }
        }

        this.parent = parent;
    }


    public void checkValid() throws InvalidModelException
    {
        for ( String req : getRequiredProperties() )
        {
            try
            {
                if ( getProperty( req ) == null )
                {
                    throw new InvalidModelException( this, "Missing property " + req );
                }
            }
            catch ( NoSuchMethodException e )
            {
                throw new InvalidModelException( this, "No such property " + req );
            }
        }
    }


    public Object getProperty( String name ) throws NoSuchMethodException
    {
        return support.getProperty( name );
    }


    public void setProperty( String name, Object value ) throws NoSuchMethodException
    {
        support.setProperty( name, value );
    }


    public void addProperty( String name, Object value ) throws NoSuchMethodException
    {
        support.addProperty( name, value );
    }


    public void removeProperty( String name, Object value ) throws NoSuchMethodException
    {
        support.removeProperty( name, value );
    }


    public Object getDefaultPropertyValue( String name )
    {
        return support.getDefaultPropertyValue( name );
    }


    public Set<String> getPropertyNames()
    {
        return support.getPropertyNames();
    }


    public Set<String> getRequiredProperties()
    {
        return Collections.emptySet();
    }

    public Class<?> getPropertyType( String name ) throws NoSuchMethodException
    {
        return support.getPropertyType( name );
    }

    protected Object writeReplace()
    {
        AbstractModelElement clone = clone();

        if ( clone.meta != null ) {
            clone.serializedMeta = new HashMap<Serializable, Serializable>(clone.meta.size());
            
            for ( Map.Entry<Object, Object> e : clone.meta.entrySet() )
            {
                if ( e.getKey() instanceof Serializable && e.getValue() instanceof Serializable )
                {
                    clone.serializedMeta.put( ( Serializable ) e.getKey(), ( Serializable ) e.getValue() );
                }
            }
    
            clone.meta = null;
        }

        return clone;
    }

    protected Object readResolve()
    {
        meta = serializedMeta == null ? null : new HashMap<Object, Object>( serializedMeta );
        serializedMeta = null;
        return this;
    }


    public OverrideOptions getOverride()
    {
        return override;
    }


    public void setOverride( OverrideOptions override )
    {
        this.override = override;
    }
}

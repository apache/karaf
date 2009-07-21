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

package org.apache.felix.sigil.model.common;


import java.util.Map;


public class Or implements LDAPExpr
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LDAPExpr[] children;


    public static LDAPExpr apply( LDAPExpr... terms )
    {
        if ( terms == null )
        {
            throw new NullPointerException( "terms cannot be null" );
        }
        else if ( terms.length == 0 )
        {
            return Expressions.T;
        }
        else if ( terms.length == 1 )
        {
            return terms[0];
        }
        LDAPExpr[] filtered = new LDAPExpr[terms.length];
        int ctr = 0;
        for ( int i = 0; i < terms.length; i++ )
        {
            if ( terms[i].equals( Expressions.T ) )
                return Expressions.T;
            if ( terms[i].equals( Expressions.F ) )
                continue;
            filtered[ctr] = terms[i];
            ctr++;
        }
        if ( ctr == 0 )
        {
            return Expressions.F;
        }
        else if ( ctr == 1 )
        {
            return filtered[0];
        }
        LDAPExpr[] orTerms = new LDAPExpr[ctr];
        System.arraycopy( filtered, 0, orTerms, 0, ctr );

        return new Or( orTerms );
    }


    private Or( LDAPExpr... children )
    {
        this.children = children;
    }


    public boolean eval( Map<String, ?> map )
    {
        for ( int i = 0; i < children.length; i++ )
        {
            if ( children[i].eval( map ) )
            {
                return true;
            }
        }
        return false;
    }


    public void visit( ExprVisitor v )
    {
        v.visitOr( this );
    }


    public LDAPExpr[] getChildren()
    {
        return children;
    }


    public void setChildren( LDAPExpr[] children )
    {
        this.children = children;
    }


    @Override
    public boolean equals( Object other )
    {
        if ( other instanceof Or )
        {
            Or that = ( Or ) other;
            if ( children.length != that.children.length )
            {
                return false;
            }
            for ( int i = 0; i < children.length; i++ )
            {
                if ( children[i].equals( that.children[i] ) )
                {
                    return true;
                }
            }
            return false;
        }
        return false;
    }


    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer( 256 );
        buf.append( "(|" );
        for ( int i = 0; i < children.length; i++ )
        {
            buf.append( " " ).append( children[i] ).append( " " );
        }
        buf.append( ")" );
        return buf.toString();
    }

}

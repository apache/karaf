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

package org.apache.felix.sigil.common.osgi;


import java.util.Map;


public class Not extends AbstractExpr
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LDAPExpr[] children;


    public static LDAPExpr apply( LDAPExpr e )
    {
        if ( e == null )
        {
            throw new NullPointerException( "cannot apply Not to a null expression" );
        }
        if ( e.equals( Expressions.T ) )
        {
            return Expressions.F;
        }
        if ( e.equals( Expressions.F ) )
        {
            return Expressions.T;
        }
        return new Not( e );
    }


    private Not( LDAPExpr child )
    {
        this.children = new LDAPExpr[]
            { child };
    }


    public boolean eval( Map<String, ?> map )
    {
        return !children[0].eval( map );
    }


    public LDAPExpr getEx()
    {
        return children[0];
    }


    public LDAPExpr[] getChildren()
    {
        return children;
    }


    public void setChild( LDAPExpr child )
    {
        this.children = new LDAPExpr[]
            { child };
    }


    @Override
    public boolean equals( Object other )
    {
        if ( other instanceof Not )
        {
            Not that = ( Not ) other;
            return children[0].equals( that.children[0] );
        }
        return false;
    }


    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer( 256 );
        buf.append( "(!" );
        buf.append( " " ).append( children[0] ).append( " " );
        buf.append( ")" );
        return buf.toString();
    }

}

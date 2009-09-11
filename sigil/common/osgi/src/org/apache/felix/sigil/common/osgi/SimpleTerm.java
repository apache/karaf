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


import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;


public class SimpleTerm extends AbstractExpr
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final char WILDCARD = 2 ^ 16 - 1;
    private static final String WILDCARD_STRING = new String( new char[]
        { SimpleTerm.WILDCARD } );

    private Ops op;
    private String name;
    private String rval;


    public SimpleTerm( String name, Ops op, String value )
    {
        this.op = op;
        this.name = name.intern();
        this.rval = value.intern();
    }


    public String getName()
    {
        return name;
    }


    public Ops getOp()
    {
        return op;
    }


    public String getRval()
    {
        return rval;
    }


    public boolean eval( Map<String, ?> map )
    {

        Object lval = map.get( name );
        if ( lval == null )
        {
            return false;
        }
        else if ( Ops.EQ == op && WILDCARD_STRING.equals( lval ) )
        {
            return true;
        }
        // any match in the vector will do
        else if ( lval instanceof Vector<?> )
        {
            Vector<?> vec = ( Vector<?> ) lval;
            for ( Iterator<?> i = vec.iterator(); i.hasNext(); )
            {
                if ( check( i.next() ) )
                {
                    return true;
                }
            }
            return false;
        }
        // any match in the array will do
        else if ( lval instanceof Object[] )
        {
            Object[] arr = ( Object[] ) lval;
            for ( int i = 0; i < arr.length; i++ )
            {
                if ( check( arr[i] ) )
                {
                    return true;
                }
            }
            return false;
        }
        return check( lval );
    }


    @SuppressWarnings("unchecked")
    private boolean check( Object lval )
    {
        if ( lval == null )
        {
            return false;
        }
        else if ( Ops.EQ == op && WILDCARD_STRING.equals( lval ) )
        {
            return true;
        }

        Object rhs = null;

        if ( lval instanceof String )
        {

            if ( Ops.APPROX == op )
            {
                rhs = collapseWhiteSpace( rval );
                lval = collapseWhiteSpace( ( String ) lval );
            }

            if ( Ops.EQ == op || Ops.APPROX == op )
            {
                return stringCheck( ( String ) lval );
            }
            // rhs already a string

        }
        else if ( lval.getClass() == Byte.class )
        {
            rhs = Byte.valueOf( rval );
        }
        else if ( lval.getClass() == Short.class )
        {
            rhs = Short.valueOf( rval );
        }
        else if ( lval.getClass() == Integer.class )
        {
            rhs = Integer.valueOf( rval );
        }
        else if ( lval.getClass() == Long.class )
        {
            rhs = Long.valueOf( rval );
        }
        else if ( lval.getClass() == Float.class )
        {
            rhs = Float.valueOf( rval );
        }
        else if ( lval.getClass() == Double.class )
        {
            rhs = Double.valueOf( rval );
        }
        else
        {
            try
            {
                Constructor<?> stringCtor = lval.getClass().getConstructor( new Class[]
                    { String.class } );
                rhs = stringCtor.newInstance( rval );
            }
            catch ( Exception e )
            {
                // log it
                e.printStackTrace();
                return false;
            }
        }

        if ( !( lval instanceof Comparable ) )
        {
            return Ops.EQ == op && lval.equals( rval );
        }
        else
        {

            Comparable<? super Object> lhs = ( Comparable<? super Object> ) lval;

            int compare = lhs.compareTo( rhs );

            switch ( op )
            {
                case EQ:
                    return compare == 0;
                case APPROX:
                    return compare == 0;
                case GE:
                    return compare >= 0;
                case LE:
                    return compare <= 0;
                case GT:
                    return compare > 0;
                case LT:
                    return compare < 0;
            }
        }

        return false;
    }


    private boolean stringCheck( String lhs )
    {

        String rhs;
        switch ( op )
        {
            case EQ:
            case APPROX:
                rhs = rval;
                break;
            default:
                return false;
        }

        int valLength = lhs.length();
        int patLength = rval.length();

        if ( valLength == 0 && patLength == 0 )
        {
            return true;
        }

        boolean wc = false;
        int j = 0;
        for ( int i = 0; i < patLength; i++ )
        {
            // trailing wildcards
            char pc = rhs.charAt( i );
            if ( j == valLength )
            {
                if ( pc != SimpleTerm.WILDCARD )
                {
                    return false;
                }
                continue;
            }
            if ( pc == SimpleTerm.WILDCARD )
            {
                wc = true;
                continue;
            }
            while ( wc && j < valLength - 1 && lhs.charAt( j ) != pc )
            {
                j++;
            }
            if ( lhs.charAt( j ) != pc )
            {
                return false;
            }
            else
            {
                wc = false;
                j++;
            }
        }
        return ( wc || j == valLength );

    }


    private String collapseWhiteSpace( String in )
    {
        StringBuffer out = new StringBuffer( in.trim().length() );
        boolean white = false;
        for ( int i = 0; i < in.length(); i++ )
        {
            char ch = in.charAt( i );
            if ( Character.isWhitespace( ch ) )
            {
                white = true;
            }
            else
            {
                if ( white )
                {
                    out.append( " " );
                    white = false;
                }
                out.append( ch );
            }
        }
        return out.toString();
    }


    public LDAPExpr[] getChildren()
    {
        return CHILDLESS;
    }


    @Override
    public boolean equals( Object other )
    {
        if ( other instanceof SimpleTerm )
        {
            SimpleTerm that = ( SimpleTerm ) other;
            return name.equals( that.name ) && op.equals( that.op ) && rval.equals( that.rval );
        }
        return false;
    }


    @Override
    public String toString()
    {
        return "(" + name + " " + op.toString() + " " + escape( rval ) + ")";
    }


    private String escape( String raw )
    {
        StringBuffer buf = new StringBuffer( raw.length() + 10 );
        for ( int i = 0; i < raw.length(); i++ )
        {
            char ch = raw.charAt( i );
            switch ( ch )
            {
                case SimpleTerm.WILDCARD:
                    buf.append( "*" );
                    break;
                case '(':
                case ')':
                case '*':
                    buf.append( "\\" ).append( ch );
                    break;
                default:
                    buf.append( ch );
            }
        }
        return buf.toString();
    }
}

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


import static org.apache.felix.sigil.common.osgi.Expressions.and;
import static org.apache.felix.sigil.common.osgi.Expressions.not;
import static org.apache.felix.sigil.common.osgi.Expressions.or;
import static org.apache.felix.sigil.common.osgi.Ops.APPROX;
import static org.apache.felix.sigil.common.osgi.Ops.EQ;
import static org.apache.felix.sigil.common.osgi.Ops.GE;
import static org.apache.felix.sigil.common.osgi.Ops.GT;
import static org.apache.felix.sigil.common.osgi.Ops.LE;
import static org.apache.felix.sigil.common.osgi.Ops.LT;

import java.util.ArrayList;
import java.util.List;


public class LDAPParser
{

    private static final LDAPParser parser = new LDAPParser();


    public static LDAPExpr parseExpression( String strExpr ) throws LDAPParseException
    {
        return parser.parse( strExpr );
    }

    public LDAPExpr parse( String strExpr ) throws LDAPParseException
    {

        if ( strExpr == null || strExpr.trim().length() == 0 )
        {
            return LDAPExpr.ACCEPT_ALL;
        }

        ParseState ps = new ParseState( strExpr );
        LDAPExpr expr = parseExpr( ps );
        ps.skipWhitespace();
        if ( !ps.isEndOfString() )
        {
            error( "expected end of expression ", ps );
        }
        return expr;
    }


    public LDAPExpr parseExpr( ParseState ps ) throws LDAPParseException
    {
        ps.skipWhitespace();
        if ( !( ps.peek() == '(' ) )
        {
            error( "expected (", ps );
        }
        ps.read();
        LDAPExpr expr = null;
        ps.skipWhitespace();
        char ch = ps.peek();
        switch ( ch )
        {
            case '&':
                ps.readAndSkipWhiteSpace();
                List<LDAPExpr> andList = new ArrayList<LDAPExpr>();
                while ( ps.peek() == '(' )
                {
                    andList.add( parseExpr( ps ) );
                    ps.skipWhitespace();
                }
                LDAPExpr[] andArr = andList.toArray( new LDAPExpr[andList.size()] );
                expr = and( andArr );
                break;
            case '|':
                ps.readAndSkipWhiteSpace();
                List<LDAPExpr> orList = new ArrayList<LDAPExpr>();
                while ( ps.peek() == '(' )
                {
                    orList.add( parseExpr( ps ) );
                    ps.skipWhitespace();
                }
                LDAPExpr[] orArray = orList.toArray( new LDAPExpr[orList.size()] );
                expr = or( orArray );
                break;
            case '!':
                ps.readAndSkipWhiteSpace();
                expr = not( parseExpr( ps ) );
                break;
            default:
                if ( isNameChar( ch ) )
                {
                    expr = parseSimple( ps );
                }
                else
                {
                    error( "unexpected character: '" + ch + "'", ps );
                }
        }
        ps.skipWhitespace();
        if ( ps.peek() != ')' )
        {
            error( "expected )", ps );
        }
        ps.read();
        return expr;

    }


    void error( String message, ParseState ps ) throws LDAPParseException
    {
        throw new LDAPParseException( message, ps );
    }


    private SimpleTerm parseSimple( ParseState ps ) throws LDAPParseException
    {
        // read name
        StringBuffer name = new StringBuffer( 16 );
        for ( char c = ps.peek(); !ps.isEndOfString() && isNameChar( c ); c = ps.peek() )
        {
            ps.read();
            name.append( c );
        }
        ps.skipWhitespace();
        Ops op = null;
        // read op
        if ( ps.lookingAt( "=" ) )
        {
            op = EQ;
            ps.skip( 1 );
        }
        else if ( ps.lookingAt( ">=" ) )
        {
            op = GE;
            ps.skip( 2 );
        }
        else if ( ps.lookingAt( "<=" ) )
        {
            op = LE;
            ps.skip( 2 );
        }
        else if ( ps.lookingAt( ">" ) )
        {
            op = GT;
            ps.skip( 1 );
        }
        else if ( ps.lookingAt( "<" ) )
        {
            op = LT;
            ps.skip( 1 );
        }
        else if ( ps.lookingAt( "-=" ) )
        {
            op = APPROX;
            ps.skip( 2 );
        }
        else if ( ps.isEndOfString() )
        {
            error( "unexpected end of expression", ps );
        }
        else
        {
            error( "unexpected character: '" + ps.peek() + "'", ps );
        }
        ps.skipWhitespace();

        boolean escaped = false;
        StringBuffer value = new StringBuffer( 16 );

        while ( !ps.isEndOfString() && !Character.isWhitespace( ps.peek() ) && !( ps.peek() == ')' && !escaped ) )
        {

            char ch = ps.peek();

            if ( ch == '\\' )
            {
                escaped = true;
                ps.read();
            }
            else if ( ch == '*' )
            {
                if ( escaped )
                {
                    value.append( ch );
                    escaped = false;
                }
                else
                {
                    value.append( SimpleTerm.WILDCARD );
                }
                ps.read();
            }
            else if ( isLiteralValue( ch ) )
            {
                if ( escaped )
                {
                    error( "incorrectly applied escape of '" + ch + "'", ps );
                }
                value.append( ps.read() );
            }
            else if ( isEscapedValue( ch ) )
            {
                if ( !escaped )
                {
                    error( "missing escape for '" + ch + "'", ps );
                }
                value.append( ps.read() );
                escaped = false;
            }
            else
            {
                error( "unexpected character: '" + ps.peek() + "'", ps );
            }
        }
        ps.skipWhitespace();

        SimpleTerm expr = new SimpleTerm( name.toString(), op, value.toString() );

        return expr;
    }


    private boolean isNameChar( int ch )
    {
        return !( Character.isWhitespace( ch ) || ( ch == '(' ) || ( ch == ')' ) || ( ch == '<' ) || ( ch == '>' )
            || ( ch == '=' ) || ( ch == '~' ) || ( ch == '*' ) || ( ch == '\\' ) );
    }


    private boolean isLiteralValue( int ch )
    {
        return !( Character.isWhitespace( ch ) || ( ch == '(' ) || ( ch == ')' ) || ( ch == '*' ) );
    }


    private boolean isEscapedValue( int ch )
    {
        return ( ch == '(' ) || ( ch == ')' ) || ( ch == '*' );
    }
}

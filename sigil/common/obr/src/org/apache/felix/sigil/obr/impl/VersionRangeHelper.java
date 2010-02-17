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

package org.apache.felix.sigil.obr.impl;


import java.util.ArrayList;
import java.util.List;

import org.apache.felix.sigil.common.osgi.LDAPExpr;
import org.apache.felix.sigil.common.osgi.LDAPParseException;
import org.apache.felix.sigil.common.osgi.LDAPParser;
import org.apache.felix.sigil.common.osgi.Not;
import org.apache.felix.sigil.common.osgi.Ops;
import org.apache.felix.sigil.common.osgi.SimpleTerm;
import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionTable;
import org.osgi.framework.Version;


class VersionRangeHelper
{

    // e.g. (&(version>=1.0.0)(version<=2.0.0)) (&(version>1.0.0)(version<2.0.0)) (&(!(version<1.0.0))(!(version>2.0.0))) (&(!(version<=1.0.0))(!(version>=2.0.0))) (version=1.0.0) (version>=1.0.0) (version<=2.0.0) (version>1.0.0) (version<2.0.0)  (!(version>2.0.0)) (!(version<1.0.0)) (!(version>=2.0.0)) (!(version<=1.0.0))
    public static void main( String[] args ) throws LDAPParseException
    {
        for ( String arg : args )
        {
            LDAPExpr expr = LDAPParser.parseExpression( arg.trim() );
            System.out.println( expr + " -> " + decodeVersions( expr ) );
        }
    }


    static VersionRange decodeVersions( LDAPExpr expr ) throws NumberFormatException
    {
        ArrayList<LDAPExpr> terms = new ArrayList<LDAPExpr>( 1 );

        findExpr( "version", expr, terms );

        if ( terms.isEmpty() )
        {
            // woo hoo!
            return VersionRange.ANY_VERSION;
        }
        else
        {
            switch ( terms.size() )
            {
                case 1:
                {
                    return parseSimpleVersionRange( terms.get( 0 ) );
                }
                case 2:
                {
                    return parseCompoundVersionRange( terms.get( 0 ), terms.get( 1 ) );
                }
                default:
                {
                    // (&(version>=min)(!(version=min))(version<=max)(!(version=max))) 	- (min,max) - not dealt with!!
                    // (&(|(version>min)(version=min))(|(version<max)(version=max))) 	- [min,max] - not dealt with!!
                    throw new NumberFormatException( "Failed to parse complicated version expression " + expr );
                }
            }
        }
    }


    // (&(version>=min)(version<=max)) 									- [min,max]
    // (&(version>min)(version<max))									- (min,max)
    //
    // (&(!(version<min))(!(version>max)))								- [min,max]
    // (&(!(version<=min))(!(version>=max)) 							- (min,max)
    private static VersionRange parseCompoundVersionRange( LDAPExpr left, LDAPExpr right ) throws NumberFormatException
    {
        VersionRange one = parseSimpleVersionRange( left );
        VersionRange two = parseSimpleVersionRange( right );

        // sanity check
        if ( one.isPointVersion() || two.isPointVersion() )
        {
            throw new NumberFormatException( "Unexpected point version in compound expression " + left );
        }

        VersionRange max = one.getFloor().equals( Version.emptyVersion ) ? one : two;
        VersionRange min = max == one ? two : one;

        return new VersionRange( min.isOpenFloor(), min.getFloor(), max.getCeiling(), max.isOpenCeiling() );
    }


    // possible variations				
    // (version=v)														- [v,v]
    //
    // (version>=min)													- [min,*)
    // (version<=max)													- [0,max]
    //
    // (version>min)													- (min,*)
    // (version<max)													- [0,max)
    //
    // (!(version>max))													- [0,max]
    // (!(version<min))													- [min,*)
    // (!(version>=max))												- [0,max)
    // (!(version<=min))												- (0,*)
    private static VersionRange parseSimpleVersionRange( LDAPExpr expr ) throws NumberFormatException
    {
        Version min = Version.emptyVersion;
        Version max = VersionRange.INFINITE_VERSION;
        boolean openFloor = false;
        boolean openCeiling = false;
        if ( expr instanceof Not )
        {
            Not n = ( Not ) expr;
            SimpleTerm t = ( SimpleTerm ) n.getEx();
            if ( t.getOp() == Ops.EQ )
            {
                throw new NumberFormatException( "Unexpected point version in negated expression " + expr );
            }
            if ( !isMax( t.getOp() ) )
            {
                max = toVersion( t );
                openCeiling = !openFloor( t );
            }
            else if ( !isMin( t.getOp() ) )
            {
                min = toVersion( t );
                openFloor = !openCeiling( t );
            }
            else
            {
                throw new IllegalStateException( "Unexpected operator " + t.getOp() );
            }
        }
        else
        {
            SimpleTerm t = ( SimpleTerm ) expr;
            if ( t.getOp().equals( Ops.EQ ) )
            {
                max = toVersion( t );
                min = max;
                openFloor = false;
                openCeiling = false;
            }
            else if ( isMax( t.getOp() ) )
            {
                max = toVersion( t );
                openCeiling = openCeiling( t );
            }
            else if ( isMin( t.getOp() ) )
            {
                min = toVersion( t );
                openFloor = openFloor( t );
            }
            else
            {
                throw new IllegalStateException( "Unexpected operator " + t.getOp() );
            }
        }

        return new VersionRange( openFloor, min, max, openCeiling );
    }


    private static Version toVersion( SimpleTerm t )
    {
        return VersionTable.getVersion( t.getRval() );
    }


    private static boolean isMax( Ops op )
    {
        return op == Ops.LE || op == Ops.LT;
    }


    private static boolean isMin( Ops op )
    {
        return op == Ops.GE || op == Ops.GT;
    }


    private static boolean openFloor( SimpleTerm t )
    {
        return t.getOp() == Ops.GT;
    }


    private static boolean openCeiling( SimpleTerm t )
    {
        return t.getOp() == Ops.LT;
    }


    private static void findExpr( String string, LDAPExpr expr, List<LDAPExpr> terms )
    {
        if ( expr instanceof SimpleTerm )
        {
            SimpleTerm term = ( SimpleTerm ) expr;
            if ( term.getName().equals( string ) )
            {
                terms.add( term );
            }
        }
        else if ( expr instanceof Not )
        {
            Not not = ( Not ) expr;
            if ( not.getEx() instanceof SimpleTerm )
            {
                SimpleTerm term = ( SimpleTerm ) not.getEx();
                if ( term.getName().equals( string ) )
                {
                    terms.add( not );
                }
            }
        }
        else
        {
            for ( LDAPExpr c : expr.getChildren() )
            {
                findExpr( string, c, terms );
            }
        }
    }
}

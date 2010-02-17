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


import java.io.Serializable;

import org.osgi.framework.Version;


public class VersionRange implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final Version INFINITE_VERSION = new Version( Integer.MAX_VALUE, Integer.MAX_VALUE,
        Integer.MAX_VALUE, "" );
    public static final VersionRange ANY_VERSION = new VersionRange( false, Version.emptyVersion, INFINITE_VERSION,
        true );

    private boolean openFloor;
    private Version floor;
    private Version ceiling;
    private boolean openCeiling;


    /**
     * Interval constructor
     * 
     * @param openFloor Whether the lower bound of the range is inclusive (false) or exclusive (true).
     * @param floor The lower bound version of the range.
     * @param ceiling The upper bound version of the range.
     * @param openCeiling Whether the upper bound of the range is inclusive (false) or exclusive (true).
     */
    public VersionRange( boolean openFloor, Version floor, Version ceiling, boolean openCeiling )
    {
        this.openFloor = openFloor;
        this.floor = floor;
        this.ceiling = ceiling;
        this.openCeiling = openCeiling;
    }


    /**
     * atLeast constructor
     * 
     * @param openFloor
     * @param floor
     */
    public VersionRange( Version atLeast )
    {
        this.openFloor = false;
        this.floor = atLeast;
        this.ceiling = INFINITE_VERSION;
        this.openCeiling = true;
    }


    public static VersionRange parseVersionRange( String val ) throws IllegalArgumentException, NumberFormatException
    {
        if ( val == null || val.trim().length() == 0 )
        {
            return ANY_VERSION;
        }

        boolean openFloor;
        boolean openCeiling;
        val = val.replaceAll( "\\s", "" );
        val = val.replaceAll( "\"", "" );
        int fst = val.charAt( 0 );
        if ( fst == '[' )
        {
            openFloor = false;
        }
        else if ( fst == '(' )
        {
            openFloor = true;
        }
        else
        {
            Version atLeast = VersionTable.getVersion( val );
            return new VersionRange( atLeast );
        }

        int lst = val.charAt( val.length() - 1 );
        if ( lst == ']' )
        {
            openCeiling = false;
        }
        else if ( lst == ')' )
        {
            openCeiling = true;
        }
        else
        {
            throw new IllegalArgumentException( "illegal version range syntax " + val
                + ": range must end in ')' or ']'" );
        }

        String inner = val.substring( 1, val.length() - 1 );
        String[] floorCeiling = inner.split( "," );
        if ( floorCeiling.length != 2 )
        {
            throw new IllegalArgumentException( "illegal version range syntax " + "too many commas" );
        }
        Version floor = VersionTable.getVersion( floorCeiling[0] );
        Version ceiling = "*".equals( floorCeiling[1] ) ? INFINITE_VERSION : Version.parseVersion( floorCeiling[1] );
        return new VersionRange( openFloor, floor, ceiling, openCeiling );
    }


    public Version getCeiling()
    {
        return ceiling;
    }


    public Version getFloor()
    {
        return floor;
    }


    public boolean isOpenCeiling()
    {
        return openCeiling;
    }


    public boolean isOpenFloor()
    {
        return openFloor;
    }


    public boolean isPointVersion()
    {
        return !openFloor && !openCeiling && floor.equals( ceiling );
    }


    /**
     * test a version to see if it falls in the range
     * 
     * @param version
     * @return
     */
    public boolean contains( Version version )
    {
        if ( version.equals( INFINITE_VERSION ) )
        {
            return ceiling.equals( INFINITE_VERSION );
        }
        else
        {
            return ( version.compareTo( floor ) > 0 && version.compareTo( ceiling ) < 0 )
                || ( !openFloor && version.equals( floor ) ) || ( !openCeiling && version.equals( ceiling ) );
        }
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( ceiling == null ) ? 0 : ceiling.hashCode() );
        result = prime * result + ( ( floor == null ) ? 0 : floor.hashCode() );
        result = prime * result + ( openCeiling ? 1231 : 1237 );
        result = prime * result + ( openFloor ? 1231 : 1237 );
        return result;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final VersionRange other = ( VersionRange ) obj;
        if ( ceiling == null )
        {
            if ( other.ceiling != null )
                return false;
        }
        else if ( !ceiling.equals( other.ceiling ) )
            return false;
        if ( floor == null )
        {
            if ( other.floor != null )
                return false;
        }
        else if ( !floor.equals( other.floor ) )
            return false;
        if ( openCeiling != other.openCeiling )
            return false;
        if ( openFloor != other.openFloor )
            return false;
        return true;
    }


    @Override
    public String toString()
    {
        if ( ANY_VERSION.equals( this ) )
        {
            return makeString( openFloor, Version.emptyVersion, INFINITE_VERSION, openCeiling );
        }
        return makeString( openFloor, floor, ceiling, openCeiling );
    }


    private String makeString( boolean openFloor, Version floor, Version ceiling, boolean openCeiling )
    {
        StringBuffer vr = new StringBuffer( 32 );
        if ( INFINITE_VERSION.equals( ceiling ) )
        {
            vr.append( Version.emptyVersion.equals( floor ) ? "0" : floor.toString() );
        }
        else
        {
            vr.append( openFloor ? "(" : "[" );
            String floorStr = Version.emptyVersion.equals( floor ) ? "0" : floor.toString();
            String ceilingStr = ceiling.toString();
            vr.append( floorStr ).append( "," ).append( ceilingStr );
            vr.append( openCeiling ? ")" : "]" );
        }
        return vr.toString();
    }


    public static VersionRange newInstance( Version pointVersion, VersionRangeBoundingRule lowerBoundRule,
        VersionRangeBoundingRule upperBoundRule )
    {
        Version floor = null;
        switch ( lowerBoundRule )
        {
            case Any:
                floor = VersionTable.getVersion( 0, 0, 0 );
                break;
            case Major:
                floor = VersionTable.getVersion( pointVersion.getMajor(), 0, 0 );
                break;
            case Minor:
                floor = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor(), 0 );
                break;
            case Micro:
                floor = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor(), pointVersion.getMicro() );
                break;
            case Exact:
                floor = pointVersion;
                break;
        }

        Version ceiling = null;
        boolean openCeiling = true;
        switch ( upperBoundRule )
        {
            case Any:
                ceiling = INFINITE_VERSION;
                break;
            case Major:
                ceiling = VersionTable.getVersion( pointVersion.getMajor() + 1, 0, 0 );
                break;
            case Minor:
                ceiling = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor() + 1, 0 );
                break;
            case Micro:
                ceiling = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor(), pointVersion.getMicro() + 1 );
                break;
            case Exact:
                ceiling = pointVersion;
                openCeiling = false;
                break;
        }

        return new VersionRange( false, floor, ceiling, openCeiling );
    }
}

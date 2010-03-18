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
package org.apache.felix.utils.version;

import java.io.Serializable;

import org.osgi.framework.Version;

public class VersionRange implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final Version INFINITE_VERSION = new Version( Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, "" );
    public static final VersionRange ANY_VERSION = new VersionRange( false, Version.emptyVersion, INFINITE_VERSION, true );

    public static final int EXACT = 0;
    public static final int MICRO = 1;
    public static final int MINOR = 2;
    public static final int MAJOR = 3;
    public static final int ANY   = 40;

    private final boolean openFloor;
    private final Version floor;
    private final Version ceiling;
    private final boolean openCeiling;


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
        checkRange();
    }


    /**
     * atLeast constructor
     *
     * @param atLeast
     */
    public VersionRange( Version atLeast )
    {
        this( atLeast, false );
    }

    /**
     * atLeast constructor
     *
     * @param atLeast
     */
    public VersionRange( Version atLeast, boolean exact )
    {

        this.openFloor = false;
        this.floor = atLeast;
        this.ceiling = exact ? atLeast : INFINITE_VERSION;
        this.openCeiling = exact ? false : true;
        checkRange();
    }


    public VersionRange( String val ) throws IllegalArgumentException, NumberFormatException
    {
        this( val, false );
    }

    public VersionRange( String val, boolean exact ) throws IllegalArgumentException, NumberFormatException
    {
        this( val, exact, true );
    }

    public VersionRange( String val, boolean exact, boolean clean ) throws IllegalArgumentException, NumberFormatException
    {
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
            openFloor = false;
            floor = VersionTable.getVersion( val, clean );
            ceiling = exact ? floor : INFINITE_VERSION;
            openCeiling = exact ? false : true;
            return;
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
        floor = VersionTable.getVersion( floorCeiling[0], clean );
        ceiling = "*".equals( floorCeiling[1] ) ? INFINITE_VERSION : VersionTable.getVersion( floorCeiling[1], clean );
        checkRange();
    }

    public static VersionRange parseVersionRange( String val ) throws IllegalArgumentException, NumberFormatException
    {
        if ( val == null || val.trim().length() == 0 )
        {
            return ANY_VERSION;
        }

        return new VersionRange( val );
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

    /*
    * (non-Javadoc)
    *
    * @see org.apache.aries.application.impl.VersionRange#intersect(VersionRange
    * range)
    */

    public VersionRange intersect(VersionRange r)
    {
        // Use the highest minimum version.
        final Version newFloor;
        final boolean newOpenFloor;
        int minCompare = floor.compareTo(r.getFloor());
        if (minCompare > 0)
        {
            newFloor = floor;
            newOpenFloor = openFloor;
        }
        else if (minCompare < 0)
        {
            newFloor = r.getFloor();
            newOpenFloor = r.isOpenFloor();
        }
        else
        {
            newFloor = floor;
            newOpenFloor = (openFloor || r.isOpenFloor());
        }

        // Use the lowest maximum version.
        final Version newCeiling;
        final boolean newOpenCeiling;
        // null maximum version means unbounded, so the highest possible value.
        int maxCompare = ceiling.compareTo(r.getCeiling());
        if (maxCompare < 0)
        {
            newCeiling = ceiling;
            newOpenCeiling = openCeiling;
        }
        else if (maxCompare > 0)
        {
            newCeiling = r.getCeiling();
            newOpenCeiling = r.isOpenCeiling();
        }
        else
        {
            newCeiling = ceiling;
            newOpenCeiling = (openCeiling || r.isOpenCeiling());
        }

        VersionRange result;
        if (isRangeValid(newOpenFloor, newFloor, newCeiling, newOpenCeiling))
        {
            result = new VersionRange(newOpenFloor, newFloor, newCeiling, newOpenCeiling);
        }
        else
        {
            result = null;
        }
        return result;
    }

    /**
     * Check if the supplied parameters describe a valid version range.
     *
     * @param floor
     *          the minimum version.
     * @param openFloor
     *          whether the minimum version is exclusive.
     * @param ceiling
     *          the maximum version.
     * @param openCeiling
     *          whether the maximum version is exclusive.
     * @return true is the range is valid; otherwise false.
     */
    private static boolean isRangeValid(boolean openFloor, Version floor, Version ceiling, boolean openCeiling) {
        boolean result;
        int compare = floor.compareTo(ceiling);
        if (compare > 0)
        {
            // Minimum larger than maximum is invalid.
            result = false;
        }
        else if (compare == 0 && (openFloor || openCeiling))
        {
            // If floor and ceiling are the same, and either are exclusive, no valid range
            // exists.
            result = false;
        }
        else
        {
            // Range is valid.
            result = true;
        }
        return result;
    }

    private void checkRange()
    {
        if (!isRangeValid(openFloor, floor, ceiling, openCeiling))
        {
            throw new IllegalArgumentException("invalid version range: " + makeString(openFloor, floor, ceiling, openCeiling));
        }
    }


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


    public static VersionRange newInstance( Version pointVersion,
                                            int lowerBoundRule,
                                            int upperBoundRule )
    {
        Version floor = null;
        switch ( lowerBoundRule )
        {
            case ANY:
                floor = VersionTable.getVersion( 0, 0, 0 );
                break;
            case MAJOR:
                floor = VersionTable.getVersion( pointVersion.getMajor(), 0, 0 );
                break;
            case MINOR:
                floor = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor(), 0 );
                break;
            case MICRO:
                floor = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor(), pointVersion.getMicro() );
                break;
            case EXACT:
                floor = pointVersion;
                break;
        }

        Version ceiling = null;
        boolean openCeiling = true;
        switch ( upperBoundRule )
        {
            case ANY:
                ceiling = INFINITE_VERSION;
                break;
            case MAJOR:
                ceiling = VersionTable.getVersion( pointVersion.getMajor() + 1, 0, 0 );
                break;
            case MINOR:
                ceiling = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor() + 1, 0 );
                break;
            case MICRO:
                ceiling = VersionTable.getVersion( pointVersion.getMajor(), pointVersion.getMinor(), pointVersion.getMicro() + 1 );
                break;
            case EXACT:
                ceiling = pointVersion;
                openCeiling = false;
                break;
        }

        return new VersionRange( false, floor, ceiling, openCeiling );
    }
}

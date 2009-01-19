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
package org.apache.felix.cm.impl;


import java.util.Comparator;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;


/**
 * The <code>RankingComparator</code> TODO
 */
public class RankingComparator implements Comparator
{

    private final boolean naturalOrder;
    private final String rankProperty;


    public RankingComparator(boolean naturalOrder)
    {
        this( naturalOrder, Constants.SERVICE_RANKING );
    }


    public RankingComparator( boolean naturalOrder, String rankProperty )
    {
        this.naturalOrder = naturalOrder;
        this.rankProperty = rankProperty;
    }


    public int compare( Object obj1, Object obj2 )
    {
        if ( obj1.equals( obj2 ) )
        {
            return 0;
        }

        long rank1 = this.getLong( ( ServiceReference ) obj1, rankProperty );
        long rank2 = this.getLong( ( ServiceReference ) obj2, rankProperty );
        boolean order = naturalOrder;

        // use service id, if rankings are equal
        if ( rank1 == rank2 )
        {
            rank1 = this.getLong( ( ServiceReference ) obj1, Constants.SERVICE_ID );
            rank2 = this.getLong( ( ServiceReference ) obj2, Constants.SERVICE_ID );
            order = false; // always order lower service.id before higher
        }

        if ( rank1 == rank2 )
        {
            return 0;
        }
        else if ( order )
        {
            return ( rank1 > rank2 ) ? 1 : -1;
        }
        else
        {
            return ( rank1 < rank2 ) ? 1 : -1;
        }
    }


    private long getLong( ServiceReference sr, String property )
    {
        Object rankObj = sr.getProperty( property );
        if ( rankObj instanceof Number )
        {
            return ( ( Number ) rankObj ).longValue();
        }

        // null or not an integer
        return 0;
    }

}

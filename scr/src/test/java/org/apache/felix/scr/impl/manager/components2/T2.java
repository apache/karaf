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
package org.apache.felix.scr.impl.manager.components2;


import java.util.Map;

import org.apache.felix.scr.impl.manager.components.FakeService;
import org.apache.felix.scr.impl.manager.components.SuperFakeService;
import org.apache.felix.scr.impl.manager.components.T1;
import org.osgi.framework.ServiceReference;


public class T2 extends T1
{
    private void privateT2()
    {
        callPerformed = "privateT2";
    }


    private void privateT2SR( ServiceReference sr )
    {
        if ( sr != null )
        {
            callPerformed = "privateT2SR";
        }
        else
        {
            callPerformed = "privateT2SR with null param";
        }
    }


    private void privateT2SI( FakeService si )
    {
        if ( si != null )
        {
            callPerformed = "privateT2SI";
        }
        else
        {
            callPerformed = "privateT2SI with null param";
        }
    }


    private void privateT2SIMap( FakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "privateT2SIMap";
        }
        else if ( si == null )
        {
            callPerformed = "privateT2SIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "privateT2SIMap with null props";
        }
        else
        {
            callPerformed = "privateT2SIMap with empty props";
        }
    }


    private void privateT2SSI( SuperFakeService si )
    {
        if ( si != null )
        {
            callPerformed = "privateT2SSI";
        }
        else
        {
            callPerformed = "privateT2SSI with null param";
        }
    }


    private void privateT2SSIMap( SuperFakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "privateT2SSIMap";
        }
        else if ( si == null )
        {
            callPerformed = "privateT2SSIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "privateT2SSIMap with null props";
        }
        else
        {
            callPerformed = "privateT2SSIMap with empty props";
        }
    }


    void packageT2()
    {
        callPerformed = "packageT2";
    }


    void packageT2SR( ServiceReference sr )
    {
        if ( sr != null )
        {
            callPerformed = "packageT2SR";
        }
        else
        {
            callPerformed = "packageT2SR with null param";
        }
    }


    void packageT2SI( FakeService si )
    {
        if ( si != null )
        {
            callPerformed = "packageT2SI";
        }
        else
        {
            callPerformed = "packageT2SI with null param";
        }
    }


    void packageT2SIMap( FakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "packageT2SIMap";
        }
        else if ( si == null )
        {
            callPerformed = "packageT2SIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "packageT2SIMap with null props";
        }
        else
        {
            callPerformed = "packageT2SIMap with empty props";
        }
    }


    void packageT2SSI( SuperFakeService si )
    {
        if ( si != null )
        {
            callPerformed = "packageT2SSI";
        }
        else
        {
            callPerformed = "packageT2SSI with null param";
        }
    }


    void packageT2SSIMap( SuperFakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "packageT2SSIMap";
        }
        else if ( si == null )
        {
            callPerformed = "packageT2SSIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "packageT2SSIMap with null props";
        }
        else
        {
            callPerformed = "packageT2SSIMap with empty props";
        }
    }


    // this method must hide the T1#suitable method !
    private void suitable( FakeService si )
    {
        callPerformed = "suitableT2";
    }
}
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
package org.apache.felix.scr.impl.manager.components;


import java.util.Map;

import org.osgi.framework.ServiceReference;


public class T1
{

    public String callPerformed = null;


    private void privateT1()
    {
        callPerformed = "privateT1";
    }


    private void privateT1SR( ServiceReference sr )
    {
        if ( sr != null )
        {
            callPerformed = "privateT1SR";
        }
        else
        {
            callPerformed = "privateT1SR with null param";
        }
    }


    private void privateT1SI( FakeService si )
    {
        if ( si != null )
        {
            callPerformed = "privateT1SI";
        }
        else
        {
            callPerformed = "privateT1SI with null param";
        }
    }


    private void privateT1SIMap( FakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "privateT1SIMap";
        }
        else if ( si == null )
        {
            callPerformed = "privateT1SIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "privateT1SIMap with null props";
        }
        else
        {
            callPerformed = "privateT1SIMap with empty props";
        }
    }


    private void privateT1SSI( SuperFakeService si )
    {
        if ( si != null )
        {
            callPerformed = "privateT1SSI";
        }
        else
        {
            callPerformed = "privateT1SSI with null param";
        }
    }


    private void privateT1SSIMap( SuperFakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "privateT1SSIMap";
        }
        else if ( si == null )
        {
            callPerformed = "privateT1SSIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "privateT1SSIMap with null props";
        }
        else
        {
            callPerformed = "privateT1SSIMap with empty props";
        }
    }


    void packageT1()
    {
        callPerformed = "packageT1";
    }


    void packageT1SR( ServiceReference sr )
    {
        if ( sr != null )
        {
            callPerformed = "packageT1SR";
        }
        else
        {
            callPerformed = "packageT1SR with null param";
        }
    }


    void packageT1SI( FakeService si )
    {
        if ( si != null )
        {
            callPerformed = "packageT1SI";
        }
        else
        {
            callPerformed = "packageT1SI with null param";
        }
    }


    void packageT1SIMap( FakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "packageT1SIMap";
        }
        else if ( si == null )
        {
            callPerformed = "packageT1SIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "packageT1SIMap with null props";
        }
        else
        {
            callPerformed = "packageT1SIMap with empty props";
        }
    }


    void packageT1SSI( SuperFakeService si )
    {
        if ( si != null )
        {
            callPerformed = "packageT1SSI";
        }
        else
        {
            callPerformed = "packageT1SSI with null param";
        }
    }


    void packageT1SSIMap( SuperFakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "packageT1SSIMap";
        }
        else if ( si == null )
        {
            callPerformed = "packageT1SSIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "packageT1SSIMap with null props";
        }
        else
        {
            callPerformed = "packageT1SSIMap with empty props";
        }
    }


    protected void protectedT1()
    {
        callPerformed = "protectedT1";
    }


    protected void protectedT1SR( ServiceReference sr )
    {
        if ( sr != null )
        {
            callPerformed = "protectedT1SR";
        }
        else
        {
            callPerformed = "protectedT1SR with null param";
        }
    }


    protected void protectedT1SI( FakeService si )
    {
        if ( si != null )
        {
            callPerformed = "protectedT1SI";
        }
        else
        {
            callPerformed = "protectedT1SI with null param";
        }
    }


    protected void protectedT1SIMap( FakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "protectedT1SIMap";
        }
        else if ( si == null )
        {
            callPerformed = "protectedT1SIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "protectedT1SIMap with null props";
        }
        else
        {
            callPerformed = "protectedT1SIMap with empty props";
        }
    }


    protected void protectedT1SSI( SuperFakeService si )
    {
        if ( si != null )
        {
            callPerformed = "protectedT1SSI";
        }
        else
        {
            callPerformed = "protectedT1SSI with null param";
        }
    }


    protected void protectedT1SSIMap( SuperFakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "protectedT1SSIMap";
        }
        else if ( si == null )
        {
            callPerformed = "protectedT1SSIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "protectedT1SSIMap with null props";
        }
        else
        {
            callPerformed = "protectedT1SSIMap with empty props";
        }
    }


    public void publicT1()
    {
        callPerformed = "publicT1";
    }


    public void publicT1SR( ServiceReference sr )
    {
        if ( sr != null )
        {
            callPerformed = "publicT1SR";
        }
        else
        {
            callPerformed = "publicT1SR with null param";
        }
    }


    public void publicT1SI( FakeService si )
    {
        if ( si != null )
        {
            callPerformed = "publicT1SI";
        }
        else
        {
            callPerformed = "publicT1SI with null param";
        }
    }


    public void publicT1SIMap( FakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "publicT1SIMap";
        }
        else if ( si == null )
        {
            callPerformed = "publicT1SIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "publicT1SIMap with null props";
        }
        else
        {
            callPerformed = "publicT1SIMap with empty props";
        }
    }


    public void publicT1SSI( SuperFakeService si )
    {
        if ( si != null )
        {
            callPerformed = "publicT1SSI";
        }
        else
        {
            callPerformed = "publicT1SSI with null param";
        }
    }


    public void publicT1SSIMap( SuperFakeService si, Map props )
    {
        if ( si != null && props != null && props.size() > 0 )
        {
            callPerformed = "publicT1SSIMap";
        }
        else if ( si == null )
        {
            callPerformed = "publicT1SSIMap with null service instance";
        }
        else if ( props == null )
        {
            callPerformed = "publicT1SSIMap with null props";
        }
        else
        {
            callPerformed = "publicT1SSIMap with empty props";
        }
    }


    public void suitable( ServiceReference sr )
    {
        callPerformed = "suitableT1";
    }
}
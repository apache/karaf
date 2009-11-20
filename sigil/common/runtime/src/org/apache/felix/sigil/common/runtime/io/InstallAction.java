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

package org.apache.felix.sigil.common.runtime.io;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import static org.apache.felix.sigil.common.runtime.io.Constants.INSTALL;


/**
 * @author dave
 *
 */
public class InstallAction extends Action<String, Long>
{

    public InstallAction( DataInputStream in, DataOutputStream out ) throws IOException
    {
        super( in, out );
    }


    @Override
    public Long client( String url ) throws IOException, BundleException
    {
        writeInt( INSTALL );
        writeString( url );
        flush();
        if ( checkOk() )
        {
            return readLong();
        }
        else
        {
            String msg = readString();
            throw new BundleException( msg );
        }
    }


    @Override
    public void server( Framework fw ) throws IOException
    {
        String url = readString();
        try
        {
            Bundle val = fw.getBundleContext().installBundle( url );
            log( "Installed " + url );
            writeOk();
            writeLong( val.getBundleId() );
        }
        catch ( BundleException e )
        {
            e.printStackTrace();
            writeError();
            writeThrowable( e );
        }
        flush();
    }

}

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

import static org.apache.felix.sigil.common.runtime.io.Constants.UNINSTALL;


/**
 * @author dave
 *
 */
public class UninstallAction extends Action<Long, Void>
{

    public UninstallAction( DataInputStream in, DataOutputStream out ) throws IOException
    {
        super( in, out );
    }


    @Override
    public Void client( Long bundle ) throws IOException, BundleException
    {
        writeInt( UNINSTALL );
        writeLong( bundle );
        if ( !checkOk() )
        {
            String msg = readString();
            throw new BundleException( msg );
        }
        return null;
    }


    @Override
    public void server( Framework fw ) throws IOException
    {
        long id = readLong();
        Bundle b = fw.getBundleContext().getBundle( id );
        if ( b == null )
        {
            writeError();
            writeString( "Unknown bundle " + id );
        }
        else
        {
            try
            {
                b.uninstall();
                writeOk();
            }
            catch ( BundleException e )
            {
                writeError();
                writeThrowable( e );
            }
        }
    }

}

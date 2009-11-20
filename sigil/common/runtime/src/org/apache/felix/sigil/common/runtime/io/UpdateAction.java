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
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import static org.apache.felix.sigil.common.runtime.io.Constants.UPDATE;


/**
 * @author dave
 *
 */
public class UpdateAction extends Action<UpdateAction.Update, Void>
{
    public static class Update
    {
        final long bundleID;
        final String location;


        public Update( long bundleID, String location )
        {
            this.bundleID = bundleID;
            this.location = location;
        }
    }


    public UpdateAction( DataInputStream in, DataOutputStream out ) throws IOException
    {
        super( in, out );
        // TODO Auto-generated constructor stub
    }


    @Override
    public Void client( Update update ) throws IOException, BundleException
    {
        writeInt( UPDATE );
        writeLong( update.bundleID );
        if ( update.location == null )
        {
            writeBoolean( false );
        }
        else
        {
            writeBoolean( true );
            writeString( update.location );
        }
        flush();

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
                boolean remote = readBoolean();
                if ( remote )
                {
                    String loc = readString();
                    try {
                        InputStream in = open( loc );
                        try {
                            b.update(in);
                            writeOk();
                        }
                        finally {
                            in.close();
                        }
                    }
                    catch (IOException e) {
                        writeError();
                        writeThrowable( e );
                    }
                }
                else
                {
                    b.update();
                    writeOk();
                }
            }
            catch ( BundleException e )
            {
                writeError();
                writeString( e.getMessage() );
            }
        }

        flush();
    }


    private InputStream open( String loc ) throws IOException
    {
        URL url = new URL( loc );
        return url.openStream();
    }
}

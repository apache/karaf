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
import java.net.URLConnection;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.felix.sigil.common.runtime.Main;

import static org.apache.felix.sigil.common.runtime.io.Constants.OK;
import static org.apache.felix.sigil.common.runtime.io.Constants.ERROR;


/**
 * @author dave
 *
 */
public abstract class Action<I, O>
{
    private static final String ASCII = "ASCII";
    private final DataInputStream in;
    private final DataOutputStream out;


    public Action( DataInputStream in, DataOutputStream out ) throws IOException
    {
        this.in = in;
        this.out = out;
    }


    public O client() throws IOException, BundleException
    {
        return client( null );
    }


    public abstract O client( I input ) throws IOException, BundleException;


    public abstract void server( Framework fw ) throws IOException;


    protected boolean checkOk() throws IOException
    {
        int ch = readInt();
        switch ( ch )
        {
            case OK:
                return true;
            case ERROR:
                return false;
            default:
                throw new IOException( "Unexpected return code " + ch );
        }
    }


    protected void writeOk() throws IOException
    {
        writeInt( OK );
    }


    protected void writeError() throws IOException
    {
        writeInt( ERROR );
    }


    protected String readString() throws IOException
    {
        int l = in.readInt();
        byte[] buf = new byte[l];
        in.readFully( buf );
        return new String(buf, ASCII);
    }


    protected void writeString( String str ) throws IOException
    {
        byte[] buf = str.getBytes( ASCII );
        out.writeInt( buf.length );
        out.write( buf );
    }


    protected void writeInt( int i ) throws IOException
    {
        out.writeInt( i );
    }


    protected int readInt() throws IOException
    {
        return in.readInt();
    }


    protected void writeLong( long l ) throws IOException
    {        
        out.writeLong( l );
    }


    protected long readLong() throws IOException
    {
        return in.readLong();
    }

    protected void writeBoolean( boolean b ) throws IOException
    {
        out.writeBoolean( b );
    }
    
    protected boolean readBoolean() throws IOException {
        return in.readBoolean();
    }
    
    protected void writeStream( String location ) throws IOException {
        URL url = new URL( location );
        URLConnection conn = url.openConnection();
        conn.connect();
        
        int l = conn.getContentLength();
        writeInt( l );
        InputStream uin = conn.getInputStream();
        byte[] buf = new byte[1024*1024];
        for (;;) {
            int r = uin.read( buf );
            if ( r == -1 ) break;
            out.write( buf, 0, r );
        }
    }
    
    protected InputStream readStream() throws IOException {
        final int length = readInt();
        return new CountingInputStream(in) {
            @Override
            public int read() throws IOException
            {
                if ( getCount() < length )
                    return super.read();
                else
                    return -1;
            }

            @Override
            public int read( byte[] b, int off, int len ) throws IOException
            {
                len = (getCount() + len) > length ? (length - getCount()) : len;
                if ( len == 0 )
                    return -1;
                else 
                    return super.read( b, off, len );
            }

            @Override
            public int read( byte[] b ) throws IOException
            {
                return read( b, 0, b.length );
            }

            @Override
            public long skip( long len ) throws IOException
            {
                len = (getCount() + len) > length ? (length - getCount()) : len;
                if ( len == 0 )
                    return -1;
                else 
                    return super.skip( len );
            }  
        };
    }
    
    protected void flush() throws IOException
    {
        out.flush();
    }
    
    protected void log(String msg) {
        Main.log( msg );
    }
}

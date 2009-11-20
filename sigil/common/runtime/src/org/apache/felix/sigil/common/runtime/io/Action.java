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


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

import org.apache.felix.sigil.common.runtime.Main;

import static org.apache.felix.sigil.common.runtime.io.Constants.OK;
import static org.apache.felix.sigil.common.runtime.io.Constants.ERROR;


/**
 * @author dave
 *
 */
public abstract class Action<I, O>
{
    private static final String PREFIX = "\t";
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

    protected void writeThrowable(Throwable t) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream( bos ));
        writeString( bos.toString() );
    }

    protected String readString() throws IOException
    {
        int l = in.readInt();
        if ( l == -1 ) {
            return null;
        }
        else {
            byte[] buf = new byte[l];
            in.readFully( buf );
            return new String(buf, ASCII);
        }
    }


    protected void writeString( String str ) throws IOException
    {
        if ( str == null ) {
            out.writeInt(-1);
        }
        else {
            byte[] buf = str.getBytes( ASCII );
            out.writeInt( buf.length );
            out.write( buf );
        }
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
    
    protected void flush() throws IOException
    {
        out.flush();
    }
    
    protected void log(String msg) {
        Main.log( msg );
    }
}

/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.felix.shell.remote;


import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;


/**
 * Class implementing a <tt>TerminalPrintStream</tt>.
 */
class TerminalPrintStream extends PrintStream
{

    /**
     * Constructs a new instance wrapping the given <tt>OutputStream</tt>.
     *
     * @param tout the <tt>OutputStream</tt> to be wrapped.
     */
    public TerminalPrintStream( OutputStream tout )
    {
        super( tout );
    }//constructor


    public void print( String str )
    {
        try
        {
            byte[] bytes = str.getBytes();
            out.write( bytes, 0, bytes.length );
            flush();
        }
        catch ( IOException ex )
        {
            Activator.getServices().error( "TerminalPrintStream::print()", ex );
        }
    }//print


    public void println( String str )
    {
        print( str + "\r\n" );
    }//println


    public void flush()
    {
        try
        {
            out.flush();
        }
        catch ( IOException ex )
        {
            Activator.getServices().error( "TerminalPrintStream::println()", ex );
        }
    }//flush

}//class TerminalPrintStream

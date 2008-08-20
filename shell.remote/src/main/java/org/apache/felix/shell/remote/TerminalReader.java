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
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;


/**
 * Class implementing a terminal reader adapter
 * originally designed for the BeanShell Interpreter.
 * <p/>
 * Provides simple line editing (Backspace, Strg-U and a history).
 */
class TerminalReader extends Reader
{

    protected InputStream m_In;
    protected PrintStream m_Out;
    protected boolean m_Echo = false;
    protected boolean m_EOF = false;


    public TerminalReader( InputStream in, PrintStream out )
    {
        m_In = in;
        m_Out = out;
    }//TerminalReader


    /**
     * Tests if this <tt>TerminalReader</tt> will echo
     * the character input to the terminal.
     *
     * @return true if echo, false otherwise.
     */
    public boolean isEcho()
    {
        return m_Echo;
    }//isEcho


    /**
     * Sets if this <tt>TerminalReader</tt> will echo
     * the character input to the terminal.
     * <p/>
     * This only makes sense in character input mode,
     * in line mode the terminal will handle editing,
     * and this is recommended for using a more complex shell.
     * If you implement your own character based editor, you
     * might as well change this.<br/>
     * (Default is false).
     *
     * @param echo true if echo, false otherwise.
     */
    public void setEcho( boolean echo )
    {
        m_Echo = echo;
    }//setEcho


    public int read( char[] chars, int off, int len ) throws IOException
    {
        if ( m_EOF )
        {
            return -1;
        }
        for ( int i = off; i < off + len; i++ )
        {
            int ch = m_In.read();
            //shortcut for EOT and simple EOF
            if ( ch == EOT || ( i == off && ch == -1 ) )
            {
                return -1;
            }
            chars[i] = ( char ) ch;
            if ( ch == -1 || ch == 10 || ch == 13 )
            {
                m_EOF = ch == -1; //store EOF
                int read = i - off + 1;
                if ( m_Echo )
                {
                    m_Out.write( CRLF );
                }
                return read;
            }
            //naive backspace handling
            if ( ch == BS || ch == DEL )
            {
                if ( i > off )
                {
                    i = i - 2;
                    moveLeft( 1 );
                    eraseToEndOfLine();
                }
                else
                {
                    i--;
                    bell();
                }
            }
            else if ( ch == CTRL_U )
            {
                moveLeft( i - off );
                eraseToEndOfLine();
                i = off - 1;
            }
            else
            {
                if ( m_Echo )
                {
                    m_Out.write( chars[i] );
                }
            }
        }
        return len;
    }//read


    /**
     * Writes the NVT BEL character to the output.
     */
    private void bell()
    {
        m_Out.write( BEL );
        m_Out.flush();
    }//bell


    /**
     * Writes the standard vt100/ansi cursor moving code to the output.
     *
     * @param i the number of times the cursor should be moved left.
     * @throws IOException if I/O fails.
     */
    private void moveLeft( int i ) throws IOException
    {
        CURSOR_LEFT[2] = Byte.decode( Integer.toString( i ) ).byteValue();
        m_Out.write( CURSOR_LEFT );
        m_Out.flush();
    }//moveLeft


    /**
     * Writes the standard vt100/ansi sequence for erasing to the end of the current line.
     *
     * @throws IOException if I/O fails.
     */
    private void eraseToEndOfLine() throws IOException
    {
        m_Out.write( ERASE_TEOL );
        m_Out.flush();
    }//eraseToEndOfLine


    /**
     * Closes this reader.
     * Note: will close the input, but not the output.
     *
     * @throws IOException
     */
    public void close() throws IOException
    {
        m_In.close();
    }//close

    /**
     * <b>Bell</b><br>
     * The ANSI defined byte code for the NVT bell.
     */
    public static final byte BEL = 7;

    /**
     * <b>BackSpace</b><br>
     * The ANSI defined byte code of backspace.
     */
    public static final byte BS = 8;

    /**
     * <b>Delete</b><br>
     * The ANSI defined byte code of delete.
     */
    public static final byte DEL = 127;

    /**
     * CTRL-u
     */
    public static final byte CTRL_U = 21;

    /**
     * Escape character.
     */
    private static byte ESC = 27;

    /**
     * vt100/ansi standard sequence for moving the cursor left.
     */
    private static byte[] CURSOR_LEFT =
        { ESC, '[', '1', 'D' };

    /**
     * vt100/ansi standard sequence for erasing everything from the actual cursor to
     * the end of the current line.
     */
    private static byte[] ERASE_TEOL =
        { ESC, '[', 'K' };

    /**
     * Standard NVT line break, which HAS TO BE CR and LF.
     */
    private static byte[] CRLF =
        { '\r', '\n' };

    /**
     * Standard ASCII end of transmission.
     */
    private static byte EOT = 4;

}//class TerminalReader

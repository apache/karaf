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
package org.apache.felix.webconsole.internal.filter;


import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.VariableResolver;


/**
 * The <code>ResourceFilteringWriter</code> is a writer, which translates
 * strings of the form <code>${some key text}</code> to a translation
 * of the respective <i>some key text</i> or to the <i>some key text</i>
 * itself if no translation is available from a resource bundle.
 */
class ResourceFilteringWriter extends FilterWriter
{

    /**
     * normal processing state, $ signs are recognized here
     * proceeds to {@link #STATE_DOLLAR} if a $ sign is encountered
     * proceeds to {@link #STATE_ESCAPE} if a \ sign is encountered
     * otherwise just writes the character
     */
    private static final int STATE_NULL = 0;

    /**
     * State after a $ sign has been recognized
     * proceeds to {@value #STATE_BUFFERING} if a { sign is encountered
     * otherwise proceeds to {@link #STATE_NULL} and writes the $ sign and
     * the current character
     */
    private static final int STATE_DOLLAR = 1;

    /**
     * buffers characters until a } is encountered
     * proceeds to {@link #STATE_NULL} if a } sign is encountered and
     * translates and writes buffered text before returning
     * otherwise collects characters to gather the translation key
     */
    private static final int STATE_BUFFERING = 2;

    /**
     * escaping the next character, if the character is a $ sign, the
     * $ sign is writeted. otherwise the \ and the next character is
     * written
     * proceeds to {@link #STATE_NULL}
     */
    private static final int STATE_ESCAPE = 3;

    /**
     * The ResourceBundle used for translation
     */
    private final ResourceBundle locale;

    private final VariableResolver variables;

    /**
     * The buffer to gather the text to be translated
     */
    private final StringBuffer lineBuffer = new StringBuffer();

    /**
     * The current state, starts with {@link #STATE_NULL}
     */
    private int state = STATE_NULL;


    ResourceFilteringWriter( final Writer out, final ResourceBundle locale, final VariableResolver variables )
    {
        super( out );
        this.locale = locale;
        this.variables = ( variables != null ) ? variables : new DefaultVariableResolver();
    }


    /**
     * Write a single character following the state machine:
     * <table>
     * <tr><th>State</th><th>Character</th><th>Task</th><th>Next State</th></tr>
     * <tr><td>NULL</td><td>$</td><td>&nbsp;</td><td>DOLLAR</td></tr>
     * <tr><td>NULL</td><td>\</td><td>&nbsp;</td><td>ESCAPE</td></tr>
     * <tr><td>NULL</td><td>any</td><td>write c</td><td>NULL</td></tr>
     * <tr><td>DOLLAR</td><td>{</td><td>&nbsp;</td><td>BUFFERING</td></tr>
     * <tr><td>DOLLAR</td><td>any</td><td>write $ and c</td><td>NULL</td></tr>
     * <tr><td>BUFFERING</td><td>}</td><td>translate and write translation</td><td>NULL</td></tr>
     * <tr><td>BUFFERING</td><td>any</td><td>buffer c</td><td>BUFFERING</td></tr>
     * <tr><td>ESACPE</td><td>$</td><td>write $</td><td>NULL</td></tr>
     * <tr><td>ESCAPE</td><td>any</td><td>write \ and c</td><td>NULL</td></tr>
     * </table>
     *
     * @exception IOException If an I/O error occurs
     */
    public void write( int c ) throws IOException
    {
        switch ( state )
        {
            case STATE_NULL:
                if ( c == '$' )
                {
                    state = STATE_DOLLAR;
                }
                else if ( c == '\\' )
                {
                    state = STATE_ESCAPE;
                }
                else
                {
                    out.write( c );
                }
                break;

            case STATE_DOLLAR:
                if ( c == '{' )
                {
                    state = STATE_BUFFERING;
                }
                else
                {
                    state = STATE_NULL;
                    out.write( '$' );
                    out.write( c );
                }
                break;

            case STATE_BUFFERING:
                if ( c == '}' )
                {
                    state = STATE_NULL;
                    super.write( translate() );
                }
                else
                {
                    lineBuffer.append( ( char ) c );
                }
                break;

            case STATE_ESCAPE:
                state = STATE_NULL;
                if ( c != '$' )
                {
                    out.write( '\\' );
                }
                out.write( c );
                break;
        }
    }


    /**
     * Writes each character calling {@link #write(int)}
     *
     * @param cbuf Buffer of characters to be written
     * @param off Offset from which to start reading characters
     * @param len Number of characters to be written
     * @exception IOException If an I/O error occurs
     */
    public void write( char cbuf[], int off, int len ) throws IOException
    {
        final int limit = off + len;
        for ( int i = off; i < limit; i++ )
        {
            write( cbuf[i] );
        }
    }


    /**
     * Writes each character calling {@link #write(int)}
     *
     * @param str String to be written
     * @param off Offset from which to start reading characters
     * @param len Number of characters to be written
     * @exception IOException If an I/O error occurs
     */
    public void write( String str, int off, int len ) throws IOException
    {
        final int limit = off + len;
        for ( int i = off; i < limit; i++ )
        {
            write( str.charAt( i ) );
        }
    }


    /**
     * Translates the current buffer contents and returns the translation.
     * First the name is looked up in the variables, if not found the
     * resource bundle is queried. If still not found, the buffer contents
     * is returned unmodified.
     */
    private String translate()
    {
        final String key = lineBuffer.toString();
        lineBuffer.delete( 0, lineBuffer.length() );

        String value = variables.resolve( key );
        if ( value == null )
        {
            try
            {
                value = locale.getString( key );
            }
            catch ( MissingResourceException mre )
            {
                // ignore and write the key as the value
                value = key;
            }
        }

        return value;
    }
}
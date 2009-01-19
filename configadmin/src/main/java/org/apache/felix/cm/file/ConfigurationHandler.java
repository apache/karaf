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
package org.apache.felix.cm.file;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * The <code>ConfigurationHandler</code> class implements configuration reading
 * form a <code>java.io.InputStream</code> and writing to a
 * <code>java.io.OutputStream</code> on behalf of the
 * {@link FilePersistenceManager} class.
 * 
 * <pre>
 * cfg = prop &quot;=&quot; value .
 *  prop = symbolic-name . // 1.4.2 of OSGi Core Specification
 *  symbolic-name = token { &quot;.&quot; token } .
 *  token = { [ 0..9 ] | [ a..z ] | [ A..Z ] | '_' | '-' } .
 *  value = [ type ] ( &quot;[&quot; values &quot;]&quot; | &quot;(&quot; values &quot;)&quot; | simple ) .
 *  values = simple { &quot;,&quot; simple } .
 *  simple = &quot;&quot;&quot; stringsimple &quot;&quot;&quot; .
 *  type = // 1-char type code .
 *  stringsimple = // quoted string representation of the value .
 * </pre>
 */
public class ConfigurationHandler
{
    protected static final String ENCODING = "UTF-8";

    protected static final int TOKEN_NAME = 'N';
    protected static final int TOKEN_EQ = '=';
    protected static final int TOKEN_ARR_OPEN = '[';
    protected static final int TOKEN_ARR_CLOS = ']';
    protected static final int TOKEN_VEC_OPEN = '(';
    protected static final int TOKEN_VEC_CLOS = ')';
    protected static final int TOKEN_COMMA = ',';
    protected static final int TOKEN_VAL_OPEN = '"'; // '{';
    protected static final int TOKEN_VAL_CLOS = '"'; // '}';

    // simple types (string & primitive wrappers)
    protected static final int TOKEN_SIMPLE_STRING = 'T';
    protected static final int TOKEN_SIMPLE_INTEGER = 'I';
    protected static final int TOKEN_SIMPLE_LONG = 'L';
    protected static final int TOKEN_SIMPLE_FLOAT = 'F';
    protected static final int TOKEN_SIMPLE_DOUBLE = 'D';
    protected static final int TOKEN_SIMPLE_BYTE = 'X';
    protected static final int TOKEN_SIMPLE_SHORT = 'S';
    protected static final int TOKEN_SIMPLE_CHARACTER = 'C';
    protected static final int TOKEN_SIMPLE_BOOLEAN = 'B';

    // primitives
    protected static final int TOKEN_PRIMITIVE_INT = 'i';
    protected static final int TOKEN_PRIMITIVE_LONG = 'l';
    protected static final int TOKEN_PRIMITIVE_FLOAT = 'f';
    protected static final int TOKEN_PRIMITIVE_DOUBLE = 'd';
    protected static final int TOKEN_PRIMITIVE_BYTE = 'x';
    protected static final int TOKEN_PRIMITIVE_SHORT = 's';
    protected static final int TOKEN_PRIMITIVE_CHAR = 'c';
    protected static final int TOKEN_PRIMITIVE_BOOLEAN = 'b';

    protected static final String CRLF = "\r\n";

    protected static final Map type2Code;
    protected static final Map code2Type;

    // set of valid characters for "symblic-name"
    private static final BitSet NAME_CHARS;
    private static final BitSet TOKEN_CHARS;

    static
    {
        type2Code = new HashMap();

        // simple (exclusive String whose type code is not written)
        type2Code.put( Integer.class, new Integer( TOKEN_SIMPLE_INTEGER ) );
        type2Code.put( Long.class, new Integer( TOKEN_SIMPLE_LONG ) );
        type2Code.put( Float.class, new Integer( TOKEN_SIMPLE_FLOAT ) );
        type2Code.put( Double.class, new Integer( TOKEN_SIMPLE_DOUBLE ) );
        type2Code.put( Byte.class, new Integer( TOKEN_SIMPLE_BYTE ) );
        type2Code.put( Short.class, new Integer( TOKEN_SIMPLE_SHORT ) );
        type2Code.put( Character.class, new Integer( TOKEN_SIMPLE_CHARACTER ) );
        type2Code.put( Boolean.class, new Integer( TOKEN_SIMPLE_BOOLEAN ) );

        // primitives
        type2Code.put( Integer.TYPE, new Integer( TOKEN_PRIMITIVE_INT ) );
        type2Code.put( Long.TYPE, new Integer( TOKEN_PRIMITIVE_LONG ) );
        type2Code.put( Float.TYPE, new Integer( TOKEN_PRIMITIVE_FLOAT ) );
        type2Code.put( Double.TYPE, new Integer( TOKEN_PRIMITIVE_DOUBLE ) );
        type2Code.put( Byte.TYPE, new Integer( TOKEN_PRIMITIVE_BYTE ) );
        type2Code.put( Short.TYPE, new Integer( TOKEN_PRIMITIVE_SHORT ) );
        type2Code.put( Character.TYPE, new Integer( TOKEN_PRIMITIVE_CHAR ) );
        type2Code.put( Boolean.TYPE, new Integer( TOKEN_PRIMITIVE_BOOLEAN ) );

        // reverse map to map type codes to classes, string class mapping
        // to be added manually, as the string type code is not written and
        // hence not included in the type2Code map
        code2Type = new HashMap();
        for ( Iterator ti = type2Code.entrySet().iterator(); ti.hasNext(); )
        {
            Map.Entry entry = ( Map.Entry ) ti.next();
            code2Type.put( entry.getValue(), entry.getKey() );
        }
        code2Type.put( new Integer( TOKEN_SIMPLE_STRING ), String.class );

        NAME_CHARS = new BitSet();
        for ( int i = '0'; i <= '9'; i++ )
            NAME_CHARS.set( i );
        for ( int i = 'a'; i <= 'z'; i++ )
            NAME_CHARS.set( i );
        for ( int i = 'A'; i <= 'Z'; i++ )
            NAME_CHARS.set( i );
        NAME_CHARS.set( '_' );
        NAME_CHARS.set( '-' );
        NAME_CHARS.set( '.' );

        TOKEN_CHARS = new BitSet();
        TOKEN_CHARS.set( TOKEN_EQ );
        TOKEN_CHARS.set( TOKEN_ARR_OPEN );
        TOKEN_CHARS.set( TOKEN_ARR_CLOS );
        TOKEN_CHARS.set( TOKEN_VEC_OPEN );
        TOKEN_CHARS.set( TOKEN_VEC_CLOS );
        TOKEN_CHARS.set( TOKEN_COMMA );
        TOKEN_CHARS.set( TOKEN_VAL_OPEN );
        TOKEN_CHARS.set( TOKEN_VAL_CLOS );
        TOKEN_CHARS.set( TOKEN_SIMPLE_STRING );
        TOKEN_CHARS.set( TOKEN_SIMPLE_INTEGER );
        TOKEN_CHARS.set( TOKEN_SIMPLE_LONG );
        TOKEN_CHARS.set( TOKEN_SIMPLE_FLOAT );
        TOKEN_CHARS.set( TOKEN_SIMPLE_DOUBLE );
        TOKEN_CHARS.set( TOKEN_SIMPLE_BYTE );
        TOKEN_CHARS.set( TOKEN_SIMPLE_SHORT );
        TOKEN_CHARS.set( TOKEN_SIMPLE_CHARACTER );
        TOKEN_CHARS.set( TOKEN_SIMPLE_BOOLEAN );

        // primitives
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_INT );
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_LONG );
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_FLOAT );
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_DOUBLE );
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_BYTE );
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_SHORT );
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_CHAR );
        TOKEN_CHARS.set( TOKEN_PRIMITIVE_BOOLEAN );
    }


    /**
     * Writes the configuration data from the <code>Dictionary</code> to the
     * given <code>OutputStream</code>.
     * <p>
     * This method writes at the current location in the stream and does not
     * close the outputstream.
     * 
     * @param out
     *            The <code>OutputStream</code> to write the configurtion data
     *            to.
     * @param properties
     *            The <code>Dictionary</code> to write.
     * @throws IOException
     *             If an error occurrs writing to the output stream.
     */
    public static void write( OutputStream out, Dictionary properties ) throws IOException
    {
        BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( out, ENCODING ) );

        for ( Enumeration ce = properties.keys(); ce.hasMoreElements(); )
        {
            String key = ( String ) ce.nextElement();

            // cfg = prop "=" value "." .
            writeQuoted( bw, key );
            bw.write( TOKEN_EQ );
            writeValue( bw, properties.get( key ) );
            bw.write( CRLF );
        }

        bw.flush();
    }


    /**
     * Reads configuration data from the given <code>InputStream</code> and
     * returns a new <code>Dictionary</code> object containing the data.
     * <p>
     * This method reads from the current location in the stream upto the end of
     * the stream but does not close the stream at the end.
     * 
     * @param ins
     *            The <code>InputStream</code> from which to read the
     *            configuration data.
     * @return A <code>Dictionary</code> object containing the configuration
     *         data. This object may be empty if the stream contains no
     *         configuration data.
     * @throws IOException
     *             If an error occurrs reading from the stream. This exception
     *             is also thrown if a syntax error is encountered.
     */
    public static Dictionary read( InputStream ins ) throws IOException
    {
        return new ConfigurationHandler().readInternal( ins );
    }


    // private constructor, this class is not to be instantiated from the
    // outside
    private ConfigurationHandler()
    {
    }

    // ---------- Configuration Input Implementation ---------------------------

    private int token;
    private String tokenValue;
    private int line;
    private int pos;


    private Dictionary readInternal( InputStream ins ) throws IOException
    {
        BufferedReader br = new BufferedReader( new InputStreamReader( ins, ENCODING ) );
        PushbackReader pr = new PushbackReader( br, 1 );

        token = 0;
        tokenValue = null;
        line = 0;
        pos = 0;

        Hashtable configuration = new Hashtable();
        token = 0;
        while ( nextToken( pr ) == TOKEN_NAME )
        {
            String key = tokenValue;

            // expect equal sign
            if ( nextToken( pr ) != TOKEN_EQ )
            {
                throw readFailure( token, TOKEN_EQ );
            }

            // expect the token value
            Object value = readValue( pr );
            if ( value != null )
            {
                configuration.put( key, value );
            }
        }

        return configuration;
    }


    /**
     * value = type ( "[" values "]" | "(" values ")" | simple ) . values =
     * value { "," value } . simple = "{" stringsimple "}" . type = // 1-char
     * type code . stringsimple = // quoted string representation of the value .
     * 
     * @param pr
     * @return
     * @throws IOException
     */
    private Object readValue( PushbackReader pr ) throws IOException
    {
        // read (optional) type code
        int type = read( pr );

        // read value kind code if type code is not a value kinde code
        int code;
        if ( code2Type.containsKey( new Integer( type ) ) )
        {
            code = read( pr );
        }
        else
        {
            code = type;
            type = TOKEN_SIMPLE_STRING;
        }

        switch ( code )
        {
            case TOKEN_ARR_OPEN:
                return readArray( type, pr );

            case TOKEN_VEC_OPEN:
                return readCollection( type, pr );

            case TOKEN_VAL_OPEN:
                return readSimple( type, pr );

            default:
                return null;
        }
    }


    private Object readArray( int typeCode, PushbackReader pr ) throws IOException
    {
        List list = new ArrayList();
        for ( ;; )
        {
            if ( !checkNext( pr, TOKEN_VAL_OPEN ) )
            {
                return null;
            }

            Object value = readSimple( typeCode, pr );
            if ( value == null )
            {
                // abort due to error
                return null;
            }

            list.add( value );

            int c = read( pr );
            if ( c == TOKEN_ARR_CLOS )
            {
                Class type = ( Class ) code2Type.get( new Integer( typeCode ) );
                Object array = Array.newInstance( type, list.size() );
                for ( int i = 0; i < list.size(); i++ )
                {
                    Array.set( array, i, list.get( i ) );
                }
                return array;
            }
            else if ( c < 0 )
            {
                return null;
            }
            else if ( c != TOKEN_COMMA )
            {
                return null;
            }
        }
    }


    private Collection readCollection( int typeCode, PushbackReader pr ) throws IOException
    {
        Collection collection = new ArrayList();
        for ( ;; )
        {
            if ( !checkNext( pr, TOKEN_VAL_OPEN ) )
            {
                return null;
            }

            Object value = readSimple( typeCode, pr );
            if ( value == null )
            {
                // abort due to error
                return null;
            }

            collection.add( value );

            int c = read( pr );
            if ( c == TOKEN_VEC_CLOS )
            {
                return collection;
            }
            else if ( c < 0 )
            {
                return null;
            }
            else if ( c != TOKEN_COMMA )
            {
                return null;
            }
        }
    }


    private Object readSimple( int code, PushbackReader pr ) throws IOException
    {
        switch ( code )
        {
            case -1:
                return null;

            case TOKEN_SIMPLE_STRING:
                return readQuoted( pr );

                // Simple/Primitive, only use wrapper classes
            case TOKEN_SIMPLE_INTEGER:
            case TOKEN_PRIMITIVE_INT:
                return Integer.valueOf( readQuoted( pr ) );

            case TOKEN_SIMPLE_LONG:
            case TOKEN_PRIMITIVE_LONG:
                return Long.valueOf( readQuoted( pr ) );

            case TOKEN_SIMPLE_FLOAT:
            case TOKEN_PRIMITIVE_FLOAT:
                int fBits = Integer.parseInt( readQuoted( pr ) );
                return new Float( Float.intBitsToFloat( fBits ) );

            case TOKEN_SIMPLE_DOUBLE:
            case TOKEN_PRIMITIVE_DOUBLE:
                long dBits = Long.parseLong( readQuoted( pr ) );
                return new Double( Double.longBitsToDouble( dBits ) );

            case TOKEN_SIMPLE_BYTE:
            case TOKEN_PRIMITIVE_BYTE:
                return Byte.valueOf( readQuoted( pr ) );

            case TOKEN_SIMPLE_SHORT:
            case TOKEN_PRIMITIVE_SHORT:
                return Short.valueOf( readQuoted( pr ) );

            case TOKEN_SIMPLE_CHARACTER:
            case TOKEN_PRIMITIVE_CHAR:
                String cString = readQuoted( pr );
                if ( cString != null && cString.length() > 0 )
                {
                    return new Character( cString.charAt( 0 ) );
                }
                return null;

            case TOKEN_SIMPLE_BOOLEAN:
            case TOKEN_PRIMITIVE_BOOLEAN:
                return Boolean.valueOf( readQuoted( pr ) );

                // unknown type code
            default:
                return null;
        }
    }


    private boolean checkNext( PushbackReader pr, int expected ) throws IOException
    {
        int next = read( pr );
        if ( next < 0 )
        {
            return false;
        }

        if ( next == expected )
        {
            return true;
        }

        return false;
    }


    private String readQuoted( PushbackReader pr ) throws IOException
    {
        StringBuffer buf = new StringBuffer();
        for ( ;; )
        {
            int c = read( pr );
            switch ( c )
            {
                // escaped character
                case '\\':
                    c = read( pr );
                    switch ( c )
                    {
                        // well known escapes
                        case 'b':
                            buf.append( '\b' );
                            break;
                        case 't':
                            buf.append( '\t' );
                            break;
                        case 'n':
                            buf.append( '\n' );
                            break;
                        case 'f':
                            buf.append( '\f' );
                            break;
                        case 'r':
                            buf.append( '\r' );
                            break;
                        case 'u':// need 4 characters !
                            char[] cbuf = new char[4];
                            if ( read( pr, cbuf ) == 4 )
                            {
                                c = Integer.parseInt( new String( cbuf ), 16 );
                                buf.append( ( char ) c );
                            }
                            break;

                        // just an escaped character, unescape
                        default:
                            buf.append( ( char ) c );
                    }
                    break;

                // eof
                case -1: // fall through

                    // separator token
                case TOKEN_VAL_CLOS:
                    return buf.toString();

                    // no escaping
                default:
                    buf.append( ( char ) c );
            }
        }
    }


    private int nextToken( PushbackReader pr ) throws IOException
    {
        int c = ignorableWhiteSpace( pr );

        // immediately return EOF
        if ( c < 0 )
        {
            return ( token = c );
        }

        // check whether there is a name
        if ( NAME_CHARS.get( c ) )
        {
            // read the property name
            tokenValue = readName( pr, ( char ) c );
            return ( token = TOKEN_NAME );
        }

        // check another token
        if ( TOKEN_CHARS.get( c ) )
        {
            return ( token = c );
        }

        // unexpected character -> so what ??
        return ( token = -1 );
    }


    private int ignorableWhiteSpace( PushbackReader pr ) throws IOException
    {
        int c = read( pr );
        while ( c >= 0 && Character.isWhitespace( ( char ) c ) )
        {
            c = read( pr );
        }
        return c;
    }


    private String readName( PushbackReader pr, char firstChar ) throws IOException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( firstChar );

        int c = read( pr );
        while ( c >= 0 && NAME_CHARS.get( c ) )
        {
            buf.append( ( char ) c );
            c = read( pr );
        }
        pr.unread( c );

        if ( buf.charAt( 0 ) == '.' || buf.charAt( buf.length() - 1 ) == '.' )
        {
            throw new IOException( "Name (" + buf + ") must not start or end with a dot" );
        }

        return buf.toString();
    }


    private int read( PushbackReader pr ) throws IOException
    {
        int c = pr.read();
        if ( c == '\r' )
        {
            int c1 = pr.read();
            if ( c1 != '\n' )
            {
                pr.unread( c1 );
            }
            c = '\n';
        }

        if ( c == '\n' )
        {
            line++;
            pos = 0;
        }
        else
        {
            pos++;
        }

        return c;
    }


    private int read( PushbackReader pr, char[] buf ) throws IOException
    {
        for ( int i = 0; i < buf.length; i++ )
        {
            int c = read( pr );
            if ( c >= 0 )
            {
                buf[i] = ( char ) c;
            }
            else
            {
                return i;
            }
        }

        return buf.length;
    }


    private IOException readFailure( int current, int expected )
    {
        return new IOException( "Unexpected token " + current + "; expected: " + expected + " (line=" + line + ", pos="
            + pos + ")" );
    }


    // ---------- Configuration Output Implementation --------------------------

    private static void writeValue( Writer out, Object value ) throws IOException
    {
        Class clazz = value.getClass();
        if ( clazz.isArray() )
        {
            writeArray( out, value );
        }
        else if ( value instanceof Collection )
        {
            writeCollection( out, ( Collection ) value );
        }
        else
        {
            writeType( out, clazz );
            writeSimple( out, value );
        }
    }


    private static void writeArray( Writer out, Object arrayValue ) throws IOException
    {
        int size = Array.getLength( arrayValue );
        if ( size == 0 )
        {
            return;
        }

        writeType( out, arrayValue.getClass().getComponentType() );
        out.write( TOKEN_ARR_OPEN );
        for ( int i = 0; i < size; i++ )
        {
            if ( i > 0 )
                out.write( TOKEN_COMMA );
            writeSimple( out, Array.get( arrayValue, i ) );
        }
        out.write( TOKEN_ARR_CLOS );
    }


    private static void writeCollection( Writer out, Collection collection ) throws IOException
    {
        if ( collection.isEmpty() )
        {
            return;
        }

        Iterator ci = collection.iterator();
        Object firstElement = ci.next();

        writeType( out, firstElement.getClass() );
        out.write( TOKEN_VEC_OPEN );
        writeSimple( out, firstElement );

        while ( ci.hasNext() )
        {
            out.write( TOKEN_COMMA );
            writeSimple( out, ci.next() );
        }
        out.write( TOKEN_VEC_CLOS );
    }


    private static void writeType( Writer out, Class valueType ) throws IOException
    {
        Integer code = ( Integer ) type2Code.get( valueType );
        if ( code != null )
        {
            out.write( ( char ) code.intValue() );
        }
    }


    private static void writeSimple( Writer out, Object value ) throws IOException
    {
        if ( value instanceof Double )
        {
            double dVal = ( ( Double ) value ).doubleValue();
            value = new Long( Double.doubleToRawLongBits( dVal ) );
        }
        else if ( value instanceof Float )
        {
            float fVal = ( ( Float ) value ).floatValue();
            value = new Integer( Float.floatToRawIntBits( fVal ) );
        }

        out.write( TOKEN_VAL_OPEN );
        writeQuoted( out, String.valueOf( value ) );
        out.write( TOKEN_VAL_CLOS );
    }


    private static void writeQuoted( Writer out, String simple ) throws IOException
    {
        if ( simple == null || simple.length() == 0 )
        {
            return;
        }

        char c = 0;
        int len = simple.length();
        for ( int i = 0; i < len; i++ )
        {
            c = simple.charAt( i );
            switch ( c )
            {
                case '\\':
                case TOKEN_VAL_CLOS:
                    out.write( '\\' );
                    out.write( c );
                    break;

                // well known escapes
                case '\b':
                    out.write( "\\b" );
                    break;
                case '\t':
                    out.write( "\\t" );
                    break;
                case '\n':
                    out.write( "\\n" );
                    break;
                case '\f':
                    out.write( "\\f" );
                    break;
                case '\r':
                    out.write( "\\r" );
                    break;

                // other escaping
                default:
                    if ( c < ' ' )
                    {
                        String t = "000" + Integer.toHexString( c );
                        out.write( "\\u" + t.substring( t.length() - 4 ) );
                    }
                    else
                    {
                        out.write( c );
                    }
            }
        }
    }
}

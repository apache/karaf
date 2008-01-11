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
package org.apache.felix.metatype;


import java.util.*;

import org.apache.felix.metatype.internal.Activator;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;


/**
 * The <code>AD</code> class represents the <code>AD</code> element of the
 * meta type descriptor.
 *
 * @author fmeschbe
 */
public class AD
{

    /**
     * The message returned from the {@link #validate(String)} method if the
     * value is not any of the specified {@link #getOptionValues() option values}
     * (value is "%not a valid option").
     */
    public static final String VALIDATE_NOT_A_VALID_OPTION = "%not a valid option";

    /**
     * The message returned from the {@link #validate(String)} method if the
     * value is greater than the specified {@link #getMax() maximum value}
     * (value is "%greater than maximum").
     */
    public static final String VALIDATE_GREATER_THAN_MAXIMUM = "%greater than maximum";

    /**
     * The message returned from the {@link #validate(String)} method if the
     * value is less than the specified {@link #getMin() minimum value}
     * (value is "%less than minimum").
     */
    public static final String VALIDATE_LESS_THAN_MINIMUM = "%less than minimum";

    private String id;
    private String name;
    private String description;
    private int type;
    private int cardinality = 0;
    private String[] optionLabels;
    private String[] optionValues;
    private String[] defaultValue;
    private String min;
    private String max;
    private boolean isRequired = true;


    public String getID()
    {
        return id;
    }


    public String getName()
    {
        return name;
    }


    public String getDescription()
    {
        return description;
    }


    public int getType()
    {
        return type;
    }


    public int getCardinality()
    {
        return cardinality;
    }


    public String[] getOptionLabels()
    {
        return optionLabels;
    }


    public String[] getOptionValues()
    {
        return optionValues;
    }


    public String[] getDefaultValue()
    {
        return defaultValue;
    }


    public String getMin()
    {
        return min;
    }


    public String getMax()
    {
        return max;
    }


    public boolean isRequired()
    {
        return isRequired;
    }


    /**
     * Implements validation of the <code>valueString</code> and returns an
     * indication of the success:
     * <dl>
     * <dt><code>null</code>
     * <dd>If neither a {@link #getMin() minimal value} nor a
     *      {@link #getMax() maximal value} nor any
     *      {@link #getOptionValues() optional values} are defined in this
     *      instance, validation cannot be performed.
     * <dt>Empty String
     * <dd>If validation succeeds. This value is also returned if the
     *      <code>valueString</code> is empty or <code>null</code> or cannot be
     *      converted into a numeric type.
     * <dt><b>%</b>message
     * <dd>If the value falls below the minimum, higher than the maximum or is
     *      not any of the option values, an explanatory message, which may be
     *      localized is returned. If any of the minimum, maximum or option
     *      values is <code>null</code>, the respective value is not checked.
     * </dl>
     *
     * @param valueString The string representation of the value to validate.
     *
     * @return As explained above.
     *
     * @see #VALIDATE_GREATER_THAN_MAXIMUM
     * @see #VALIDATE_LESS_THAN_MINIMUM
     * @see #VALIDATE_NOT_A_VALID_OPTION
     */
    public String validate( String valueString )
    {
        // no validation if no min and max
        if ( getMin() == null && getMax() == null && getOptionValues() == null )
        {
            return null;
        }

        Comparable value = convertToType( valueString );
        if ( value == null )
        {
            return ""; // accept null value
        }

        Comparable other = convertToType( getMin() );
        if ( other != null )
        {
            if ( value.compareTo( other ) < 0 )
            {
                return VALIDATE_LESS_THAN_MINIMUM;
            }
        }

        other = convertToType( getMax() );
        if ( other != null )
        {
            if ( value.compareTo( other ) > 0 )
            {
                return VALIDATE_GREATER_THAN_MAXIMUM;
            }
        }

        String[] optionValues = getOptionValues();
        if ( optionValues != null )
        {
            for ( int i = 0; i < optionValues.length; i++ )
            {
                other = convertToType( optionValues[i] );
                if ( value.compareTo( other ) == 0 )
                {
                    // one of the option values
                    return "";
                }
            }

            // not any of the option values, fail
            return VALIDATE_NOT_A_VALID_OPTION;
        }

        // finally, we accept the value
        return "";
    }


    //--------- Setters for setting up this instance --------------------------

    /**
     * @param id the id to set
     */
    public void setID( String id )
    {
        this.id = id;
    }


    /**
     * @param name the name to set
     */
    public void setName( String name )
    {
        this.name = name;
    }


    /**
     * @param description the description to set
     */
    public void setDescription( String description )
    {
        this.description = description;
    }


    /**
     * @param typeString the type to set
     */
    public void setType( String typeString )
    {
        this.type = toType( typeString );
    }


    /**
     * @param cardinality the cardinality to set
     */
    public void setCardinality( int cardinality )
    {
        this.cardinality = cardinality;
    }


    /**
     * @param options the options to set
     */
    public void setOptions( Map options )
    {
        optionLabels = new String[options.size()];
        optionValues = new String[options.size()];
        int i = 0;
        for ( Iterator oi = options.entrySet().iterator(); oi.hasNext(); i++ )
        {
            Map.Entry entry = ( Map.Entry ) oi.next();
            optionValues[i] = String.valueOf( entry.getKey() );
            optionLabels[i] = String.valueOf( entry.getValue() );
        }
    }


    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue( String defaultValue )
    {
        this.defaultValue = splitList( defaultValue );
    }


    /**
     * @param min the min to set
     */
    public void setMin( String min )
    {
        this.min = min;
    }


    /**
     * @param max the max to set
     */
    public void setMax( String max )
    {
        this.max = max;
    }


    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue( String[] defaultValue )
    {
        this.defaultValue = ( String[] ) defaultValue.clone();
    }


    /**
     * @param isRequired the isRequired to set
     */
    public void setRequired( boolean isRequired )
    {
        this.isRequired = isRequired;
    }


    public static int toType( String typeString )
    {
        if ( "String".equals( typeString ) )
        {
            return AttributeDefinition.STRING;
        }
        else if ( "Long".equals( typeString ) )
        {
            return AttributeDefinition.LONG;
        }
        else if ( "Double".equals( typeString ) )
        {
            return AttributeDefinition.DOUBLE;
        }
        else if ( "Float".equals( typeString ) )
        {
            return AttributeDefinition.FLOAT;
        }
        else if ( "Integer".equals( typeString ) )
        {
            return AttributeDefinition.INTEGER;
        }
        else if ( "Byte".equals( typeString ) )
        {
            return AttributeDefinition.BYTE;
        }
        else if ( "Char".equals( typeString ) )
        {
            return AttributeDefinition.CHARACTER;
        }
        else if ( "Boolean".equals( typeString ) )
        {
            return AttributeDefinition.BOOLEAN;
        }
        else if ( "Short".equals( typeString ) )
        {
            return AttributeDefinition.SHORT;
        }

        // finally fall back to string for illegal values
        return AttributeDefinition.STRING;
    }


    public static String[] splitList( String listString )
    {
        // return nothing ...
        if ( listString == null )
        {
            return null;
        }

        List values = new ArrayList();
        boolean escape = false;
        StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < listString.length(); i++ )
        {
            char c = listString.charAt( i );

            if ( escape )
            {
                // just go ahead
                escape = false;
            }
            else if ( c == ',' )
            {
                String value = buf.toString().trim();
                if ( value.length() > 0 )
                {
                    values.add( value );
                }
                buf.delete( 0, buf.length() );
                continue;
            }
            else if ( c == '\\' )
            {
                escape = true;
                continue;
            }

            buf.append( c );
        }

        // add last string
        if ( buf.length() > 0 )
        {
            String value = buf.toString().trim();
            if ( value.length() > 0 )
            {
                values.add( value );
            }
        }

        return values.isEmpty() ? null : ( String[] ) values.toArray( new String[values.size()] );
    }


    protected Comparable convertToType( final String value )
    {
        if ( value != null && value.length() > 0 )
        {
            try
            {
                switch ( getType() )
                {
                    case AttributeDefinition.BOOLEAN:
                        // Boolean is only Comparable starting with Java 5
                        return new ComparableBoolean(value);
                    case AttributeDefinition.CHARACTER:
                        return new Character( value.charAt( 0 ) );
                    case AttributeDefinition.BYTE:
                        return Byte.valueOf( value );
                    case AttributeDefinition.SHORT:
                        return Short.valueOf( value );
                    case AttributeDefinition.INTEGER:
                        return Integer.valueOf( value );
                    case AttributeDefinition.LONG:
                        return Long.valueOf( value );
                    case AttributeDefinition.FLOAT:
                        return Float.valueOf( value );
                    case AttributeDefinition.DOUBLE:
                        return Double.valueOf( value );
                    case AttributeDefinition.STRING:
                    default:
                        return value;
                }
            }
            catch ( NumberFormatException nfe )
            {
                Activator.log( LogService.LOG_INFO, "Cannot convert value '" + value + "'", nfe );
            }
        }

        return null;
    }

    private static class ComparableBoolean implements Comparable {
        private boolean value;

        ComparableBoolean(String boolValue) {
            value = Boolean.valueOf(boolValue).booleanValue();
        }

        public int compareTo(Object obj) {
            ComparableBoolean cb = (ComparableBoolean) obj;
            return (cb.value == value ? 0 : (value ? 1 : -1));
        }
    }
}

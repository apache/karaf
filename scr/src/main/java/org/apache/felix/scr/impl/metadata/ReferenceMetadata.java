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
package org.apache.felix.scr.impl.metadata;

import java.util.Set;
import java.util.TreeSet;

/**
 * Information associated to a dependency
 *
 */
public class ReferenceMetadata
{
    // constant for option single reference - 0..1
    public static final String CARDINALITY_0_1 = "0..1";

    // constant for option multiple reference - 0..n
    public static final String CARDINALITY_0_N = "0..n";

    // constant for required single reference - 1..1
    public static final String CARDINALITY_1_1 = "1..1";

    // constant for required multiple reference - 1..n
    public static final String CARDINALITY_1_N = "1..n";

    // set of valid cardinality settings
    private static final Set CARDINALITY_VALID;

    // constant for static policy
    public static final String POLICY_STATIC = "static";

    // constant for dynamic policy
    public static final String POLICY_DYNAMIC = "dynamic";

    // set of valid policy settings
    private static final Set POLICY_VALID;

    // Name for the reference (required)
    private String m_name = null;

    // Interface name (required)
    private String m_interface = null;

    // Cardinality (optional, default="1..1")
    private String m_cardinality = null;

    // Target (optional)
    private String m_target;

    // Name of the bind method (optional)
    private String m_bind = null;

    // Name of the unbind method (optional)
    private String m_unbind = null;

    // Policy attribute (optional, default = static)
    private String m_policy = null;

    // Flag that is set once the component is verified (its properties cannot be changed)
    private boolean m_validated = false;

    // Flags that store the values passed as strings
    private boolean m_isStatic = true;
    private boolean m_isOptional = false;
    private boolean m_isMultiple = false;

    static
    {
        CARDINALITY_VALID = new TreeSet();
        CARDINALITY_VALID.add( CARDINALITY_0_1 );
        CARDINALITY_VALID.add( CARDINALITY_0_N );
        CARDINALITY_VALID.add( CARDINALITY_1_1 );
        CARDINALITY_VALID.add( CARDINALITY_1_N );

        POLICY_VALID = new TreeSet();
        POLICY_VALID.add( POLICY_DYNAMIC );
        POLICY_VALID.add( POLICY_STATIC );
    }


    /////////////////////////////////////////////// setters ///////////////////////////////////

    /**
     * Setter for the name attribute
     *
     * @param name
     */
    public void setName( String name )
    {
        if ( m_validated )
        {
            return;
        }

        m_name = name;
    }


    /**
     * Setter for the interfaceName attribute
     *
     * @param interfaceName
     */
    public void setInterface( String interfaceName )
    {
        if ( m_validated )
        {
            return;
        }

        m_interface = interfaceName;

    }


    /**
     * Setter for the cardinality attribute
     *
     * @param cardinality
     */
    public void setCardinality( String cardinality )
    {
        if ( m_validated )
        {
            return;
        }

        m_cardinality = cardinality;

        // secondary properties
        m_isOptional = CARDINALITY_0_1.equals( cardinality ) || CARDINALITY_0_N.equals( cardinality );
        m_isMultiple = CARDINALITY_0_N.equals( cardinality ) || CARDINALITY_1_N.equals( cardinality );
    }


    /**
     *	Setter for the policy attribute
     *
     * @param policy
     */
    public void setPolicy( String policy )
    {
        if ( m_validated )
        {
            return;
        }

        m_policy = policy;

        // secondary property
        m_isStatic = POLICY_STATIC.equals( policy );
    }


    /**
     * Setter for the target attribute (filter)
     *
     * @param target
     */
    public void setTarget( String target )
    {
        if ( m_validated )
        {
            return;
        }

        m_target = ( target == null || target.length() == 0 ) ? null : target;
    }


    /**
     * Setter for the bind method attribute
     *
     * @param bind
     */
    public void setBind( String bind )
    {
        if ( m_validated )
        {
            return;
        }

        m_bind = bind;
    }


    /**
     * Setter for the unbind method attribute
     *
     * @param unbind
     */
    public void setUnbind( String unbind )
    {
        if ( m_validated )
        {
            return;
        }

        m_unbind = unbind;
    }


    /////////////////////////////////////////////// getters ///////////////////////////////////

    /**
     * Returns the name of the reference
     *
     * @return A string containing the reference's name
    **/
    public String getName()
    {
        return m_name;
    }


    /**
     * Returns the fully qualified name of the class that is used by the component to access the service
     *
     * @return A string containing a fully qualified name
    **/
    public String getInterface()
    {
        return m_interface;
    }


    /**
     * Get the cardinality as a string
     *
     * @return A string with the cardinality
    **/
    public String getCardinality()
    {
        return m_cardinality;
    }


    /**
     * Get the policy as a string
     *
     * @return A string with the policy
    **/
    public String getPolicy()
    {
        return m_policy;
    }


    /**
     * Returns the filter expression that further constrains the set of target services
     *
     * @return A string with a filter
    **/
    public String getTarget()
    {
        return m_target;
    }


    /**
     * Get the name of a method in the component implementation class that is used to notify that
     * a service is bound to the component configuration
     *
     * @return a String with the name of the bind method
    **/
    public String getBind()
    {
        return m_bind;
    }


    /**
     * Get the name of a method in the component implementation class that is used to notify that
     * a service is unbound from the component configuration
     *
     * @return a String with the name of the unbind method
    **/
    public String getUnbind()
    {
        return m_unbind;
    }


    // Getters for boolean values that determine both policy and cardinality

    /**
     * Test if dependency's binding policy is static
     *
     * @return true if static
    **/
    public boolean isStatic()
    {
        return m_isStatic;
    }


    /**
     * Test if dependency is optional (0..1 or 0..n)
     *
     * @return true if the dependency is optional
    **/
    public boolean isOptional()
    {
        return m_isOptional;
    }


    /**
     * Test if dependency is multiple (0..n or 1..n)
     *
     * @return true if the dependency is multiple
    **/
    public boolean isMultiple()
    {
        return m_isMultiple;
    }


    /**
     * Returns the name of the component property referring to the {@link #getTarget() target}
     * property of this reference.
     *
     * @return the name of the target property which is the name of this referene
     *      suffixed with the string ".target".
     */
    public String getTargetPropertyName()
    {
        return getName() + ".target";
    }


    /**
     *  Method used to verify if the semantics of this metadata are correct
     *
     */
    void validate( ComponentMetadata componentMetadata )
    {
        if ( m_name == null )
        {
            // 112.10 name attribute is optional, defaults to interface since DS 1.1
            if ( !componentMetadata.isDS11() )
            {
                throw componentMetadata.validationFailure( "A name must be declared for the reference" );
            }
            setName( getInterface() );
        }

        if ( m_interface == null )
        {
            throw componentMetadata.validationFailure( "An interface must be declared for the reference" );
        }


        if ( m_cardinality == null )
        {
            setCardinality( CARDINALITY_1_1 );
        }
        else if ( !CARDINALITY_VALID.contains( m_cardinality ) )
        {
            throw componentMetadata.validationFailure( "Cardinality must be one of " + CARDINALITY_VALID );
        }

        if ( m_policy == null )
        {
            setPolicy( POLICY_STATIC );
        }
        else if ( !POLICY_VALID.contains( m_policy ) )
        {
            throw componentMetadata.validationFailure( "Policy must be one of " + POLICY_VALID );
        }
    }

}
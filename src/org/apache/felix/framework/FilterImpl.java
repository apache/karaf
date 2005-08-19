/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.*;

import org.apache.felix.framework.util.CaseInsensitiveMap;
import org.apache.felix.framework.util.ldap.*;
import org.osgi.framework.*;

/**
 * This class implements an RFC 1960-based filter. The syntax of the
 * filter string is the string representation of LDAP search filters
 * as defined in RFC 1960. These filters are used to search for services
 * and to track services using <tt>ServiceTracker</tt> objects.
**/
public class FilterImpl implements Filter
{
    private LogWrapper m_logger = null;
    private String m_toString = null;
    private Evaluator m_evaluator = null;
    private SimpleMapper m_mapper = null;

// TODO: FilterImpl needs a logger, this is a hack to get FrameworkUtil to compile.
    public FilterImpl(String expr) throws InvalidSyntaxException
    {
        this(null, expr);
    }

    /**
     * Construct a filter for a given filter expression string.
     * @param expr the filter expression string for the filter.
    **/
    public FilterImpl(LogWrapper logger, String expr) throws InvalidSyntaxException
    {
        m_logger = logger;
        if (expr == null)
        {
            throw new InvalidSyntaxException("Filter cannot be null", null);
        }

        if (expr != null)
        {
            CharArrayReader car = new CharArrayReader(expr.toCharArray());
            LdapLexer lexer = new LdapLexer(car);
            Parser parser = new Parser(lexer);
            try
            {
                if (!parser.start())
                {
                    throw new InvalidSyntaxException(
                        "Failed to parse LDAP query.", expr);
                }
            }
            catch (ParseException ex)
            {
                throw new InvalidSyntaxException(
                    ex.getMessage(), expr);
            }
            catch (IOException ex)
            {
                throw new InvalidSyntaxException(
                    ex.getMessage(), expr);
            }
            m_evaluator = new Evaluator(parser.getProgram());
            m_mapper = new SimpleMapper();
        }
    }

    /**
     * Compares the <tt>Filter</tt> object to another.
     * @param o the object to compare this <tt>Filter</tt> against.
     * @return If the other object is a <tt>Filter</tt> object, it
     *         returns <tt>this.toString().equals(obj.toString())</tt>;
     *         <tt>false</tt> otherwise.
    **/
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        else if (o instanceof Filter)
        {
            return toString().equals(o.toString());
        }
        return false;
    }

    /**
     * Returns the hash code for the <tt>Filter</tt> object.
     * @return The value <tt>this.toString().hashCode()</tt>.
    **/
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Filter using a <tt>Dictionary</tt> object. The <tt>Filter</tt>
     * is executed using the <tt>Dictionary</tt> object's keys and values.
     * @param dict the <tt>Dictionary</tt> object whose keys and values
     *             are used to determine a match.
     * @return <tt>true</tt> if the <tt>Dictionary</tt> object's keys
     *         and values match this filter; <tt>false</tt> otherwise.
     * @throws IllegalArgumentException if the dictionary contains case
     *         variants of the same key name.
    **/
    public boolean match(Dictionary dict)
        throws IllegalArgumentException
    {
        try
        {
            m_mapper.setSource(dict);
            return m_evaluator.evaluate(m_mapper);
        }
        catch (AttributeNotFoundException ex)
        {
            m_logger.log(LogWrapper.LOG_DEBUG, "FilterImpl: " + ex);
        }
        catch (EvaluationException ex)
        {
            m_logger.log(LogWrapper.LOG_ERROR, "FilterImpl: " + toString(), ex);
        }
        return false;
    }

    /**
     * Filter using a service's properties. The <tt>Filter</tt>
     * is executed using the properties of the referenced service.
     * @param ref A reference to the service whose properties
     *             are used to determine a match.
     * @return <tt>true</tt> if the service's properties match this
     *         filter; <tt>false</tt> otherwise.
    **/
    public boolean match(ServiceReference ref)
    {
        try
        {
            m_mapper.setSource(ref);
            return m_evaluator.evaluate(m_mapper);
        }
        catch (AttributeNotFoundException ex)
        {
            m_logger.log(LogWrapper.LOG_DEBUG, "FilterImpl: " + ex);
        }
        catch (EvaluationException ex)
        {
            m_logger.log(LogWrapper.LOG_ERROR, "FilterImpl: " + toString(), ex);
        }
        return false;
    }

    public boolean matchCase(Dictionary dictionary)
    {
        // TODO: Implement Filter.matchCase()
        return false;
    }

    /**
     * Returns the <tt>Filter</tt> object's filter string.
     * @return Filter string.
    **/
    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_evaluator.toStringInfix();
        }
        return m_toString;
    }

    static class SimpleMapper implements Mapper
    {
        private ServiceReference m_ref = null;
        private Map m_map = null;

        public void setSource(ServiceReference ref)
        {
            m_ref = ref;
            m_map = null;
        }

        public void setSource(Dictionary dict)
        {
            if (m_map == null)
            {
                m_map = new CaseInsensitiveMap();
            }
            else
            {
                m_map.clear();
            }

            if (dict != null)
            {
                Enumeration keys = dict.keys();
                while (keys.hasMoreElements())
                {
                    Object key = keys.nextElement();
                    if (m_map.get(key) == null)
                    {
                        m_map.put(key, dict.get(key));
                    }
                    else
                    {
                        throw new IllegalArgumentException(
                            "Duplicate attribute: " + key.toString());
                    }
                }
            }
            m_ref = null;
        }

        public Object lookup(String name)
        {
            if (m_map == null)
            {
                return m_ref.getProperty(name);
            }
            return m_map.get(name);
        }
    }
}
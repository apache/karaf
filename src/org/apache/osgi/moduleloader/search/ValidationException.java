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
package org.apache.osgi.moduleloader.search;

import org.apache.osgi.moduleloader.Module;

/**
 * <p>
 * This exception is thrown if a module cannot be validated. The module
 * that failed to be validated is recorded, along with the failed import target
 * identifier and version number. If the error was a result of a propagation
 * conflict, then the propagation error flag is set.
 * </p>
 * @see org.apache.osgi.moduleloader.search.ImportSearchPolicy#validate(org.apache.osgi.moduleloader.Module)
**/
public class ValidationException extends Exception
{
    private Module m_module = null;
    private Object m_identifier = null;
    private Object m_version = null;
    private boolean m_isPropagation = false;

    /**
     * Constructs an exception with the specified message, module,
     * import identifier, import version number, and propagation flag.
    **/
    public ValidationException(String msg, Module module,
        Object identifier, Object version, boolean isPropagation)
    {
        super(msg);
        m_module = module;
        m_identifier = identifier;
        m_version = version;
        m_isPropagation = isPropagation;
    }

    /**
     * Returns the module that was being validated.
     * @return the module that was being validated.
    **/
    public Module getModule()
    {
        return m_module;
    }

    /**
     * Returns the identifier of the import target that could not be resolved.
     * @return the identifier of the import target that could not be resolved.
    **/
    public Object getIdentifier()
    {
        return m_identifier;
    }

    /**
     * Returns the version number of the import target that could not be resolved.
     * @return the version number of the import target that could not be resolved.
    **/
    public Object getVersion()
    {
        return m_version;
    }

    /**
     * Returns a flag indicating whether the exception was caused by a
     * a propagation conflict.
     * @return <tt>true</tt> if the exception was thrown due to a propagation
     *         conflict, <tt>false</tt> otherwise.
    **/
    public boolean isPropagationError()
    {
        return m_isPropagation;
    }
}
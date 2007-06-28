/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.felix.mosgi.jmx.agent.mx4j.log;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.HashMap;

import javax.management.RuntimeOperationsException;

import org.apache.felix.mosgi.jmx.agent.mx4j.MX4JSystemKeys;

/**
 * Main class for the log service. <p>
 * The system property 'mx4j.log.priority' controls the priority of the standard logging, and defaults to 'warn'.
 * Possible values are, from least to greatest priority, the following (case insensitive):
 * <ul>
 * <li>trace
 * <li>debug
 * <li>info
 * <li>warn
 * <li>error
 * <li>fatal
 * </ul>
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class Log
{
	private static Logger m_prototype;
	private static Map m_prototypeMap = new HashMap();
	private static Map m_loggerCache = new HashMap();
	private static int m_defaultPriority;

	static
	{
		// Do not require callers up in the stack to have this permission
		String priority = (String)AccessController.doPrivileged(new PrivilegedAction()
		{
			public Object run()
			{
				 return System.getProperty(MX4JSystemKeys.MX4J_LOG_PRIORITY);
			}
		});
		if ("trace".equalsIgnoreCase(priority)) {m_defaultPriority = Logger.TRACE;}
		else if ("debug".equalsIgnoreCase(priority)) {m_defaultPriority = Logger.DEBUG;}
		else if ("info".equalsIgnoreCase(priority)) {m_defaultPriority = Logger.INFO;}
		else if ("warn".equalsIgnoreCase(priority)) {m_defaultPriority = Logger.WARN;}
		else if ("error".equalsIgnoreCase(priority)) {m_defaultPriority = Logger.ERROR;}
		else if ("fatal".equalsIgnoreCase(priority)) {m_defaultPriority = Logger.FATAL;}
		else {m_defaultPriority = Logger.INFO;}

		String prototype = (String)AccessController.doPrivileged(new PrivilegedAction()
		{
			public Object run()
			{
				 return System.getProperty(MX4JSystemKeys.MX4J_LOG_PROTOTYPE);
			}
		});
		if (prototype != null && prototype.trim().length() > 0)
		{
			try
			{
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				Class cls = cl.loadClass(prototype);
				redirectTo((Logger)cls.newInstance());
			}
			catch (Exception x)
			{
				x.printStackTrace();
				// Do nothing else: the user will see the exception trace
				// and understand that the property was wrong
			}
		}
	}

	private Log() {}

	/**
	 * Sets the default priority for all loggers.
	 * @see #setDefaultPriority
	 */
	public static void setDefaultPriority(int priority)
	{
		switch (priority)
		{
			case Logger.TRACE: m_defaultPriority = Logger.TRACE; break;
			case Logger.DEBUG: m_defaultPriority = Logger.DEBUG; break;
			case Logger.INFO: m_defaultPriority = Logger.INFO; break;
			case Logger.WARN: m_defaultPriority = Logger.WARN; break;
			case Logger.ERROR: m_defaultPriority = Logger.ERROR; break;
			case Logger.FATAL: m_defaultPriority = Logger.FATAL; break;
			default: m_defaultPriority = Logger.WARN; break;
		}
	}

	/**
	 * Returns the default priority.
	 * @see #setDefaultPriority
	 */
	public static int getDefaultPriority()
	{
		return m_defaultPriority;
	}

	/**
	 * Returns a new instance of a Logger associated with the given <code>category</code>;
	 * if {@link #redirectTo} has been called then a new instance of the prototype Logger, associated with the given
	 * <code>category<code>, is returned. This requires the prototype Logger class to have a public parameterless
	 * constructor.
	 */
	public static Logger getLogger(String category)
	{
		if (category == null) {throw new RuntimeOperationsException(new IllegalArgumentException("Category cannot be null"));}

		synchronized (m_loggerCache)
		{
			Logger logger = (Logger)m_loggerCache.get(category);
			if (logger == null)
			{
				// Try to see if a delegate for this category overrides other settings
				Logger prototype = null;
				synchronized (m_prototypeMap)
				{
					prototype = (Logger)m_prototypeMap.get(category);
				}
				if (prototype == null)
				{
					// Try to see if a prototype for all categories has been set
					if (m_prototype != null)
					{
						logger = createLogger(m_prototype, category);
					}
					else
					{
						logger = createLogger(null, category);
					}
				}
				else
				{
					logger = createLogger(prototype, category);
				}

				// cache it
				m_loggerCache.put(category, logger);
			}
			return logger;
		}
	}

	private static Logger createLogger(Logger prototype, String category)
	{
		Logger logger = null;
		try
		{
			logger = prototype == null ? new Logger() : (Logger)prototype.getClass().newInstance();
		}
		catch (Exception x)
		{
			x.printStackTrace();
			logger = new Logger();
		}
		logger.setCategory(category);
		logger.setPriority(m_defaultPriority);
		return logger;
	}

	/**
	 * Tells to the log service to use the given <code>delegate</code> Logger to perform logging. <br>
	 * Use a null delegate to remove redirection.
	 * @see #getLogger
	 */
	public static void redirectTo(Logger prototype)
	{
		m_prototype = prototype;

		// Clear the cache, as we want requests for new loggers to be generated again.
		synchronized (m_loggerCache)
		{
			m_loggerCache.clear();
		}
	}

	/**
	 * Tells to the log service to use the given <code>delegate</code> Logger to perform logging for the given
	 * category (that cannot be null). <br>
	 * Settings made using this method overrides the ones made with {@link #redirectTo(Logger) redirectTo}, meaning
	 * that it is possible to redirect all the log to a certain delegate but certain categories.
	 * Use a null delegate to remove redirection for the specified category.
	 * @see #getLogger
	 */
	public static void redirectTo(Logger prototype, String category)
	{
		if (category == null) {throw new RuntimeOperationsException(new IllegalArgumentException("Category cannot be null"));}

		if (prototype == null)
		{
			// Remove the redirection
			synchronized (m_prototypeMap)
			{
				m_prototypeMap.remove(category);
			}

			// Clear the cache for this category
			synchronized (m_loggerCache)
			{
				m_loggerCache.remove(category);
			}
		}
		else
		{
			// Put or replace
			synchronized (m_prototypeMap)
			{
				m_prototypeMap.put(category, prototype);
			}

			// Clear the cache for this category
			synchronized (m_loggerCache)
			{
				m_loggerCache.remove(category);
			}
		}
	}
}

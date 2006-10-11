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

/**
 * Base class for logging objects.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class Logger
{
	public static final int TRACE = 0;
	public static final int DEBUG = TRACE + 10;
	public static final int INFO = DEBUG + 10;
	public static final int WARN = INFO + 10;
	public static final int ERROR = WARN + 10;
	public static final int FATAL = ERROR + 10;

	private int m_priority = WARN;
	private String m_category;

	protected Logger() {}

	public void setPriority(int priority) {m_priority = priority;}
	public int getPriority() {return m_priority;}

	public String getCategory() {return m_category;}
	protected void setCategory(String category) {m_category = category;}

	public final boolean isEnabledFor(int priority)
	{
		return priority >= getPriority();
	}

	public final void fatal(Object message) {log(FATAL, message, null);}
	public final void fatal(Object message, Throwable t) {log(FATAL, message, t);}
	public final void error(Object message) {log(ERROR, message, null);}
	public final void error(Object message, Throwable t) {log(ERROR, message, t);}
	public final void warn(Object message) {log(WARN, message, null);}
	public final void warn(Object message, Throwable t) {log(WARN, message, t);}
	public final void info(Object message) {log(INFO, message, null);}
	public final void info(Object message, Throwable t) {log(INFO, message, t);}
	public final void debug(Object message) {log(DEBUG, message, null);}
	public final void debug(Object message, Throwable t) {log(DEBUG, message, t);}
	public final void trace(Object message) {log(TRACE, message, null);}
	public final void trace(Object message, Throwable t) {log(TRACE, message, t);}

	protected void log(int priority, Object message, Throwable t)
	{
		if (isEnabledFor(priority))
		{
			System.out.println(message);
			if (t != null)
			{
				t.printStackTrace(System.out);
			}
		}
	}
}

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
package org.apache.felix.jmood.compendium;

import javax.management.openmbean.CompositeData;

public interface LogManagerMBean {
	//FUTURE WORK: add persistence to sequence numbers
	public abstract void setLogLevel(int level) throws Exception;
	public abstract int getLogLevel() throws Exception;
	public abstract CompositeData[] getLog() throws Exception;
	/**
	 * This method exposes the attribute LogFromReader for remote management. The main difference with the log attribute is that the later 
	 * uses the level configuration specified by the log level attribute and as a drawback does not include log entries registered before the log manager was started.
	 * @return
	 */
	public abstract String[] getLogMessages() throws Exception;
}
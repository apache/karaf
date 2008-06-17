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
package org.apache.felix.mosgi.console.component;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.apache.felix.mosgi.console.ifc.CommonPlugin;
import java.lang.String;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

public class Activator implements BundleActivator {

  public static ObjectName REMOTE_LOGGER_ON = null;
  static {
    try {
      REMOTE_LOGGER_ON = new ObjectName("OSGI:name=Remote Logger");
    } catch (MalformedObjectNameException mone) {
      //
    }
  }
  
  public void start(BundleContext context) throws Exception{ 
    String propVal=new String("both");
    String propValue=context.getProperty("mosgi.jmxconsole.remotelogger.componentfilter");
    if (propValue!=null) {
      if (propValue.equals("treeonly") | propValue.equals("tableonly") | propValue.equals("both") | propValue.equals("none")) {
        propVal=propValue;
      }else {
        propVal="both";
      }
    }

    if (propVal.equals("treeonly") | propVal.equals("both")) {
      context.registerService(CommonPlugin.class.getName(), new RemoteLogger_jtree(context), null);
    }
    if (propVal.equals("tableonly") | propVal.equals("both")) {
      context.registerService(CommonPlugin.class.getName(), new RemoteLogger_jtable(), null);
    }
  }

  public void stop(BundleContext context) {
  }

//        m_context.registerService( Plugin.class.getName(), new NodeDetails(), null);
//        m_context.registerService( Plugin.class.getName(), new BundleListPanel(), null);
//
//        m_context.registerService( CommonPlugin.class.getName(), new OBRPlugin(context), null);
//
//        m_context.registerService( Plugin.class.getName(), new MemoryLauncher(context), null);
//        m_context.registerService( Plugin.class.getName(), new LinuxDetails(), null);
//

}

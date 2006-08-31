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
package org.apache.felix.mosgi.managedelements.memoryprobe.tab;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

//import org.mc4j.console.swing.graph.AbstractGraphPanel;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

public class AttributeGraphPanel extends AbstractGraphPanel {

    protected ObjectName objectName;
    protected MBeanServer server;


    public AttributeGraphPanel() {

    }

    protected String createName() {
        StringBuffer buf = new StringBuffer();
        buf.append(this.objectName.getDomain());
        Hashtable props = this.objectName.getKeyPropertyList();
        buf.append(" {");
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry =  (Map.Entry) iterator.next();
            buf.append(" ");
            buf.append(entry.getKey());
            buf.append("=");
            buf.append(entry.getValue());
        }
        buf.append("}");
        return buf.toString();
    }


    public void addObservation() throws Exception {
      System.out.println("AttributeGraphPanel.addObservation: I should not be there");
    }


}

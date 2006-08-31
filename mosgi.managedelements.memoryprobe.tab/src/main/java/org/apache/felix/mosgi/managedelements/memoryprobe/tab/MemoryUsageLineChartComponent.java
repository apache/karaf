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

import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MemoryUsageLineChartComponent extends NumericAttributeGraph {

    public static final String COMMITTED = "Committed";
    public static final String INIT = "Initial";
    public static final String MAX = "Maximum";
    public static final String USED = "Used";

    protected DefaultPieDataset pieDataset;

    protected List beanList;


    ObjectName permGen, tenuredGen, codeCache, edenSpace, survivorSpace = null;
    

    public MemoryUsageLineChartComponent()  {

	this.chart.setTitle("Memory Usage");
    	
    	try {
    		permGen = new ObjectName("java.lang:type=MemoryPool,name=Perm Gen");
    		tenuredGen = new ObjectName("java.lang:type=MemoryPool,name=Tenured Gen");
    		codeCache = new ObjectName("java.lang:type=MemoryPool,name=Code Cache");
    		edenSpace = new ObjectName("java.lang:type=MemoryPool,name=Eden Space");
    		survivorSpace = new ObjectName("java.lang:type=MemoryPool,name=Survivor Space");
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	createTimeSeries("Permanent Generation", permGen);
    	createTimeSeries("Tenured Generation", tenuredGen);
    	createTimeSeries("Code Cache", codeCache);
    	createTimeSeries("Eden Space", edenSpace);
    	createTimeSeries("Survivor Space", survivorSpace);
        reschedule();
    }
    
    public void addObservation() throws Exception {
    	//MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    	MemoryUsage usage = MemoryUsage.from((CompositeData) MemoryProbeTabUI.mbs.getAttribute(permGen, "Usage"));
    	MemoryUsage usage2 = MemoryUsage.from((CompositeData) MemoryProbeTabUI.mbs.getAttribute(tenuredGen, "Usage"));
    	MemoryUsage usage3 = MemoryUsage.from((CompositeData) MemoryProbeTabUI.mbs.getAttribute(codeCache, "Usage"));
    	MemoryUsage usage4 = MemoryUsage.from((CompositeData) MemoryProbeTabUI.mbs.getAttribute(edenSpace, "Usage"));
    	MemoryUsage usage5 = MemoryUsage.from((CompositeData) MemoryProbeTabUI.mbs.getAttribute(survivorSpace, "Usage"));
        getTimeSeries(permGen).add( new Millisecond(), usage.getUsed() );
        getTimeSeries(tenuredGen).add( new Millisecond(), usage2.getUsed() );
        getTimeSeries(codeCache).add( new Millisecond(), usage3.getUsed() );
        getTimeSeries(edenSpace).add( new Millisecond(), usage4.getUsed() );
        getTimeSeries(survivorSpace).add( new Millisecond(), usage5.getUsed() );
        
    }

    
    
    public static void main(String[] args) {
    	MemoryUsageLineChartComponent graph = new MemoryUsageLineChartComponent();

        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        frame.getContentPane().add(panel);
        panel.add(graph, BorderLayout.CENTER);

        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }

    
    
}

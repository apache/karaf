package org.apache.geronimo.gshell.spring;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.util.HashMap;
import java.util.Map;

import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.layout.NotFoundException;
import org.apache.geronimo.gshell.layout.model.CommandNode;
import org.apache.geronimo.gshell.layout.model.GroupNode;
import org.apache.geronimo.gshell.layout.model.Layout;
import org.apache.geronimo.gshell.layout.model.Node;

/**
 * The spring implementation of the {@link LayoutManager}.
 *
 * @version $Rev$ $Date$
 */
public class OsgiLayoutManager implements LayoutManager 
{
	private final HashMap<String, Layout> layouts = new HashMap<String, Layout>();
	private String shell = "";
	
    public void register(final Command command, Map<String, ?> properties) {
    	String shellName = (String) properties.get("shell");
    	if( shellName!=null ) {
        	Layout layout = getLayoutForShell(shellName);
    		
        	String id = command.getId();
        	String alias = (String) properties.get("alias");
        	if( alias == null ) {
	        	alias = id;
	        	int p;
	        	if( (p=alias.lastIndexOf(":")) >= 0 ) {
	        		alias = alias.substring(p+1);
	        	}
	        	layout.add(new CommandNode(alias, id));    		
        	} else {
        		String[] aliases = alias.split(",");
        		for (String a : aliases) {
        			a = a.trim();
					if( a.length()> 0 ) {
			        	layout.add(new CommandNode(a, id));    		
					}
				}
        	}
        	
    	}
    }


    public void unregister(final Command command, Map<String, ?> properties) {
    	// TODO: layout does not allow removal of commands at this time.    	
    }
    
	public Node findNode(String path) throws NotFoundException {
		return findNode(getLayout(), path);
	}

	public Node findNode(Node start, String path) throws NotFoundException {
        assert start != null;
        assert path != null;

        if (!(start instanceof GroupNode)) {
        	throw new NotFoundException(path);
        }
        
        Node node = ((GroupNode)start).find(path);
        if (node == null) {
            throw new NotFoundException(path);
        }
        
        return node;        
	}

	public Layout getLayout() {
		return getLayoutForShell(shell);
	}
	
	public Layout getLayoutForShell(String shellName) {
		Layout layout = layouts.get(shellName);
		if( layout == null ) {
			layout = new Layout();
			layouts.put(shellName, layout);
		}
		return layout;
	}
	
	public String getShell() {
		return shell;
	}

	public void setShell(String shell) {
		this.shell = shell;
	}

	public LayoutManager createLayoutManagerForShell(final String shell) {
		return new LayoutManager() {

			public Node findNode(String path) throws NotFoundException {
				return OsgiLayoutManager.this.findNode(getLayoutForShell(shell), path);
			}

			public Node findNode(Node node, String path) throws NotFoundException {
				return OsgiLayoutManager.this.findNode(node, path);
			}

			public Layout getLayout() {
				return getLayoutForShell(shell);
			}
		};
	}

}
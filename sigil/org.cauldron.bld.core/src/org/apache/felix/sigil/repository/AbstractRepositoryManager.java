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

package org.apache.felix.sigil.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.sigil.core.repository.BundleResolver;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.eclipse.ILibrary;
import org.apache.felix.sigil.model.eclipse.ILibraryImport;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.repository.RepositoryChangeEvent.Type;

public abstract class AbstractRepositoryManager implements IRepositoryManager, IBundleRepositoryListener {

	private HashSet<IRepositoryChangeListener> listeners = new HashSet<IRepositoryChangeListener>();
	
	private boolean initialised;
	
	private HashMap<String, IBundleRepository> repositories = new HashMap<String, IBundleRepository>();
	private ArrayList<IBundleRepository> order = new ArrayList<IBundleRepository>();
	private TreeMap<Integer, HashSet<IBundleRepository>> levelMap = new TreeMap<Integer, HashSet<IBundleRepository>>();
	private int[] levels;
	
	private BundleResolver resolver = new BundleResolver(this);
	
	private ArrayList<ILibrary> libraries = new ArrayList<ILibrary>();
	
	public void initialise() {
		synchronized( repositories ) {
			if ( !initialised ) {
				initialised = true;
				loadRepositories();
			}
		}
	}
	
	protected abstract void loadRepositories();

	public void addRepositoryChangeListener(IRepositoryChangeListener listener) {
		synchronized(listeners) {
			listeners.add(listener);			
		}
	}

	public void removeRepositoryChangeListener(IRepositoryChangeListener listener) {
		synchronized(listeners) {
			listeners.remove(listener);
		}
	}
	
	public void notifyChange(IBundleRepository repository) {
		notifyListeners( new RepositoryChangeEvent(repository, Type.CHANGED ) );
	}

	private void notifyListeners(RepositoryChangeEvent event) {
		ArrayList<IRepositoryChangeListener> safe = null;
		synchronized(listeners) {
			safe = new ArrayList<IRepositoryChangeListener>(listeners);
		}
		for ( IRepositoryChangeListener l : safe ) {
			l.repositoryChanged(event);
		}
	}
	
	protected void setRepositories(IBundleRepository[] repos) {
		synchronized( repositories ) {
			repositories.clear();
			order.clear();
			levelMap.clear();
			resetLevels();
			if ( repos != null ) {
				for ( int i = 0; i < repos.length; i++ ) {
					addRepository(repos[i], i);
				}
			}
		}
	}

	protected void addRepository(IBundleRepository rep, int level) {
		Type type = null;
		
		synchronized( repositories ) {
			IBundleRepository old = repositories.put(rep.getId(), rep); 
			if ( old == null ) {
				type = Type.ADDED;
				rep.addBundleRepositoryListener(this);
			}
			else {
				old.removeBundleRepositoryListener(this);
				type = Type.CHANGED;
				order.remove(old);
				clearLevel(rep);
			}
			
			order.add(rep);
			
			HashSet<IBundleRepository> set = levelMap.get(level);
			
			if ( set == null ) {
				set = new HashSet<IBundleRepository>();
				levelMap.put( level, set );
			}
			
			set.add( rep );
			resetLevels();
		}
		
		notifyListeners( new RepositoryChangeEvent(rep, type ) );
	}
	
	protected void removeRepository(IBundleRepository rep) {
		Type type = null;
		
		synchronized( repositories ) {
			if ( repositories.remove(rep.getId()) != null ) {
				order.remove(rep);
				type = Type.REMOVED;
				clearLevel(rep);
				resetLevels();
			}
		}
		
		if ( type != null ) {
			notifyListeners( new RepositoryChangeEvent(rep, type ) );
		}
	}
	
	private void clearLevel(IBundleRepository rep) {
		for ( Iterator<Map.Entry<Integer, HashSet<IBundleRepository>>> iter = levelMap.entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<Integer, HashSet<IBundleRepository>> e = iter.next();
			if ( e.getValue().remove(rep) ) {
				if ( e.getValue().isEmpty() ) {
					iter.remove();
				}					
				break;
			}				
		}
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.repository.IRepositoryManager#getRepositories()
	 */
	public Collection<IBundleRepository> getRepositories() {
		initialise();
		ArrayList<IBundleRepository> safe = null;
		
		synchronized( repositories ) {
			safe = new ArrayList<IBundleRepository>( order );
		}
		
		return safe;
	}

	private void resetLevels() {
		Collections.sort(order, new Comparator<IBundleRepository>() {
			public int compare(IBundleRepository o1, IBundleRepository o2) {
				int l1 = findLevel(o1);
				int l2 = findLevel(o2);
				
				if ( l1 < l2 ) {
					return -1;
				}
				else if ( l1 > l2 ) {
					return 1;
				}
				else {
					return 0;
				}
			}

			private int findLevel(IBundleRepository rep) {
				for ( Map.Entry<Integer, HashSet<IBundleRepository>> e : levelMap.entrySet() ) {
					if ( e.getValue().contains( rep ) ) {
						return e.getKey();
					}
				}
				throw new IllegalStateException();
			}
		});
		levels = new int[levelMap.size()];
		int i = 0;
		for ( Integer v : levelMap.keySet() ) {
			levels[i++] = v;
		}			
	}
	

	public int[] getPriorityLevels() {
		initialise();
		synchronized( repositories ) {
			return levels;
		}
	}

	public Collection<IBundleRepository> getRepositories(int priorityLevel) {
		initialise();
		List<IBundleRepository> found = null;
		
		synchronized (repositories) {
			HashSet<IBundleRepository> b = levelMap.get(priorityLevel);
			if ( b == null ) {
				found = Collections.emptyList();
			}
			else {
				found = new ArrayList<IBundleRepository>(b);
			}
		}
		
		return found;
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.repository.IRepositoryManager#addLibrary(org.cauldron.sigil.model.eclipse.ILibrary)
	 */
	public void addLibrary(ILibrary library) {
		synchronized( libraries ) {
			libraries.add(library);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.repository.IRepositoryManager#removeLibrary(org.cauldron.sigil.model.eclipse.ILibrary)
	 */
	public void removeLibrary(ILibrary library) {
		synchronized( libraries ) {
			libraries.remove(library);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.repository.IRepositoryManager#getLibraries()
	 */
	public Collection<ILibrary> getLibraries() {
		synchronized( libraries ) {
			return libraries;
		}
	}

	/* (non-Javadoc)
	 * @see org.cauldron.sigil.internal.repository.IRepositoryManager#resolveLibrary(org.cauldron.sigil.model.eclipse.ILibraryImport)
	 */
	public ILibrary resolveLibrary(final ILibraryImport l) {
		final ArrayList<ILibrary> found = new ArrayList<ILibrary>(1);
		//ISigilProjectModel p = l.getAncestor(ISigilProjectModel.class);
		//
		//IModelWalker w = new IModelWalker() {
		//	public boolean visit(IModelElement element) {
		//		if ( element instanceof ILibrary ) {
		//			updateLibrary(l, (ILibrary) element, found);
		//			return false;
		//		}
		//		
		//		return true;
		//	}
		//};
		
		//p.visit( w );
		
		//if ( found.isEmpty() ) { // no project specific libraries - check workspace definitions
			synchronized( libraries ) {
				for ( ILibrary lib : libraries ) {
					if ( l.getLibraryName().equals( lib.getName() ) && l.getVersions().contains(lib.getVersion()) ) {
						updateLibrary(l, lib, found);
					}
				}
			}
		//}
		
		return found.isEmpty() ? null : found.get(0);		
	}

	protected void updateLibrary(ILibraryImport li, ILibrary l, ArrayList<ILibrary> found) {
		if ( li.getLibraryName().equals( l.getName() ) && li.getVersions().contains(l.getVersion()) ) {
			if ( found.isEmpty() ) {
				found.add( l );
			}
			else {
				ILibrary c = found.get(0);
				if ( l.getVersion().compareTo(c.getVersion()) > 0 ) {
					found.remove(0);
					found.add(l);
				}
			}
		}
	}

	public IBundleResolver getBundleResolver() {
		return resolver;
	}

	public void visit(final IModelWalker walker) {
		for (IBundleRepository rep : getRepositories()) {
			IRepositoryVisitor wrapper = new IRepositoryVisitor() {
				public boolean visit(ISigilBundle bundle) {
					bundle.visit(walker);
					// return true as still want to visit other bundles
					return true;
				}
			};
			rep.accept(wrapper);
		}
	}
}
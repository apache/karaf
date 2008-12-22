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
package org.apache.felix.ipojo.handler.temporal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.ServiceReference;

/**
* Maintains a service object collection.
* This collection wrap the temporal dependency to be accessible from a
* {@link Collection}, that can be passed to helper objects (Collaborators).
* 
* The onTimeout policies are executed when the {@link Collection#iterator()},
* {@link Collection#toArray(Object[])} and {@link Collection#toArray()} methods
* are called. 
* 
* The {@link Collection#iterator()} method returns an {@link Iterator} iterating
* on a cached copy of available service objects. In the case that there are no 
* available services when the timeout is reached, the policies act as follows:
* <ul>
* <li>'null' returns a null iterator</li>
* <li>'nullable' and default-implementation returns an iterator iterating on one object (the
*  nullable or default-implementation object</li>
* <li>'empty' returns an empty iterator.</li>
* <li>'no policy' throws runtime exception</li>
* </ul>
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class ServiceCollection implements Collection {
    
    /**
     * The wrapped temporal dependencies.
     */
    private TemporalDependency m_dependency;
        
    /**
     * Creates a Service Collection.
     * @param dep the wrapped temporal dependencies
     */
    public ServiceCollection(TemporalDependency dep) {
        m_dependency = dep;
    }

    /**
     * Unsupported method.
     * @param o an object
     * @return N/A
     * @see java.util.Collection#add(java.lang.Object)
     */
    public boolean add(Object o) {
        throw new UnsupportedOperationException("Cannot add elements inside this collection");
    }

    /**
     * Unsupported method.
     * @param c an object
     * @return N/A
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException("Cannot add elements inside this collection");
    }

    /**
     * Unsupported method.
     * @see java.util.Collection#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException("Cannot remove elements from this collection");
    }

    /**
     * Checks if the wrapped temporal dependencies has always access to the
     * given service object.The method allows knowing if the provider returning the
     * service object has leaved. 
     * @param o  the service object
     * @return <code>true</code> if the object is still available,
     * <code>false</code> otherwise.
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        return getAvailableObjects().contains(o);
    }

    /**
     * Checks if the wrapped temporal dependencies has always access to the
     * given service objects.The method allows knowing if providers returning the
     * service objects have leaved. 
     * @param c the set of service object
     * @return <code>true</code> if the objects are still available,
     * <code>false</code> otherwise.
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean containsAll(Collection c) {
        return getAvailableObjects().containsAll(c);
    }

    /**
     * Checks if at least one provider matching with the dependency
     * is available.
     * @return <code>true</code> if one provider or more satisfying the
     * dependency are available. Otherwise, returns <code>false</code> 
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        return m_dependency.getSize() == 0;
    }
    
    /**
     * Helper method creating a list of available service objects.
     * @return the list of available service objects.
     */
    private List getAvailableObjects() {
        List list = new ArrayList();
        ServiceReference[] refs = m_dependency.getServiceReferences();
        if (refs != null) {
            for (int i = 0; i < refs.length; i++) {
                list.add(m_dependency.getService(refs[i]));
            }
        }
        return list;
    }

    /**
     * Gets an iterator on the actual list of available service objects.
     * This method applies on timeout policies is no services are 
     * available after the timeout.
     * The returned iterator iterates on a cached copy of the service
     * objects.
     * @return a iterator giving access to service objects.
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
        ServiceReference[] refs = m_dependency.getServiceReferences();
        if (refs != null) {
            // Immediate return.
            return new ServiceIterator(refs); // Create the service iterator with the service reference list.
        } else {
            // Begin to wait ...
            long enter = System.currentTimeMillis();
            boolean exhausted = false;
            synchronized (this) {
                while (m_dependency.getServiceReference() == null && !exhausted) {
                    try {
                        wait(1);
                    } catch (InterruptedException e) {
                        // We was interrupted ....
                    } finally {
                        long end = System.currentTimeMillis();
                        exhausted = (end - enter) > m_dependency.getTimeout();
                    }
                }
            }
            // Check
            if (exhausted) {
                Object oto = m_dependency.onTimeout(); // Throws the RuntimeException
                if (oto == null) { // If null, return null
                    return null;
                } else {
                    // oto is an instance of collection containing either empty or with only one element
                    return new ServiceIterator((Collection) oto);
                }
            } else {
                refs = m_dependency.getServiceReferences();
                return new ServiceIterator(refs);                
            }
        }
        
      
    }
    
    /**
     * Unsupported method.
     * @param o a object
     * @return N/A
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Cannot remove elements from this collection");
    }

    /**
     * Unsupported method.
     * @param c a set of objects
     * @return N/A
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Cannot remove elements from this collection");
    }

    /**
     *Unsupported method.
     * @param c a set of objects
     * @return N/A
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Cannot remove elements from this collection");
    }

    /**
     * Gets the number of available providers.
     * @return the number of matching service providers.
     * @see java.util.Collection#size()
     */
    public int size() {
        return m_dependency.getSize();
    }

    /**
     * Returns an array containing available service objects.
     * This method executed on timeout policies if no matching
     * providers when the timeout is reached. 
     * @return a array containing available service objects.
     * depending on the timeout policy, this array can also be <code>null</code>,
     * be empty, or can contain only one element (a default-implementation
     * object, or a nullable object).
     * @see java.util.Collection#toArray()
     */
    public Object[] toArray() {
        return toArray(new Object[0]);
    }
    
    /**
     * Returns an array containing available service objects.
     * This method executed on timeout policies if no matching
     * providers when the timeout is reached. 
     * @param a the array into which the elements of this collection 
     * are to be stored, if it is big enough; otherwise, a new array
     * of the same runtime type is allocated for this purpose. 
     * @return a array containing available service objects.
     * depending on the timeout policy, this array can also be <code>null</code>,
     * be empty, or can contain only one element (a default-implementation
     * object, or a nullable object).
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] a) {
        Iterator it = iterator(); // Can throw an exception.
        if (it == null) {
            return null;
        }
        // Else we get an iterator.
        List list = new ArrayList(size());
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list.toArray(a);
    }
    
    /**
     * Iterator on a set of service objects.
     * This iterator iterates on a cached copy of service objects.
     */
    private final class ServiceIterator implements Iterator {
        
        /**
         * Underlying iterator.
         */
        private Iterator m_iterator;
        
        /**
         * Creates a Service Iterator iterating
         * on the given set of providers.
         * @param refs the available service providers
         */
        private ServiceIterator(ServiceReference[] refs) {
            List objects = new ArrayList(refs.length);
            for (int i = 0; i < refs.length; i++) {
                objects.add(m_dependency.getService(refs[i]));
            }           
            m_iterator = objects.iterator();
        }
        
        /**
         * Creates a Service Iterator iterating
         * on service object contained in the given
         * collection.
         * @param col a collection containing service objects.
         */
        private ServiceIterator(Collection col) {
            m_iterator = col.iterator();
        }

        /**
         * Returns <code>true</code> if the iteration has 
         * more service objects. 
         * @return <code>true</code> if the iterator has more elements.
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return m_iterator.hasNext();
        }

        /**
         * Returns the next service objects in the iteration. 
         * @return the next service object in the iteration. 
         * @see java.util.Iterator#next()
         */
        public Object next() {
            return m_iterator.next();
        }

        /**
         * Unsupported operation.
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();            
        }
                  
    }

}

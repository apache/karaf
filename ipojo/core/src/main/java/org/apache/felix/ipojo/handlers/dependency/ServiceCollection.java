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
package org.apache.felix.ipojo.handlers.dependency;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
* Maintains a service object collection.
* This collection wrap the temporal dependency to be accessible from a
* {@link Collection}, that can be passed to helper objects (Collaborators).
* 
* 
* The {@link Collection#iterator()} method returns an {@link Iterator} iterating
* on a cached copy of available service objects. In the case that there are no 
* available services, the policies act as follows:
* <ul>
* <li>'null' returns a null iterator</li>
* <li>'nullable' and default-implementation returns an iterator iterating on one object (the
*  nullable or default-implementation object</li>
* <li>'empty' returns an empty iterator.</li>
* </ul>
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class ServiceCollection implements Collection, List, Set {
    
    /**
     * The wrapped dependency.
     */
    private Dependency m_dependency;
        
    /**
     * Creates a Service Collection.
     * @param dep the wrapped dependency
     */
    public ServiceCollection(Dependency dep) {
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
     * @param index an index
     * @param obj an object
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, Object obj) {
        throw new UnsupportedOperationException("Cannot add elements inside this collection");
    }

    /**
     * Unsupported method.
     * @param index an index
     * @param c an object
     * @return N/A
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection c) {
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
     * Checks if the wrapped dependency has always access to the
     * given service object.The method allows knowing if the provider returning the
     * service object has left. 
     * @param o  the service object
     * @return <code>true</code> if the object is still available,
     * <code>false</code> otherwise.
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        List list = (List) m_dependency.getService();
        return list.contains(o);
    }

    /**
     * Checks if the wrapped dependencies has always access to the
     * given service objects.The method allows knowing if providers returning the
     * service objects have left. 
     * @param c the set of service object
     * @return <code>true</code> if the objects are still available,
     * <code>false</code> otherwise.
     * @see java.util.Collection#contains(java.lang.Object)
     */
    public boolean containsAll(Collection c) {
        List list = (List) m_dependency.getService();
        return list.containsAll(c);
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
     * Gets an iterator on the current list of available service objects.
     * The returned iterator iterates on a cached copy of the service
     * objects.
     * @return a iterator giving access to service objects.
     * @see java.util.Collection#iterator()
     */
    public Iterator iterator() {
        List obj = (List) m_dependency.getService();
        return new ServiceIterator(obj); // Create the service iterator with the service reference list.
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
     * @param index the index
     * @return N/A
     * @see java.util.Collection#remove(java.lang.Object)
     */
    public Object remove(int index) {
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
     * Unsupported method.
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
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] a) {
        List list = (List) m_dependency.getService();
        return list.toArray(a);
    }
    
    /**
     * Gets the object stored at the given index.
     * @param index the index
     * @return the service object
     * @see java.util.List#get(int)
     */
    public Object get(int index) {
        List list = (List) m_dependency.getService();
        return list.get(index);
    }

    /**
     * Gets the index of the given object in the current
     * collection.
     * @param o the object 
     * @return the index of the object of <code>-1</code>
     * if not found.
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o) {
        List list = (List) m_dependency.getService();
        return list.indexOf(o);
    }

    /**
     * Gets the last index of the given object in the current
     * collection.
     * @param o the object 
     * @return the index of the object of <code>-1</code>
     * if not found.
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o) {
        List list = (List) m_dependency.getService();
        return list.lastIndexOf(o);
    }

    /**
     * Gets a list iterator on the current list of available service objects.
     * The returned iterator iterates on a cached copy of the service
     * objects.
     * @return a iterator giving access to service objects.
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator() {
        List obj = (List) m_dependency.getService();
        return new ServiceIterator(obj); // Create the service iterator with the service reference list.
    }

    /**
     * Unsupported Method.
     * @param index an index
     * @return N/A
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported Method.
     * @param arg0 an index
     * @param arg1 an object
     * @return N/A
     * @see java.util.List#set(int, E)
     */
    public Object set(int arg0, Object arg1) {
        throw new UnsupportedOperationException("Cannot add elements inside this collection");
    }

    /**
     * Returns a sublist from the current list.
     * @param fromIndex the index of the list beginning
     * @param toIndex the index of the list end
     * @return the sub-list
     * @see java.util.List#subList(int, int)
     */
    public List subList(int fromIndex, int toIndex) {
        List list = (List) m_dependency.getService();
        return list.subList(fromIndex, toIndex);
    }

    /**
     * Iterator on a set of service objects.
     * This iterator iterates on a cached copy of service objects.
     */
    private final class ServiceIterator implements ListIterator {
        
        /**
         * Underlying iterator.
         */
        private ListIterator m_iterator;
        
        /**
         * Creates a Service Iterator iterating
         * on the given set of providers.
         * @param list the list of service object.
         */
        private ServiceIterator(List list) {           
            m_iterator = list.listIterator();
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

        /**
         * Unsupported operation.
         * @param obj an object
         * @see java.util.ListIterator#add(E)
         */
        public void add(Object obj) {
            throw new UnsupportedOperationException();                        
        }

        /**
         * Checks if the is an element before the currently 
         * pointed one.
         * @return true if there is an element before the
         * current one.
         * @see java.util.ListIterator#hasPrevious()
         */
        public boolean hasPrevious() {
            return m_iterator.hasPrevious();
        }

        /**
         * Gets the index of the next element.
         * @return the index of the next element.
         * @see java.util.ListIterator#nextIndex()
         */
        public int nextIndex() {
            return m_iterator.nextIndex();
        }

        
        /**
         * Gets the previous elements.
         * @return the previous element
         * @see java.util.ListIterator#previous()
         */
        public Object previous() {
            return m_iterator.previous();
        }

        /**
         * Gets the index of the previous element.
         * @return the index of the previous element.
         * @see java.util.ListIterator#previousIndex()
         */
        public int previousIndex() {
            return m_iterator.previousIndex();
        }

        /**
         * Unsupported operation.
         * @param obj an object
         * @see java.util.ListIterator#set(E)
         */
        public void set(Object obj) {
            throw new UnsupportedOperationException();  
        }
                  
    }

}

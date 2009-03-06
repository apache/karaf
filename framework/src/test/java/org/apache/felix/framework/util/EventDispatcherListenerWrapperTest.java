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
package org.apache.felix.framework.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


public class EventDispatcherListenerWrapperTest extends TestCase {
    public void testRemove() {
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        Bundle b2 = getMockBundle();
        BundleContext bc2 = b2.getBundleContext();
        Bundle b3 = getMockBundle();
        BundleContext bc3 = b3.getBundleContext();
        Bundle b4 = getMockBundle();
        BundleContext bc4 = b4.getBundleContext();
        
        Object [] listeners = new Object[] {
                b1,
                String.class,
                new Object(),
                "(some=filter)",
                null,
                
                b2,
                Integer.class,
                new Object(),
                "(some.other=filter)",
                new Integer(15),
                
                b3,
                BundleContext.class,
                new Object(),
                null,
                Boolean.TRUE,               
        };
        
        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        assertEquals(3, c.size());
        assertFalse(c.isEmpty());
        assertTrue(c.contains(bc1));
        assertTrue(c.contains(bc2));
        assertTrue(c.contains(bc3));
        assertFalse(c.contains(bc4));
        
        assertTrue(c.remove(bc2));
        assertEquals(2, c.size());
        assertTrue(c.contains(bc1));
        assertFalse(c.contains(bc2));
        assertTrue(c.contains(bc3));
        assertFalse(c.contains(bc4));
        assertFalse("Already removed", c.remove(bc2));        

        Object [] actualListeners = 
            ((EventDispatcher.ListenerBundleContextCollectionWrapper) c).getListeners();
        Object [] expectedListeners = new Object[10];
        System.arraycopy(listeners, 0, expectedListeners, 0, 5);
        System.arraycopy(listeners, 10, expectedListeners, 5, 5);
        assertTrue(Arrays.equals(expectedListeners, actualListeners));

        assertTrue(c.remove(bc1));
        assertEquals(1, c.size());
        assertFalse(c.contains(bc1));
        assertFalse(c.contains(bc2));
        assertTrue(c.contains(bc3));
        assertFalse(c.contains(bc4));
        assertFalse(c.isEmpty());

        Object [] actualListeners2 = 
            ((EventDispatcher.ListenerBundleContextCollectionWrapper) c).getListeners();
        Object [] expectedListeners2 = new Object[5];
        System.arraycopy(listeners, 10, expectedListeners2, 0, 5);
        assertTrue(Arrays.equals(expectedListeners2, actualListeners2));

        assertTrue(c.remove(bc3));
        assertEquals(0, c.size());
        assertFalse(c.contains(bc1));
        assertFalse(c.contains(bc2));
        assertFalse(c.contains(bc3));
        assertFalse(c.contains(bc4));
        assertTrue(c.isEmpty());

        Object [] actualListeners3 = 
            ((EventDispatcher.ListenerBundleContextCollectionWrapper) c).getListeners();
        assertEquals(0, actualListeners3.length);
    }    
    
    public void testIterator() {
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        Bundle b2 = getMockBundle();
        BundleContext bc2 = b2.getBundleContext();
        
        Object [] listeners = new Object[] {
                b1,
                String.class,
                new Object(),
                "(some=filter)",
                null,
                
                b2,
                Integer.class,
                new Object(),
                "(some.other=filter)",
                new Integer(15)                
        };

        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        Iterator it = c.iterator();
        
        assertEquals(2, c.size());
        assertTrue(it.hasNext());
        try {
            it.remove();
            fail("Should throw an exception");
        } catch (IllegalStateException ise) {
            // good
        }        
        assertSame(bc1, it.next());
        it.remove();
        assertEquals(1, c.size());
        
        // Create another iterator and make sure it sees the removal of it
        Iterator it2 = c.iterator();
        assertTrue(it2.hasNext());
        assertSame(bc2, it2.next());
        assertFalse(it2.hasNext());
        
        // back to the origial iterator
        
        try {
            it.remove();
            fail("Should throw an exception");
        } catch (IllegalStateException ise) {
            // good
        }        
        assertTrue(it.hasNext());
        try {
            it.remove();
            fail("Should throw an exception");
        } catch (IllegalStateException ise) {
            // good
        }        
        assertSame(bc2, it.next());
        it.remove();
        assertEquals(0, c.size());
        
        assertFalse(it.hasNext());
        try {
            it.next();
            fail("Should throw an exception");
        } catch (NoSuchElementException nse) {
            // good
        }        
        try {
            it.remove();
            fail("Should throw an exception");
        } catch (IllegalStateException ise) {
            // good
        }                
    }
    
    public void testAdd() {
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        
        Object [] listeners = new Object[] {
                b1,
                String.class,
                new Object(),
                "(some=filter)",
                null,
        };

        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        try {
            c.add(new Object());
            fail("Should not have been able to add to the collection");
        } catch (UnsupportedOperationException uoe) {
            // good
        }
    }

    public void testAddAll() {       
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        Object [] listeners = {};

        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        try {
            c.addAll(Collections.singleton(bc1));
            fail("Should not have been able to add to the collection");
        } catch (UnsupportedOperationException uoe) {
            // good
        }
    }
    
    public void testContainsAll() {
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        Bundle b2 = getMockBundle();
        BundleContext bc2 = b2.getBundleContext();
        Bundle b3 = getMockBundle();
        BundleContext bc3 = b3.getBundleContext();
        
        Object [] listeners = new Object[] {
                b1,
                String.class,
                new Object(),
                "(some=filter)",
                null,
                
                b2,
                Integer.class,
                new Object(),
                "(some.other=filter)",
                new Integer(15),
                
                b3,
                BundleContext.class,
                new Object(),
                null,
                Boolean.TRUE,               
        };
        
        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        
        assertTrue(c.containsAll(Collections.emptySet()));
        assertTrue(c.containsAll(Collections.singleton(bc2)));
        assertTrue(c.containsAll(Arrays.asList(new Object [] {bc2, bc1})));
        assertTrue(c.containsAll(Arrays.asList(new Object [] {bc3, bc2, bc1})));
        assertFalse(c.containsAll(Arrays.asList(new Object [] {bc3, bc2, bc1, new Object()})));
        
        assertEquals(3, c.size());
        c.clear();
        assertEquals(0, c.size());
    }

    public void testRemoveAll() {
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        Bundle b2 = getMockBundle();
        BundleContext bc2 = b2.getBundleContext();
        Bundle b3 = getMockBundle();
        BundleContext bc3 = b3.getBundleContext();
        
        Object [] listeners = new Object[] {
                b1,
                String.class,
                new Object(),
                "(some=filter)",
                null,
                
                b2,
                Integer.class,
                new Object(),
                "(some.other=filter)",
                new Integer(15),
                
                b3,
                BundleContext.class,
                new Object(),
                null,
                Boolean.TRUE,               
        };
        
        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        assertFalse(c.removeAll(Collections.emptyList()));
        assertFalse(c.removeAll(Collections.singleton(new Object())));
        assertTrue(c.contains(bc2));
        assertTrue(c.removeAll(Arrays.asList(new Object [] {new Object(), bc2})));
        assertFalse(c.contains(bc2));
        
        assertEquals(2, c.size());
        assertTrue(c.removeAll(Arrays.asList(new Object [] {bc1, bc3})));
        assertEquals(0, c.size());
    }
    
    public void testRetainAll() {
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        Bundle b2 = getMockBundle();
        BundleContext bc2 = b2.getBundleContext();
        Bundle b3 = getMockBundle();
        BundleContext bc3 = b3.getBundleContext();
        
        Object [] listeners = new Object[] {
                b1,
                String.class,
                new Object(),
                "(some=filter)",
                null,
                
                b2,
                Integer.class,
                new Object(),
                "(some.other=filter)",
                new Integer(15),
                
                b3,
                BundleContext.class,
                new Object(),
                null,
                Boolean.TRUE,               
        };
        
        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        assertFalse(c.retainAll(Arrays.asList(new Object [] {bc3, bc1, bc2})));
        assertTrue(Arrays.equals(new Object [] {bc1, bc2, bc3}, c.toArray()));
        
        assertTrue(c.retainAll(Arrays.asList(new Object [] {bc1, bc2, new Object()})));
        assertTrue(Arrays.equals(new Object [] {bc1, bc2}, c.toArray()));
        
        assertTrue(c.retainAll(Collections.emptyList()));
        assertEquals(0, c.size());
    }
    
    public void testToArray() {
        Bundle b1 = getMockBundle();
        BundleContext bc1 = b1.getBundleContext();
        Bundle b2 = getMockBundle();
        BundleContext bc2 = b2.getBundleContext();        
        
        Object [] listeners = new Object[] {
                b1,
                String.class,
                new Object(),
                "(some=filter)",
                null,
                
                b2,
                Integer.class,
                new Object(),
                "(some.other=filter)",
                new Integer(15),                
        };
        
        Collection c = new EventDispatcher.ListenerBundleContextCollectionWrapper(listeners);
        assertTrue(Arrays.equals(new Object [] {bc1, bc2}, c.toArray()));
        assertTrue(Arrays.equals(new Object [] {bc1, bc2}, c.toArray(new Object [] {})));
        
        try {
            c.toArray(new String [] {});
            fail("Should not be allowed");
        } catch (ArrayStoreException ase) {
            // good
        }
    }
    
    private Bundle getMockBundle() {
        MockControl bcControl = MockControl.createNiceControl(BundleContext.class);
        BundleContext bc = (BundleContext) bcControl.getMock();
        
        MockControl bControl = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) bControl.getMock();
        b.getBundleContext();
        bControl.setReturnValue(bc, MockControl.ZERO_OR_MORE);
        b.getState();
        bControl.setReturnValue(Bundle.ACTIVE, MockControl.ZERO_OR_MORE);
        
        bc.getBundle();
        bcControl.setReturnValue(b, MockControl.ZERO_OR_MORE);                

        bcControl.replay();
        bControl.replay();
        
        return b;
    }
}

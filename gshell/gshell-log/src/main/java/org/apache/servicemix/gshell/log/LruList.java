package org.apache.servicemix.gshell.log;

import java.util.AbstractList;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Apr 28, 2008
 * Time: 6:15:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class LruList<E> {

    private E[] elements;
    private transient int start = 0;
    private transient int end = 0;
    private transient boolean full = false;
    private final int maxElements;

    public LruList(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        elements = (E[]) new Object[size];
        maxElements = elements.length;
    }

    public int size() {
        synchronized (elements) {
            int size = 0;
            if (end < start) {
                size = maxElements - start + end;
            } else if (end == start) {
                size = (full ? maxElements : 0);
            } else {
                size = end - start;
            }
            return size;
        }
    }

    public void add(E element) {
        synchronized (elements) {
            if (null == element) {
                 throw new NullPointerException("Attempted to add null object to buffer");
            }
            if (size() == maxElements) {
                Object e = elements[start];
                if (null != e) {
                    elements[start++] = null;
                    if (start >= maxElements) {
                        start = 0;
                    }
                    full = false;
                }
            }
            elements[end++] = element;
            if (end >= maxElements) {
                end = 0;
            }
            if (end == start) {
                full = true;
            }
        }
    }

    public Iterable<E> getElements() {
        synchronized (elements) {
            return getElements(size());
        }
    }

    public Iterable<E> getElements(int nb) {
        synchronized (elements) {
            int s = size();
            nb = Math.min(Math.max(0, nb), s);
            E[] e = (E[]) new Object[nb];
            for (int i = 0; i < nb; i++) {
                e[i] = elements[(i + s - nb + start) % maxElements];
            }
            return Arrays.asList(e);
        }
    }

}

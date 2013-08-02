package org.apache.karaf.shell.scr.support;

import org.apache.felix.scr.Component;

import java.util.Comparator;

public class IdComparator implements Comparator<Component> {
    public int compare(Component left, Component right) {
        if (left.getId() < right.getId()) {
            return -1;
        } else if (left.getId() == right.getId()) {
            return 0;
        } else {
            return 1;
        }
    }
}

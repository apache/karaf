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

import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LogarithmicTimeJSlider extends LogarithmicJSlider {

    protected static final DecimalFormat format = new DecimalFormat("#.#");

    protected void createLabels(Hashtable table, int increment, int start) {
        for (int labelIndex = start; labelIndex <= getMaximum(); labelIndex *= increment) {
            String label = formatMilleseconds(labelIndex);
            table.put(new Integer(labelIndex), new LabelUIResource(label, JLabel.CENTER));
        }
    }

    public String getTime() {
        return formatMilleseconds(getValue());
    }

    public String formatMilleseconds(int labelIndex) {
        String label;
        if (labelIndex >= (1000 * 60 * 60 * 24)) {
            label = format.format(labelIndex / (double)(1000 * 60 * 60 * 24)) + " days";
        } else if (labelIndex >= (1000 * 60 * 60)) {
            label = format.format(labelIndex / (double)(1000 * 60 * 60)) + " hours";
        } else if (labelIndex >= (double)(1000 * 60)) {
            label = format.format(labelIndex / (double)(1000 * 60)) + " mins";
        } else if (labelIndex >= 1000) {
            label = format.format(labelIndex / (double)1000) + " secs";
        } else {
            label = labelIndex + " ms";
        }
        return label;
    }

}

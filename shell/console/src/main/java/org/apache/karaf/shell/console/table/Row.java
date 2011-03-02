/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.shell.console.table;

import java.util.ArrayList;
import java.util.List;

/**
 * Row information.
 * 
 * @author ldywicki
 */
public class Row extends TableElement {

    /**
     * List of cells.
     */
    private List<Cell> cells = new ArrayList<Cell>();

    /**
     * Add borders?
     */
    private boolean borders;

    public Row(Object[] row) {
        this(true);
        for (Object object : row) {
            addCell(new Cell(object));
        }
    }

    public Row() {
        this(true);
    }

    public Row(boolean borders) {
        this.borders = borders;
    }

    public void addCell(Cell cell) {
        cells.add(cell);
    }

    public List<Cell> getCells() {
        return cells;
    }

    public boolean isBorders() {
        return borders;
    }

    public void addCell(Object value) {
        addCell(new Cell(value));
    }

}

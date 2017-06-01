/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tools.utils;

import org.apache.karaf.tools.utils.model.KarafPropertyEdit;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apply a set of edits, reading files from a stock etc dir.
 */
public class KarafPropertiesEditor {
    private File inputEtc;
    private File outputEtc;
    private KarafPropertyEdits edits;

    public KarafPropertiesEditor setInputEtc(File inputEtc) {
        this.inputEtc = inputEtc;
        return this;
    }

    public KarafPropertiesEditor setOutputEtc(File outputEtc) {
        this.outputEtc = outputEtc;
        return this;
    }

    public KarafPropertiesEditor setEdits(KarafPropertyEdits edits) {
        this.edits = edits;
        return this;
    }

    public void run() throws IOException {

        Map<String, List<KarafPropertyEdit>> editsByFile = new HashMap<>();

        // organize edits by file.
        for (KarafPropertyEdit edit : edits.getEdits()) {
            editsByFile.computeIfAbsent(edit.getFile(), k -> new ArrayList<>()).add(edit);
        }

        for (Map.Entry<String, List<KarafPropertyEdit>> fileOps : editsByFile.entrySet()) {
            File input = new File(inputEtc, fileOps.getKey());
            KarafPropertiesFile propsFile = new KarafPropertiesFile(input);
            propsFile.load();
            List<KarafPropertyEdit> edits = fileOps.getValue();
            for (KarafPropertyEdit edit : edits) {
                propsFile.apply(edit);
            }
            File outputFile = new File(outputEtc, fileOps.getKey());
            propsFile.store(outputFile);
        }
    }
}

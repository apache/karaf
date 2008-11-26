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
package org.apache.servicemix.kernel.gshell.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.geronimo.gshell.ansi.AnsiBuffer;
import org.apache.geronimo.gshell.ansi.AnsiCode;
import org.apache.geronimo.gshell.ansi.AnsiRenderWriter;
import org.apache.geronimo.gshell.application.model.Branding;

public class ServiceMixBranding extends Branding {

    private static final String[] BANNER = {
        " ____                  _          __  __ _      ",
        "/ ___|  ___ _ ____   _(_) ___ ___|  \\/  (_)_  __",
        "\\___ \\ / _ \\ '__\\ \\ / / |/ __/ _ \\ |\\/| | \\ \\/ /",
        " ___) |  __/ |   \\ V /| | (_|  __/ |  | | |>  < ",
        "|____/ \\___|_|    \\_/ |_|\\___\\___|_|  |_|_/_/\\_\\",
    };

    public String getName() {
        return "servicemix";
    }

    public String getDisplayName() {
        return "ServiceMix";
    }

    public String getProgramName() {
        throw new UnsupportedOperationException();
    }

    public String getAboutMessage() {
        StringWriter writer = new StringWriter();
        PrintWriter out = new AnsiRenderWriter(writer);

        out.println("For information about @|cyan ServiceMix|, visit:");
        out.println("    @|bold http://servicemix.apache.org| ");
        out.flush();

        return writer.toString();
    }

    public String getWelcomeMessage() {
        StringWriter writer = new StringWriter();
        PrintWriter out = new AnsiRenderWriter(writer);
        AnsiBuffer buff = new AnsiBuffer();

        for (String line : BANNER) {
            buff.attrib(line, AnsiCode.CYAN);
            out.println(buff);
        }

        out.println();
        // TODO: get version
        out.println(" @|bold ServiceMix| (" + "1.1.0-SNAPSHOT" + ")");
        out.println();
        out.println("Type '@|bold help|' for more information.");

        out.flush();

        return writer.toString();
    }
}

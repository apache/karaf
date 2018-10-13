/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.jersey.webapi.mocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

public abstract class MockHttpServletResponse implements HttpServletResponse {
    ByteArrayOutputStream output;
    ServletOutputStream servletoutput;
    private PrintWriter writer = new PrintWriter(output);
    private String contenttype;
    private String encoding;
    private int status;

    public ByteArrayOutputStream getOutput() {
        if (output == null) {
            output = new ByteArrayOutputStream();
        }
        return output;
    }

    public void setOutput(ByteArrayOutputStream output) {
        this.output = output;
    }

    @Override
    public String getCharacterEncoding() {
        return encoding;
    }

    @Override
    public String getContentType() {
        return contenttype;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (servletoutput == null) {
            servletoutput = new ServletOutputStream() {

                    @Override
                    public void write(int b) throws IOException {
                        getOutput().write(b);
                    }

                    @Override
                    public void setWriteListener(WriteListener writeListener) {
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }
                };
        }

        return servletoutput;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(getOutput());
        }

        return writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        encoding = charset;
    }

    @Override
    public void addHeader(String headerName, String headerValue) {
        if ("Content-Type".equals(headerName)) {
            contenttype = headerValue;
        }
    }

    @Override
    public void setContentType(String type) {
        contenttype = type;
    }

    @Override
    public void setStatus(int sc) {
        status = sc;
    }

    @Override
    public void setStatus(int sc, String sm) {
        status = sc;
    }

    @Override
    public int getStatus() {
        return status;
    }
}

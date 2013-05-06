/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.*;
import java.util.List;

@Command(scope = "shell", name = "wc", description = "Print newline, word, and byte counts for each file.")
public class WcAction extends AbstractAction {

    @Option(name = "-l", aliases = { "--lines" }, description = "Print the newline counts.", required = false, multiValued = false)
    private boolean lines;

    @Option(name = "-w", aliases = { "--words" }, description = "Print the word counts.", required = false, multiValued = false)
    private boolean words;

    @Option(name = "-m", aliases = { "--chars" }, description = "Print the character counts.", required = false, multiValued = false)
    private boolean chars;

    @Option(name = "-c", aliases = { "--bytes" }, description = "Print the byte counts.", required = false, multiValued = false)
    private boolean bytes;

    @Argument(index = 0, name = "files", description = "The list of files where to perform the count", required = false, multiValued = true)
    private List<File> files;

    @Override
    protected Object doExecute() throws Exception {
        setDefaultOptions();

        String resultString;

        if (files == null) {
            WordCounts wordCounts = getWordCounts(System.in);
            resultString = formatWordCounts(wordCounts, null);
        } else {
            resultString = getFilesWordCount(files);
        }

        System.out.println(resultString);

        return null;
    }

    protected void setDefaultOptions() {
        boolean noOptionSupplied = !(bytes || chars || lines || words);

        if (noOptionSupplied) {
            lines = true;
            words = true;
            bytes = true;
        }
    }

    /**
     * Create a combined word count for the given files.
     * If there are more than one file supplied, a total row will be added.
     *
     * @param files the files to word count.
     * @return the word count result.
     * @throws IOException in case of counting failure.
     */
    protected String getFilesWordCount(List<File> files) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        WordCounts totalWordCounts = new WordCounts();
        for (File file : files) {
            WordCounts fileWordCount = getWordCounts(new FileInputStream(file));
            String fileResultString = formatWordCounts(fileWordCount, file.getName());

            totalWordCounts = totalWordCounts.add(fileWordCount);
            stringBuilder.append(fileResultString).append('\n');

        }
        // add an additional total row
        if (files.size() > 1) {
            stringBuilder.append(formatWordCounts(totalWordCounts, "total"));
        }

        return stringBuilder.toString();
    }

    /**
     * Perform the main logic of counting the relevant data within a given input stream.
     * <p/>
     * Note, a line is considered to be terminated by linefeed '\n' or carriage return '\r'
     * a previous linefeed will be consumed.
     * <p/>
     * This method assumed UTF-8.
     *
     * @param inputStream the input stream where to count.
     * @return the word counts.
     * @throws IOException in case of counting failure.
     */
    protected WordCounts getWordCounts(InputStream inputStream) throws IOException {
        WordCounts wordCounts = new WordCounts();

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            int current;
            boolean skipNextLineFeed = false;
            boolean previousWhitespace = true;
            while ((current = bufferedReader.read()) != -1) {
                wordCounts.byteCount++;
                wordCounts.charCount++;

                // line handling
                // if a previous new line has been read, the next newline feed should be skipped
                boolean isSkipNewLineFeed = skipNextLineFeed && current == '\n';
                skipNextLineFeed = false;
                if (isSkipNewLineFeed) {
                    continue;
                }

                boolean eol = (current == '\n' || current == '\r');
                if (eol) {
                    wordCounts.lineCount++;
                    // store the state to skip the next linefeed if required
                    if (current == '\r') {
                        skipNextLineFeed = true;
                    }
                }

                // word handling
                boolean isCurrentWhitespace = Character.isWhitespace(current);
                if (!isCurrentWhitespace && previousWhitespace) {
                    wordCounts.wordCount++;
                }
                previousWhitespace = isCurrentWhitespace;
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        return wordCounts;
    }

    /**
     * Create a human readable String of the given count information.
     *
     * @param wordCounts the word counts containing the different counts data.
     * @param reportName the name associated with the word counts, ie a file name.
     * @return the human readable String.
     */
    protected String formatWordCounts(WordCounts wordCounts, String reportName) {
        StringBuilder stringBuilder = new StringBuilder();

        if (lines) {
            stringBuilder.append('\t').append(wordCounts.lineCount);
        }

        if (words) {
            stringBuilder.append('\t').append(wordCounts.wordCount);
        }

        if (chars) {
            stringBuilder.append('\t').append(wordCounts.charCount);
        }

        if (bytes) {
            stringBuilder.append('\t').append(wordCounts.byteCount);
        }

        if (reportName != null) {
            stringBuilder.append('\t').append(reportName);
        }

        return stringBuilder.toString();
    }

    /**
     * Represent a basic object which stores the relevant counts information.
     */
    protected static class WordCounts {

        protected int lineCount;
        protected int wordCount;
        protected int byteCount;
        protected int charCount;

        public WordCounts add(WordCounts append) {
            WordCounts result = new WordCounts();
            result.lineCount = lineCount + append.lineCount;
            result.wordCount = wordCount + append.wordCount;
            result.byteCount = byteCount + append.byteCount;
            result.charCount = charCount + append.charCount;
            return result;
        }

    }

}

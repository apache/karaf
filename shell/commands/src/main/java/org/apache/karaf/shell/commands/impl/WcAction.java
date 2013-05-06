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
package org.apache.karaf.shell.commands.impl;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
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
        this.setDefaultOptions();

        String outputString;

        if (files == null) {
            WordCounts wordCounts = getWordCounts(System.in);
            outputString = formatWordCounts(wordCounts, null);
        } else {
            outputString = getFilesWordCountReport(files);
        }

        System.out.println(outputString);

        return null;
    }

    /**
     * Create a combined word count report of the required files.
     * If there are more than one file supplied, a total row will be added.
     *
     * @param files the list of files.
     * @return the word count report String.
     * @throws IOException in case of a count failure.
     */
    protected String getFilesWordCountReport(List<File> files) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        WordCounts totalWordCounts = new WordCounts();
        for (File file : files) {
            WordCounts fileWordCount = getWordCounts(new FileInputStream(file));
            String fileFormattedString = formatWordCounts(fileWordCount, file.getName());
            // add it to the running total which will be outputted at the end
            totalWordCounts = totalWordCounts.add(fileWordCount);
            stringBuilder.append(fileFormattedString).append('\n');
        }
        // add additional total row
        if (files.size() > 1) {
            stringBuilder.append(formatWordCounts(totalWordCounts, "total"));
        }

        String report = stringBuilder.toString();
        return report;
    }

    /**
     * Set the default options for this action if none have been supplied.
     */
    protected void setDefaultOptions() {
        boolean noOptionsSupplied = !(bytes || chars || lines || words);

        if (noOptionsSupplied) {
            lines = true;
            words = true;
            bytes = true;
        }
    }

    /**
     * Perform the main logic of counting the relevant data within a given input stream.
     * <p/>
     * Note, a line is considered to be terminated by linefeed '\n' or carriage return '\r'.
     * A previous linefeed will be consumed.
     * <p/>
     * This method assumes UTF-8.
     *
     * @param inputStream the input stream.
     * @return the word count result.
     * @throws IOException in case of word count failure.
     */
    protected WordCounts getWordCounts(InputStream inputStream) throws IOException {
        WordCounts wordCounts = new WordCounts();

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            int current;
            boolean skipNextLineFeed = false;
            boolean previouslyWhitespace = true;
            while ((current = bufferedReader.read()) != -1) {
                wordCounts.byteCount++;
                wordCounts.charCount++;

                // line handling
                // if the previous read was a new line, skip the next newline feed
                boolean isSkipNewlineFeed = skipNextLineFeed && current == '\n';
                skipNextLineFeed = false;
                if (isSkipNewlineFeed) {
                    continue;
                }

                boolean eol = (current == '\n' || current == '\r');
                if (eol) {
                    wordCounts.lineCount++;
                    // store the state to skip the next newline feed if required
                    if (current == '\r') {
                        skipNextLineFeed = true;
                    }
                }

                // word handling
                boolean isCurrentWhitespace = Character.isWhitespace(current);
                if (!isCurrentWhitespace && previouslyWhitespace) {
                    wordCounts.wordCount++;
                }
                previouslyWhitespace = isCurrentWhitespace;
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        return wordCounts;
    }

    /**
     * Create a human readable format of the given count information.
     *
     * @param wordCounts the word count object containing the information.
     * @param reportName the name associated with the word counts, ie a file name.
     * @return a human readable String representing the word count information.
     */
    protected String formatWordCounts(WordCounts wordCounts, String reportName) {
        // line word chars
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

        String formattedString = stringBuilder.toString();

        return formattedString;
    }

    /**
     * Represent a basic object to store the word count data.
     */
    protected static class WordCounts {

        protected int lineCount;
        protected int wordCount;
        protected int byteCount;
        protected int charCount;

        public WordCounts add(WordCounts append) {
            WordCounts wordCounts = new WordCounts();
            wordCounts.charCount = charCount + append.charCount;
            wordCounts.byteCount = byteCount + append.byteCount;
            wordCounts.lineCount = lineCount + append.lineCount;
            wordCounts.wordCount = wordCount + append.wordCount;
            return wordCounts;
        }

    }

}

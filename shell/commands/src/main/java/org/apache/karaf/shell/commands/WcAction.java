package org.apache.karaf.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

import java.io.*;
import java.util.List;

/**
 * Perform a word count operation on the given input and output it
 * in a human readable format
 */
@Command(scope = "shell", name = "wc", description = "Offers the ability to count both words and lines.\n", detailedDescription = "classpath:wc.txt")
public class WcAction extends AbstractAction {

    @Option(name = "-l", aliases = {"--lines"}, description = "print the newline counts", required = false, multiValued = false)
    private boolean lines;

    @Option(name = "-w", aliases = {"--words"}, description = "print the newline counts", required = false, multiValued = false)
    private boolean words;

    @Option(name = "-m", aliases = {"--chars"}, description = "print the character counts", required = false, multiValued = false)
    private boolean chars;

    @Option(name = "-c", aliases = {"--bytes"}, description = "print the byte counts", required = false, multiValued = false)
    private boolean bytes;

    @Argument(index = 0, name = "files", description = "The list of files to perform the wc operation on", required = false, multiValued = true)
    private List<File> files;

    @Override
    protected Object doExecute() throws Exception {
        setDefaultOptions();

        String formattedString;

        if (files == null) {
            WcCounts wcCounts = getWordCounts(System.in);
            formattedString = formatWordCounts(wcCounts, null);
        } else {
            formattedString = getFilesWcReport(files);
        }

        System.out.println(formattedString);

        return null;
    }

    /**
     * Creates a combined wc report of the required files.
     * If there are more than one files supplied, a total row will be added
     *
     * @param files
     * @return
     * @throws IOException
     */
    protected String getFilesWcReport(List<File> files) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        WcCounts totalWcCounts = new WcCounts();
        for (File file : files) {
            WcCounts fileWordCount = getWordCounts(new FileInputStream(file));
            String fileFormattedString = formatWordCounts(fileWordCount, file.getName());

            // Add it to our running total which will be outputted at the end
            totalWcCounts = totalWcCounts.add(fileWordCount);
            stringBuilder.append(fileFormattedString).append('\n');
        }
        // Add our additional total row
        if (files.size() > 1) {
            stringBuilder.append(formatWordCounts(totalWcCounts, "total"));
        }

        String report = stringBuilder.toString();
        return report;
    }

    /**
     * Set the default options for this action if none have been supplied
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
     * Performs the main logic of counting the relevant data within a
     * given input stream.
     * <p/>
     * Note, a line is considered to be terminated by linefeed '\n' or
     * carriage return '\r'. a preceeding linefeed will be consumed.
     * <p/>
     * This method assumes UTF-8.
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    protected WcCounts getWordCounts(InputStream inputStream) throws IOException {
        WcCounts wcCounts = new WcCounts();

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            int current;
            boolean skipNextLineFeed = false;
            boolean previouslyWhitespace = true;
            while ((current = bufferedReader.read()) != -1) {
                wcCounts.byteCount++;
                wcCounts.charCount++;

                // Line Handling
                // If we previously read a new line, we should skip the next newlinefeed
                boolean isSkipableNewlineFeed = skipNextLineFeed && current == '\n';
                skipNextLineFeed = false;
                if (isSkipableNewlineFeed) {
                    continue;
                }

                boolean eol = (current == '\n' || current == '\r');
                if (eol) {
                    wcCounts.lineCount++;
                    // Store the state to skip the next newlinefeed if required
                    if (current == '\r') {
                        skipNextLineFeed = true;
                    }
                }

                // Word Handling
                boolean isCurrentlyWhitespace = Character.isWhitespace(current);
                if (!isCurrentlyWhitespace && previouslyWhitespace) {
                    wcCounts.wordCount++;
                }
                previouslyWhitespace = isCurrentlyWhitespace;
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        return wcCounts;
    }

    /**
     * Create a human readable format of the given count information.
     * Whether or not to output the information is dictated by the
     * flag fields within this class.
     *
     * @param wcCounts   The word count object containing the required data.
     * @param reportName The name associated with the wordcounts, ie a file name.
     * @return The human readable string representation of the word count data.
     */
    protected String formatWordCounts(WcCounts wcCounts, String reportName) {
        // line word chars
        StringBuilder stringBuilder = new StringBuilder();

        if (lines) {
            stringBuilder.append('\t').append(wcCounts.lineCount);
        }

        if (words) {
            stringBuilder.append('\t').append(wcCounts.wordCount);
        }

        if (chars) {
            stringBuilder.append('\t').append(wcCounts.charCount);
        }

        if (bytes) {
            stringBuilder.append('\t').append(wcCounts.byteCount);
        }

        if (reportName != null) {
            stringBuilder.append('\t').append(reportName);
        }

        String formattedString = stringBuilder.toString();
        return formattedString;
    }

    /**
     * Represents a basic object which stores the relevant word count data
     * This is useful so that we do not deal with arbitrary ints as method params
     */
    protected static class WcCounts {
        protected int lineCount;
        protected int wordCount;
        protected int byteCount;
        protected int charCount;

        public WcCounts add(WcCounts other) {
            WcCounts newObj = new WcCounts();
            newObj.charCount = charCount + other.charCount;
            newObj.byteCount = byteCount + other.byteCount;
            newObj.lineCount = lineCount + other.lineCount;
            newObj.wordCount = wordCount + other.wordCount;
            return newObj;
        }
    }
}

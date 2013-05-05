package org.apache.karaf.shell.commands;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class WcTest extends TestCase {
    private static final String SMALL_FILE = "WcActionTestSmall.txt";
    private static final String LARGE_FILE = "WcActionTestLarge.txt";

    public void testEmptyStringCounts() throws Exception {
        String string = "";
        WcAction.WcCounts expected = new WcAction.WcCounts();
        WcAction.WcCounts actual = getWordCounts(string);
        assertEquals(expected, actual);
    }

    public void testHelloWorldWordCounts() throws Exception {
        String string = "Hello World";
        WcAction.WcCounts expected = new WcAction.WcCounts();
        expected.wordCount = 2;
        expected.lineCount = 0;
        expected.charCount = 11;
        expected.byteCount = 11;
        WcAction.WcCounts actual = getWordCounts(string);
        assertEquals(expected, actual);
    }

    public void testLargeStringCounts() throws Exception {
        InputStream stream = WcTest.class.getResourceAsStream(LARGE_FILE);

        WcAction.WcCounts expected = new WcAction.WcCounts();
        expected.wordCount = 487;
        expected.lineCount = 34;
        expected.charCount = 3341;
        expected.byteCount = 3341;
        WcAction.WcCounts actual = getWordCounts(stream);
        assertEquals(expected, actual);
    }

    public void testOneFile() throws Exception {
        File file = new File(WcTest.class.getResource(LARGE_FILE).toURI());
        List<File> files = Arrays.asList(file);

        WcAction wcAction = new WcAction();
        wcAction.setDefaultOptions();

        String report = wcAction.getFilesWcReport(files);

        assertEquals("A single file report should not contain a total count row",
                "\t34\t487\t3341\tWcActionTestLarge.txt\n", report);
    }

    public void testThreeFiles() throws Exception {
        File smallFile = new File(WcTest.class.getResource(SMALL_FILE).toURI());
        File largeFile = new File(WcTest.class.getResource(LARGE_FILE).toURI());

        List<File> files = Arrays.asList(smallFile, largeFile, smallFile);

        WcAction wcAction = new WcAction();
        wcAction.setDefaultOptions();

        String report = wcAction.getFilesWcReport(files);

        assertEquals("A single file report should not contain a total",
                "\t11\t12\t45\tWcActionTestSmall.txt\n" +
                "\t34\t487\t3341\tWcActionTestLarge.txt\n" +
                "\t11\t12\t45\tWcActionTestSmall.txt\n" +
                "\t56\t511\t3431\ttotal"
                , report);
    }

    public WcAction.WcCounts getWordCounts(String string) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(string.getBytes());
        return getWordCounts(inputStream);
    }

    public WcAction.WcCounts getWordCounts(InputStream stream) throws Exception {
        WcAction wcAction = new WcAction();
        WcAction.WcCounts wcCounts = wcAction.getWordCounts(stream);

        return wcCounts;
    }

    public void assertEquals(WcAction.WcCounts expected, WcAction.WcCounts actual) {
        assertEquals("The expected wordCount should be equal", expected.wordCount, actual.wordCount);
        assertEquals("The expected lineCount should be equal", expected.lineCount, actual.lineCount);
        assertEquals("The expected charCount should be equal", expected.charCount, actual.charCount);
        assertEquals("The expected bytecount should be equal", expected.byteCount, actual.byteCount);
    }
}

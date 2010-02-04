package org.apache.felix.sigil.gogo.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Pattern;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.felix.sigil.junit.server.JUnitService;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;

public class SigilJunit
{
    private static final Options OPTIONS;
    
    static {
        OPTIONS = new Options();
        OPTIONS.addOption("d", "dir", true, "Directory to write ant test results to");
        OPTIONS.addOption("q", "quiet", false, "Run tests quietly, i.e. don't print steps to console" );
    }
    
    private JUnitService service;

    public void junit(String[] args) throws IOException, ParseException {
        Parser p = new GnuParser();
        CommandLine cmd = p.parse(OPTIONS, args);
        String[] cargs = cmd.getArgs();
        if ( cargs.length == 0 ) {
            for ( String t : service.getTests() ) {
                System.out.println( "\t" + t );
                System.out.flush();
            }
        }
        else {
            boolean quiet = cmd.hasOption( 'q' );
            String d = cmd.getOptionValue('d');
            File dir = null;
            if ( d != null ) {
                dir = new File(d);
                dir.mkdirs();
                System.out.println( "Writing results to " + dir.getAbsolutePath() );
                System.out.flush();
            }
            runTests( cargs, quiet, dir );
        }
    }
    
    private void runTests(String[] args, boolean quiet, File dir) throws IOException {
        int count = 0;
        int failures = 0;
        int errors = 0;
        for ( String t : args ) {
            TestSuite[] tests = findTests( t );
            if ( tests.length == 0 ) {
                System.err.println( "No tests found for " + t );
            }
            else {
                for ( TestSuite test : tests ) {
                    TestResult result = new TestResult();
                    if ( !quiet ) {
                        result.addListener( new PrintListener());
                    }
                    
                    JUnitTest antTest = null;
                    FileOutputStream fout = null;
                    XMLJUnitResultFormatter formatter = null;
                    
                    if ( dir != null ) {
                        antTest = new JUnitTest(t, false, false, true);
        
                        formatter = new XMLJUnitResultFormatter();
                        formatter.startTestSuite(antTest);
        
                        String name = "TEST-" + test.getName() + ".xml";
                        
                        File f = new File( dir, name );
                        fout = new FileOutputStream( f );
                        formatter.setOutput(fout);
                        result.addListener(formatter);
                    }
                    
                    test.run(result);
                    
                    if ( dir != null ) {
                        antTest.setCounts(result.runCount(), result.failureCount(), result.errorCount());
                        formatter.endTestSuite(antTest);
                        fout.flush();
                        fout.close();
                    }
                    count += result.runCount();
                    failures += result.failureCount();
                    errors += result.errorCount();
                }
            }
        }
        
        System.out.println( "Ran " + count + " tests. " + failures + " failures " + errors + " errors." );
        System.out.flush();
    }

    private TestSuite[] findTests(String t) {
        if ( t.contains("*" ) ) {
            Pattern p = compile(t);
            LinkedList<TestSuite> tests = new LinkedList<TestSuite>();
            for ( String n : service.getTests() ) {
                if ( p.matcher(n).matches() ) {
                    tests.add( service.createTest(n) );
                }
            }
            return tests.toArray( new TestSuite[tests.size()] );
        }
        else {
            TestSuite test = service.createTest(t);
            return test == null ? new TestSuite[0] : new TestSuite[] { test };
        }
    }

    public static final Pattern compile(String glob) {
        char[] chars = glob.toCharArray();
        if ( chars.length > 0 ) {
            StringBuilder builder = new StringBuilder(chars.length + 5);
    
            builder.append('^');
            
            for (char c : chars) {
                switch ( c ) {
                case '*':
                    builder.append(".*");
                    break;
                case '.':
                    builder.append("\\.");
                    break;
                case '$':
                    builder.append( "\\$" );
                    break;
                default:
                    builder.append( c );
                }
            }
    
            return Pattern.compile(builder.toString());
        }
        else {
            return Pattern.compile(glob);
        }
    }    
}

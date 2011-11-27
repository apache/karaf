package org.apache.karaf.tooling.exam.regression;

import static junit.framework.Assert.assertTrue;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.CoreOptions.maven;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class Backward224Test {

    @Configuration
    public Option[] config() {
        return new Option[]{ karafDistributionConfiguration().frameworkUrl(
            maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("zip").version("2.2.4"))
            .karafVersion("2.2.4").name("Apache Karaf") };
    }

    @Test
    public void test() throws Exception {
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        System.out.println("===========================================");
        assertTrue(true);
    }

    @Test
    public void test2() throws Exception {
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXxx");
        assertTrue(true);
    }
}

package org.apache.karaf.util;

import junit.framework.TestCase;

import org.junit.Test;

import static org.apache.karaf.util.DeployerUtils.extractNameVersionType;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


public class DeployerUtilsTest extends TestCase
{
    @Test
    public void test()
    {
        assertThat(
            extractNameVersionType("foobarbaz-1.jar"),
            equalTo(new String[]{"foobarbaz", "1", "jar"})
        );

        assertThat(
            extractNameVersionType("foobarbaz-1.0.jar"),
            equalTo(new String[]{"foobarbaz", "1.0", "jar"})
        );

        assertThat(
            extractNameVersionType("foobarbaz-1.0.0.jar"),
            equalTo(new String[]{"foobarbaz", "1.0.0", "jar"})
        );

        assertThat(
            extractNameVersionType("foobarbaz-1-PR-4-SNAPSHOT.jar"),
            equalTo(new String[]{"foobarbaz", "1.0.0.PR-4-SNAPSHOT", "jar"})
        );

        assertThat(
            extractNameVersionType("foobarbaz-1.0-PR-4-SNAPSHOT.jar"),
            equalTo(new String[]{"foobarbaz", "1.0.0.PR-4-SNAPSHOT", "jar"})
        );

        assertThat(
            extractNameVersionType("foobarbaz-1.0.0-PR-4-SNAPSHOT.jar"),
            equalTo(new String[]{"foobarbaz", "1.0.0.PR-4-SNAPSHOT", "jar"})
        );
    }
}
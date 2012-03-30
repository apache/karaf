package org.apache.karaf.packages.core;

import junit.framework.Assert;

import org.junit.Test;

public class PackageRequirementTest {
    @Test
    public void testGetPackageName() {
        PackageRequirement req = new PackageRequirement("(&(osgi.wiring.package=org.osgi.service.useradmin)(version>=1.1.0))", false, null, false);
        String packageName = req.getPackageName();
        Assert.assertEquals("org.osgi.service.useradmin", packageName);
    }
}

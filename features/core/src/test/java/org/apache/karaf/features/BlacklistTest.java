package org.apache.karaf.features;

import java.util.Arrays;
import java.util.List;

import org.apache.karaf.features.internal.service.Blacklist;

import junit.framework.TestCase;

public class BlacklistTest extends TestCase {

    public void testRepositoryBlacklist() throws Exception {
        List<String> list = Arrays.asList(new String[] {
                "mvn:org.apache.karaf.features1/framework/1.0.0/xml/features;type=repository",
                "mvn:org.apache.karaf.features1/framework/2.0.0/xml/features;type=repository",
                "mvn:org.apache.karaf.features2/framework/1.0.0/xml/features;type=repository",
                "mvn:org.apache.karaf.features2/framework/3.0.0/xml/features;type=repository"
        });
        
        Blacklist blacklist = new Blacklist(list);
        
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/1.0.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/2.0.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features2/framework/1.0.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features2/framework/3.0.0/xml/features"));

        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features2/framework/2.0.0/xml/features"));

    }

    public void testRepositoryBlacklistAlias() throws Exception {
        List<String> list = Arrays.asList(new String[] {
                "mvn:org.apache.karaf.features1/framework/[1.0.0,3.0.0)/xml/features;type=repository",
                "mvn:org.apache.karaf.features2/*/1.0.0/xml/features;type=repository",
                "mvn:org.apache.test.*/framework/[0.0.0,*)/xml/features;type=repository",
                "mvn:org.apache.karaf.features3/framework/3.0.0/*/features;type=repository",
                "mvn:org.apache.karaf.features4/framework/3.0.0/xml/*;type=repository"
        });
        
        Blacklist blacklist = new Blacklist(list);

        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/1.0.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/1.1.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/2.0.0/xml/features"));
        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/3.0.0/xml/features"));
        
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features2/something/1.0.0/xml/features"));
        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features2/something/1.1.0/xml/features"));
        
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.test.features/framework/1.0.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.test.features/framework/1.1.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.test.features/framework/11.0.0/xml/features"));
        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.test.features/framework/11.0.0/xml/feature"));
        
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features3/framework/3.0.0/txt/features"));
        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features3/framework/3.1.0/txt/features"));

        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features4/framework/3.0.0/xml/feature"));
        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features4/framework/3.1.0/xml/feature"));
        
    }

    public void testRepositoryWhitelist() throws Exception {
        List<String> list = Arrays.asList(new String[] {
                "mvn:org.apache.karaf.features1/framework/[1.0.0,3.0.0)/xml/features;type=notRepository",
                "mvn:org.apache.karaf.features1/framework/[3.0.0,*)/xml/features;type=repository",
        });
        
        Blacklist blacklist = new Blacklist(list);

        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/1.0.0/xml/features"));
        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/1.0.0/xml/features"));
        assertFalse(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/1.0.0/xml/features"));
        assertTrue(blacklist.isRepositoryBlacklisted("mvn:org.apache.karaf.features1/framework/4.0.0/xml/features"));
        
    }
    
}

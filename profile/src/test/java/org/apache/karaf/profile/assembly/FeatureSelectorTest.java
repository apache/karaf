package org.apache.karaf.profile.assembly;

import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;

public class FeatureSelectorTest {
  private FeatureSelector featureSelector;
  private Feature conditionalFeatureDep;

  @Before
  public void prepareEnv() throws Exception{
    RepositoryImpl repo = new RepositoryImpl(getClass().getClassLoader().getResource("features/f1.xml").toURI());
    Feature rootFeature = (Feature) repo.getFeatures()[0];
    Feature fb = (Feature) repo.getFeatures()[1];
    Feature fc = (Feature) repo.getFeatures()[2];
    conditionalFeatureDep = fc;

    Set<Feature> features = new HashSet<>();
    features.add( rootFeature );
    features.add( fb );
    features.add( fc );
    featureSelector = new FeatureSelector( features );
  }

  @Test
  public void testConditionalFeatures() throws Exception{
    Set<Feature> result = featureSelector.getMatching( Collections.singletonList("fa") );

    if(result.contains( conditionalFeatureDep )) {
      assertTrue("Found the correct dependency reference by conditional feature", true);
    } else {
      assertTrue( "Not found the correct dependency reference by conditional feature", false );
    }
  }
}

package org.apache.karaf.features.internal.download.impl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import static junit.framework.TestCase.assertEquals;

public class DownloadManagerHelperTest {

  @Test
  public void testSetExtraProtocols(){
    assertEquals("^(jar|war|war-i|warref|webbundle|wrap|spring|blueprint):.*$", DownloadManagerHelper.getIgnoredProtocolPattern().toString());

    List<String> extraProtocols = new ArrayList<>();
    extraProtocols.add( "extra1" );
    extraProtocols.add( "extra2" );
    DownloadManagerHelper.setExtraProtocols( extraProtocols );

    assertEquals("^(jar|war|war-i|warref|webbundle|wrap|spring|blueprint|extra1|extra2):.*$", DownloadManagerHelper.getIgnoredProtocolPattern().toString());
  }
}

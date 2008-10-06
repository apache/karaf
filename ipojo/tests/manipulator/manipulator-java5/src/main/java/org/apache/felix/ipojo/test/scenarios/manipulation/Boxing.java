package org.apache.felix.ipojo.test.scenarios.manipulation;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.manipulation.service.PrimitiveManipulationTestService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class Boxing extends OSGiTestCase {
    
    ComponentInstance instance; // Instance under test

    PrimitiveManipulationTestService prim;

    ServiceReference prim_ref;

    public void setUp() {
        Properties p1 = new Properties();
        p1.put("instance.name","primitives");
        instance = Utils.getComponentInstance(context, "ManipulationPrimitives5-PrimitiveManipulationTester", p1);
        assertTrue("check instance state", instance.getState() == ComponentInstance.VALID);
        prim_ref = Utils.getServiceReferenceByName(context, PrimitiveManipulationTestService.class.getName(), instance.getInstanceName());
        assertNotNull("Check prim availability", prim_ref);
        prim = (PrimitiveManipulationTestService) context.getService(prim_ref);
    }

    public void tearDown() {
        context.ungetService(prim_ref);
        prim = null;
        instance.dispose();
        instance = null;
    }
    
  public void testLongFromObject() {
      assertEquals("Check - 1", prim.getLong(), 1);
      Long l = new Long(2);
      prim.setLong(l);
      assertEquals("Check - 2", prim.getLong(), 2);
  }

  public void testLongFromObject2() {
      assertEquals("Check - 1", prim.getLong(), 1);
      Long l = new Long(2);
      prim.setLong(l, "ss");
      assertEquals("Check - 2", prim.getLong(), 2);
  }

}

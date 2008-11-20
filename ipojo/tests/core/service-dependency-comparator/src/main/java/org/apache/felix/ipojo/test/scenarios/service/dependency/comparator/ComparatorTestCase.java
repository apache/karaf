package org.apache.felix.ipojo.test.scenarios.service.dependency.comparator;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.osgi.framework.ServiceReference;

public class ComparatorTestCase extends OSGiTestCase {
    
    String gradeFactory="COMPARATOR-gradedFooProvider";
    String dynamic = "COMPARATOR-DynamicCheckService";
    String dynamicpriority = "COMPARATOR-DynamicPriorityCheckService";
    
    
    IPOJOHelper helper;
    ComponentInstance dynInstance;
    ComponentInstance dpInstance;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
        dynInstance = helper.createComponentInstance(dynamic, (Properties) null);
        dpInstance = helper.createComponentInstance(dynamicpriority, (Properties) null);
    }
    
    public void tearDown() {
       dynInstance.dispose();
       dpInstance.dispose();
    }
    
    public void testDynamic() {
        ComponentInstance grade1 = createGrade(1);
        ComponentInstance grade2 = createGrade(2);
        
        ServiceReference ref = getServiceReferenceByName(CheckService.class.getName(), dynInstance.getInstanceName());
        assertNotNull("CS availability", ref);
        
        CheckService cs = (CheckService) context.getService(ref);
        Properties result = cs.getProps();
        int fsGrade = ((Integer) result.get("fs")).intValue();
        int fs2Grade = ((Integer) result.get("fs2")).intValue();
        int[] fssGrades = (int[]) result.get("fss");
        
        assertEquals("fs grade -1", 2, fsGrade);
        assertEquals("fs2 grade -1", 2, fs2Grade);
        assertEquals("fss grade size -1", 2, fssGrades.length);
        

        assertEquals("fss grade[0] -1", 2, fssGrades[0]);
        assertEquals("fss grade[1] -1", 1, fssGrades[1]);
        
        ComponentInstance grade3 = createGrade(3);
        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");
        
        assertEquals("fs grade -2", 2, fsGrade);
        assertEquals("fs2 grade -2", 2, fs2Grade);
        assertEquals("fss grade size -2", 3, fssGrades.length);
        assertEquals("fss grade[0] -2", 2, fssGrades[0]);
        assertEquals("fss grade[1] -2", 1, fssGrades[1]);
        assertEquals("fss grade[2] -2", 3, fssGrades[2]);

        grade2.stop();
        
        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");
        
        assertEquals("fs grade -3", 3, fsGrade);
        assertEquals("fs2 grade -3", 3, fs2Grade);
        assertEquals("fss grade size -3", 2, fssGrades.length);
        assertEquals("fss grade[0] -3", 1, fssGrades[0]);
        assertEquals("fss grade[1] -3", 3, fssGrades[1]);        
        
        context.ungetService(ref);
        grade1.dispose();
        grade2.dispose();
        grade3.dispose();
    }
    
    public void testDynamicPriority() {
        ComponentInstance grade1 = createGrade(1);
        ComponentInstance grade2 = createGrade(2);
        
        ServiceReference ref = getServiceReferenceByName(CheckService.class.getName(), dpInstance.getInstanceName());
        assertNotNull("CS availability", ref);
        
        CheckService cs = (CheckService) context.getService(ref);
        Properties result = cs.getProps();
        int fsGrade = ((Integer) result.get("fs")).intValue();
        int fs2Grade = ((Integer) result.get("fs2")).intValue();
        int[] fssGrades = (int[]) result.get("fss");
        
        assertEquals("fs grade -1", 2, fsGrade);
        assertEquals("fs2 grade -1", 2, fs2Grade);
        assertEquals("fss grade size -1", 2, fssGrades.length);
        assertEquals("fss grade[0] -1", 2, fssGrades[0]);
        assertEquals("fss grade[1] -1", 1, fssGrades[1]);
        
        ComponentInstance grade3 = createGrade(3);
        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");
        
        assertEquals("fs grade -2", 3, fsGrade);
        assertEquals("fs2 grade -2", 3, fs2Grade);
        assertEquals("fss grade size -2", 3, fssGrades.length);
        assertEquals("fss grade[0] -2", 3, fssGrades[0]);
        assertEquals("fss grade[1] -2", 2, fssGrades[1]);
        assertEquals("fss grade[2] -2", 1, fssGrades[2]);

        grade2.stop();
        
        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");
        
        assertEquals("fs grade -3", 3, fsGrade);
        assertEquals("fs2 grade -3", 3, fs2Grade);
        assertEquals("fss grade size -3", 2, fssGrades.length);
        assertEquals("fss grade[0] -3", 3, fssGrades[0]);
        assertEquals("fss grade[1] -3", 1, fssGrades[1]);        
        
        context.ungetService(ref);
        grade1.dispose();
        grade2.dispose();
        grade3.dispose();
    }
    
    private ComponentInstance createGrade(int grade) {
        Properties props = new Properties();
        props.put("grade", new Integer(grade));
        return helper.createComponentInstance(gradeFactory, props);
    }

}

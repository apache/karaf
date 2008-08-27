package org.apache.felix.ipojo.test.scenarios.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.test.scenarios.manipulation.service.CheckService;

public class InnerClasses implements CheckService {
    
    private String privateObject;
    private int privateInt;
    
    protected String protectedObject;
    protected int protectedInt;
    
    String packageObject;
    int packageInt;
    
    public String publicObject;
    public int publicInt;
    
    private String nonObject = "not-managed";
    private int nonInt = 5;
    
    private static int staticint = 6;

    public boolean check() {
        return true;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("publicInner", new PublicNested().doSomething());
        props.put("packageInner", new PackageNested().doSomething());
        props.put("protectedInner", new ProtectedNested().doSomething());
        props.put("privateInner", new PrivateNested().doSomething());
        props.put("constructorInner", new ConstructorNested().doSomething());
        props.put("staticInner", new StaticNested().doSomething());
        
        Job anonymous = new Job()  {
            public Map doSomething() {
                Map map = new HashMap();
                map.put("publicObject", publicObject);
                map.put("publicInt", new Integer(publicInt));
                map.put("packageObject", packageObject);
                map.put("packageInt", new Integer(packageInt));
                map.put("protectedObject", protectedObject);
                map.put("protectedInt", new Integer(protectedInt));
                map.put("privateObject", privateObject);
                map.put("privateInt", new Integer(privateInt));
                map.put("nonObject", nonObject);
                map.put("nonInt", new Integer(nonInt));
                return map;
            }
        };
        
        props.put("anonymous", anonymous.doSomething());
        props.put("public", new PublicNested());
        
        
        return props;
    }
    
    private class PrivateNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    public class PublicNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    class PackageNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    protected class ProtectedNested implements Job {
        public Map doSomething() {
            Map map = new HashMap();
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
            return map;
        }
    }
    
    protected static class StaticNested implements Job {
        private Map map = new HashMap();
        
        public Map doSomething() {
            map.put("static", new Boolean(true));
            map.put("staticint", new Integer(staticint));
            return map;
        }
    }
    
    protected class ConstructorNested implements Job {
        Map map = new HashMap();
        public ConstructorNested() {
            map.put("publicObject", publicObject);
            map.put("publicInt", new Integer(publicInt));
            map.put("packageObject", packageObject);
            map.put("packageInt", new Integer(packageInt));
            map.put("protectedObject", protectedObject);
            map.put("protectedInt", new Integer(protectedInt));
            map.put("privateObject", privateObject);
            map.put("privateInt", new Integer(privateInt));
            map.put("nonObject", nonObject);
            map.put("nonInt", new Integer(nonInt));
        }
        
        public Map doSomething() {
            return map;
        }
    }
    

}

interface Job {
    public Map doSomething();
}

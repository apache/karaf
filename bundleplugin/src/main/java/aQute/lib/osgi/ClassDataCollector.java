package aQute.lib.osgi;

public interface ClassDataCollector {
    void classBegin(int access, String name);
    void extendsClass(String name);
    void implementsInterfaces(String name[]);
    void field(int access, String descriptor);
    void method(int access, String descriptor);
    void addReference(String token);
    void classEnd();
}

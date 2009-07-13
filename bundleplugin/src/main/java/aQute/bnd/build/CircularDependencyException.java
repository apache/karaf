package aQute.bnd.build;

public class CircularDependencyException extends Exception {
    public CircularDependencyException(String string) {
        super(string);
    }

    private static final long serialVersionUID = 1L;

}

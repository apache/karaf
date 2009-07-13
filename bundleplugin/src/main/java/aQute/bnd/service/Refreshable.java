package aQute.bnd.service;

import java.io.*;

public interface Refreshable {
    boolean refresh();
    File getRoot();
}

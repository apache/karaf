package aQute.bnd.service.action;

import aQute.bnd.build.*;

public interface Action {
    /**
     * A String[] that specifies a menu entry. The entry
     * can be hierarchical by separating the parts with a ':'.
     * 
     * <pre>
     *  A:B:C
     *  A:B:D
     *  
     *      A
     *      |
     *      B
     *     / \
     *    C   D
     *    
     * </pre>
     */
    String ACTION_MENU  = "bnd.action.menu";

    void execute( Project project, String action) throws Exception;
}

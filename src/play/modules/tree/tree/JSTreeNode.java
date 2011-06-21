package tree;

import java.util.List;

/**
 * Representation of a jsTree node.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface JSTreeNode extends Comparable<JSTreeNode> {

    String getName();
    Long getId();
    boolean isContainer();
    boolean isOpen();
    String getType();
    List<JSTreeNode> getChildren();
}

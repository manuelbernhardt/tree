package models.tree;

/**
 * This interface represents a Node that is associated with a {@link GenericTreeNode}.
 * A class that needs to be a node in a persistent tree should implement this interface.
 * Note that this class needs to be cloneable, so make sure to override the {@link #clone()} method if necessary.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface Node extends Cloneable {

    /**
     * Setter for the name (when a node is created in the tree)
     *
     * @param name
     */
    void setName(String name);

    Object clone() throws CloneNotSupportedException;

}

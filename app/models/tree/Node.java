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
     * Unique identifier for the Node, for example the primary key if the Node is also stored in a database
     *
     * @return a unique Long identifier.
     */
    Long getId();

    /**
     * Setter for the name (when a node is created in the tree). Implement this if you want to store the name also in the associated object.
     *
     * @param name the name of the newly created instance
     */
    void setName(String name);

    Object clone() throws CloneNotSupportedException;

}

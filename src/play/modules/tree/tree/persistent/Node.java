package tree.persistent;

/**
 * This interface represents a tree.persistent.Node that is associated with a {@link tree.persistent.GenericTreeNode}.
 * A class that needs to be a node in a persistent tree should implement this interface.
 * Note that this class needs to be cloneable, so make sure to override the {@link #clone()} method if necessary.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface Node extends Cloneable {

    /**
     * Unique identifier for the tree.persistent.Node, for example the primary key if the tree.persistent.Node is also stored in a database
     *
     * @return a unique Long identifier.
     */
    Long getId();

    Object clone() throws CloneNotSupportedException;

}

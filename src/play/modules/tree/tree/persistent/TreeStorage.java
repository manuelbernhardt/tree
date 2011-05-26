package tree.persistent;

import java.util.List;

import tree.JSTreeNode;

/**
 * Abstract storage for trees, allowing the use of more storage engines in order to store trees.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public abstract class TreeStorage {

    /**
     * Persists a Node object
     *
     * @param node the attached {@link Node}
     * @return the persisted {@link Node} instance
     */
    public abstract Node persistObject(Node node);

    /**
     * Updates a Node object
     *
     * @param node the attached {@link Node}
     * @return the updated {@link Node} instance
     */
    public abstract Node updateObject(Node node);

    /**
     * Returns a new instance of a tree node
     *
     * @return a new instance of a {@link GenericTreeNode}
     */
    public abstract GenericTreeNode getNewTreeNode();

    /**
     * Persists a new tree node
     *
     * @return the persisted {@link GenericTreeNode} instance
     */
    public abstract GenericTreeNode persistTreeNode(GenericTreeNode node);

    /**
     * Checks if a node with the same definition already exists in the storage
     * @param node the node to check the existance for
     * @return <code>true</code> if a node with the same definition is already stored
     */
    public abstract boolean exists(GenericTreeNode node);

    /**
     * Updates a tree node
     *
     * @return the updated {@link GenericTreeNode} instance
     */
    public abstract GenericTreeNode updateTreeNode(GenericTreeNode node);

    /**
     * Retrieves a tree node
     *
     * @param nodeId the ID of the attached {@link Node}
     * @param type   the type of the attached {@link Node}
     * @param treeId the identifier of the tree this TreeNode belongs to
     * @return a {@link GenericTreeNode}, or <code>null</code> if no node with that ID was found
     */
    public abstract GenericTreeNode getTreeNode(Long nodeId, String type, String treeId);

    public abstract List<JSTreeNode> getChildren(Long parentId, String treeId, String type);

    public abstract boolean remove(Long id, boolean removeObject, String treeId, String type);

    public abstract boolean rename(Long id, String name, String treeId, String type);

    public abstract boolean move(Long id, String type, Long target, String targetType, String treeId);

    public abstract boolean copy(Long id, Long target, boolean copyObject, NodeType[] objectTypes, String treeId);

    /**
     * Renames all the GenericTreeNode-s for a given {@link Node} identifier and type
     * @param name the new name
     * @param type the type of the {@link tree.persistent.Node}
     * @param nodeId the ID of the {@link tree.persistent.Node}
     * @param treeId the ID of the tree
     */
    public abstract void renameTreeNodes(String name, String type, Long nodeId, String treeId);

    /**
     * Compues the path of a TreeNode. The rules for creating the path need to be the same everywhere.
     *
     * @param parent the parent of the node
     * @param node the node
     * @return a path composed of the concatenation of parent path and id
     */
    public String computePath(GenericTreeNode parent, GenericTreeNode node, String name) {
        String path = null;

        // if it's not a null parent and if it's not a thread root
        if (parent != null && !parent.equals(node)) {
            path = parent.getPath() + "/";
        } else {
            path = "/";
        }
        path += name;
        return path;
    }
}

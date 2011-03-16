package controllers.tree;

import java.util.List;

import models.tree.GenericTreeNode;
import models.tree.JSTreeNode;
import models.tree.Node;

/**
 * Abstract storage for trees, allowing the use of more storage engines in order to store trees.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public abstract class TreeStorage {


    public abstract Node createObject(Node node);

    public abstract Node updateObject(Node node);


    public abstract GenericTreeNode getNewTreeNode();

    public abstract GenericTreeNode createTreeNode(GenericTreeNode node);

    public abstract GenericTreeNode updateTreeNode(GenericTreeNode node);

    public abstract GenericTreeNode getTreeNode(Long id);

    public abstract List<JSTreeNode> getChildren(Long parentId);


    public abstract void remove(Long id, boolean removeObject);

    public abstract void rename(Long id, String name);

    public abstract void move(Long id, Long target);

    public abstract void copy(Long id, Long target, boolean copyObject, NodeType[] objectTypes);

    /**
     * Compues the path of a TreeNode. The rules for creating the path need to be the same everywhere.
     *
     * @param parent the parent of the node
     * @param id     the id of the node
     * @return a path composed of the concatenation of parent path and id
     */
    public String computePath(GenericTreeNode parent, Long id) {
        String path = "";

        // if it's not a null parent and if it's not a thread root
        if (parent != null && parent.getId() != id) {
            path += parent.getPath();
            path += "___";
        }
        path += id;
        return path;
    }


}

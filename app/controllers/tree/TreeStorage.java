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

    public abstract Node create(Node node);

    public abstract Node update(Node node);

    public abstract GenericTreeNode getNewGenericTreeNode();

    public abstract GenericTreeNode create(GenericTreeNode node);

    public abstract GenericTreeNode update(GenericTreeNode node);

    public abstract GenericTreeNode getTreeNode(Long id);

    public abstract List<JSTreeNode> getChildren(Long parentId);

    public abstract void remove(Long id, boolean removeObject);

    public abstract void rename(Long id, String name);

    public abstract void move(Long id, Long target);

    public abstract void copy(Long id, Long target, boolean copyObject, NodeType[] objectTypes);

    /**
     * the rules for creating the path should be the same as in the TreeStorage *
     */
    public String computePath(GenericTreeNode parent, Long id, String name) {
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

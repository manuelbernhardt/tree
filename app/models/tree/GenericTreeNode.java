package models.tree;

import controllers.tree.NodeType;

/**
 * A generic node in a persistent tree. Implementations of this interface need to also have an equivalent {@link controllers.tree.TreeStorage} implementation
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface GenericTreeNode extends JSTreeNode {

    Long getId();

    String getTreeId();

    void setTreeId(String treeId);

    String getName();

    void setName(String name);

    NodeType getNodeType();

    void setNodeType(NodeType type);

    Integer getLevel();

    void setLevel(Integer level);

    GenericTreeNode getThreadRoot();

    void setThreadRoot(GenericTreeNode node);

    Node getNode();

    void setNode(Node n);

    boolean isOpen();

    void setOpen(boolean open);

    String getPath();

    void setPath(String path);

    GenericTreeNode getParent();

}

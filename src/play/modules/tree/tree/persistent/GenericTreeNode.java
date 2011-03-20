package tree.persistent;

import tree.JSTreeNode;

/**
 * A generic node in a persistent tree. Implementations of this interface need to also have an equivalent {@link TreeStorage} implementation
 *
 * TODO documentation
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface GenericTreeNode extends JSTreeNode {

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

    Long getNodeId();
    void setNodeId(Long id);

    boolean isOpen();
    void setOpen(boolean open);

    String getPath();
    void setPath(String path);

    GenericTreeNode getParent();
}

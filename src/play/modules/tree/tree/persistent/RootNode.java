package tree.persistent;

import java.util.List;

import tree.JSTreeNode;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class RootNode implements JSTreeNode {

    private String name;
    private Long id;
    private boolean container;
    private boolean open;
    private String type;
    private List<JSTreeNode> children;
    private String treeId;

    public RootNode(String name, String treeId, Long id, boolean container, boolean open, String type, List<JSTreeNode> children) {
        this.name = name;
        this.id = id;
        this.container = container;
        this.open = open;
        this.type = type;
        this.children = children;
        this.treeId = treeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isContainer() {
        return container;
    }

    public void setContainer(boolean container) {
        this.container = container;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<JSTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<JSTreeNode> children) {
        this.children = children;
    }

    public int compareTo(JSTreeNode o) {
        return this.getName().compareTo(o.getName());
    }

    public String getTreeId() {
        return treeId;
    }
}

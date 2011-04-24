package tree.persistent;

/**
 * Type of a node, to be used when implementing an {@link AbstractTree}
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class NodeType {

    private String name;

    private AbstractTree tree;

    private boolean container;

    private Class<? extends Node> nodeClass;

    public String getName() {
        return name;
    }

    public Class<? extends Node> getNodeClass() {
        return nodeClass;
    }

    public boolean isContainer() {
        return container;
    }

    public AbstractTree getTree() {
        return tree;
    }

    public NodeType(String name, boolean isContainer, Class<? extends Node> nodeClass, AbstractTree tree) {
        this.name = name;
        this.tree = tree;
        this.container = isContainer;
        this.nodeClass = nodeClass;
    }
}

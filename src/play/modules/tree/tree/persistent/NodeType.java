package tree.persistent;

/**
 * Type of a node, to be used when implementing an {@link AbstractTree}
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class NodeType {

    private String name;

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

    public NodeType(String name, boolean isContainer, Class<? extends Node> nodeClass) {
        this.name = name;
        this.container = isContainer;
        this.nodeClass = nodeClass;
    }
}

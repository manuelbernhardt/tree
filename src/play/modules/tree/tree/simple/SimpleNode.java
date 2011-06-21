package tree.simple;

import java.util.List;

import tree.JSTreeNode;

/**
 * In-memory implementation of a JSTreeNode, for use with simple trees.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class SimpleNode implements JSTreeNode, Comparable<JSTreeNode> {
    private String type;
    private String name;
    private boolean open;
    private Long id;
    private boolean container;
    private ChildProducer producer;

    public SimpleNode(Long id, String name, String type, boolean open, boolean container, ChildProducer producer) {
        this.type = type;
        this.name = name;
        this.open = open;
        this.id = id;
        this.container = container;
        this.producer = producer;
    }

    public String getName() {
        return name;
    }

    public Long getId() {
        return id;
    }

    public boolean isContainer() {
        return container;
    }

    public boolean isOpen() {
        return open;
    }

    public String getType() {
        return type;
    }

    public List<JSTreeNode> getChildren() {
        if(producer == null) {
            throw new RuntimeException("No producer set");
        }
        return producer.produce(this.id);
    }

    public int compareTo(JSTreeNode other) {
        return this.getName().compareTo(other.getName());
    }
}

package controllers.tree;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.tree.GenericTreeNode;
import models.tree.JSTreeNode;
import models.tree.Node;

/**
 * Base class for persistent trees. It is meant to offer support for more than one storage engine (for now, only JPA is supported).
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public abstract class AbstractTree implements TreeDataHandler {

    protected static Map<String, NodeType> nodeTypes = new HashMap<String, NodeType>();
    protected static Map<Class, NodeType> nodeTypesByClass = new HashMap<Class, NodeType>();

    /**
     * Registers a new {@link NodeType}. This method should be used when implementing {@link #getNodes()}
     *
     * @param nodeClass   the Class of the tree node (must implement GenericTreeNode or extend a convenience class such as {@link models.tree.jpa.AbstractNode})
     * @param isContainer whether this node can be a container of other nodes (i.e. a non-leaf node)
     * @return a registered {@link NodeType}
     */
    public static NodeType type(Class<? extends Node> nodeClass, boolean isContainer) {
        String name = nodeClass.getSimpleName().substring(0, 1).toLowerCase() + nodeClass.getSimpleName().substring(1);
        NodeType nodeType = new NodeType(name, isContainer, nodeClass);
        nodeTypes.put(name, nodeType);
        nodeTypesByClass.put(nodeClass, nodeType);
        return nodeType;
    }

    public static NodeType getNodeType(String name) {
        return nodeTypes.get(name);
    }

    protected static NodeType getNodeType(Class type) {
        return nodeTypesByClass.get(type);
    }

    protected enum StorageType {JPA}

    protected StorageType getStorageType() {
        return StorageType.JPA;
    }

    void init() {
        if (getStorageType() == StorageType.JPA) {
            storage = new JPATreeStorage();
        } else {
            throw new RuntimeException("Unknown storage type " + getStorageType());
        }

        // initialize node types
        getRootType();
        getNodes();
    }

    private TreeStorage storage = null;

    /**
     * The qualifier for this tree
     *
     * @return a qualifier for the tree, unique for all the application.
     */
    public abstract String getName();

    /**
     * Returns all the possible node types for this tree.<br>
     * Register a type with {@link #type(Class, boolean)}
     *
     * @return an Array of {@link NodeType}
     */
    protected abstract NodeType[] getNodes();

    /**
     * The default type of nodes (returned by jsTree when a default node is created)
     * @return the {@link NodeType} of the default node to create
     */
    protected abstract NodeType getDefaultType();

    /**
     * The type of the parent of all nodes in the tree, i.e. which type is at the root of the tree.
     * For the moment we don't support mixed root nodes
     *
     * @return the {@link NodeType} of the root type
     */
    protected abstract NodeType getRootType();

    /**
     * Whether or not deletion of a node in the tree leads to the deletion of the attached object
     *
     * @return <code>true</code> by default
     */
    protected boolean isRemovalPropagated() {
        return true;
    }

    /**
     * Whether a freshly created node is open by default
     *
     * @return <code>true</code> by default
     */
    protected boolean isCreatedNodeOpen() {
        return true;
    }

    /**
     * Whether or not copying a node or branch leads to the creation of copies of the attached object
     *
     * @return <code>true</code> by default
     */
    protected boolean isObjectCopyPropagated() {
        return true;
    }

    public GenericTreeNode getNode(Long id) {
        return storage.getTreeNode(id, getName());
    }

    public List<? extends JSTreeNode> getChildren(Long parentId, Map<String, String> args) {
        return storage.getChildren(parentId, getName());
    }

    public Long create(Long parentId, Long position, String name, String type, Map<String, String> args) {
        NodeType nt = null;
        if (type == null) {
            nt = getRootType();
        } else if(type.equals("default")) {
            nt = getDefaultType();
        } else {
            nt = getNodeType(type);
        }
        if (nt == null) {
            throw new RuntimeException("Could not find a registered NodeType for type '" + type + "'");
        }

        try {
            GenericTreeNode node = storage.getNewTreeNode();
            populateTreeNode(node, parentId, name, nt);
            node = storage.createTreeNode(node);

            Node object = createObjectNode(name, nt, args);
            if(object == null) {
                throw new RuntimeException(String.format("New instance for node '%s' of type '%s' is null", name, nt.getName()));
            }
            object = storage.createObject(object);

            node.setNodeId(object.getId());

            // compute only when we have an ID
            node.setPath(storage.computePath(storage.getTreeNode(parentId, getName()), node.getId()));
            node = storage.updateTreeNode(node);

            return node.getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean rename(Long id, String name, String type) {
        // TODO return false if error
        storage.rename(id, name, getName());
        return true;
    }

    public void copy(Long id, Long target, Long position) {
        storage.copy(id, target, true, getNodes(), getName());
    }

    public void move(Long id, Long target, Long position) {
        storage.move(id, target, getName());
    }

    public boolean remove(Long id, Long parentId, String type, Map<String, String> args) {
        // TODO return false if error
        storage.remove(id, isRemovalPropagated(), getName());
        return true;
    }


    /**
     * Creates a new instance of a {@link Node}. By default, this method uses reflection. Override it you need to do more complicated things.
     *
     * @param name the name of the node to create
     * @param type the {@link NodeType} of the node to create
     * @param args a map of arguments provided via the interface
     * @return a new instance of a {@link Node}
     */
    protected Node createObjectNode(String name, NodeType type, Map<String, String> args) {
        try {
            Constructor c = type.getNodeClass().getDeclaredConstructor();
            Node object = (Node) c.newInstance();
            object.setName(name);
            return object;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void populateTreeNode(GenericTreeNode n, Long parentId, String name, NodeType type) {
        GenericTreeNode parent = getNode(parentId);
        if (parent == null && parentId != -1) {
            throw new RuntimeException("Could not find parent node with ID " + parentId);
        }
        if (parent == null) {
            n.setLevel(0);
            n.setThreadRoot(n);
        } else {
            n.setLevel(parent.getLevel() + 1);
            n.setThreadRoot(parent.getThreadRoot());
        }
        n.setTreeId(this.getName());
        n.setName(name);
        n.setNodeType(type);

        n.setOpen(isCreatedNodeOpen());
    }
}
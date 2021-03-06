package tree.persistent;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Play;
import play.libs.F;
import tree.JSTreeNode;
import tree.TreeDataHandler;
import tree.TreePlugin;

/**
 * Base class for persistent trees. It is meant to offer support for more than one storage engine (for now, only JPA is supported).
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public abstract class AbstractTree implements TreeDataHandler {

    protected static Map<String, NodeType> nodeTypes = new HashMap<String, NodeType>();
    protected static Map<Class, NodeType> nodeTypesByClass = new HashMap<Class, NodeType>();

    private TreeStorage storage;

    /**
     * Registers a new {@link NodeType}. This method should be used when implementing {@link #getNodes()}
     *
     * @param nodeClass   the Class of the tree node (must implement GenericTreeNode or extend a convenience class such as {@link models.tree.jpa.AbstractNode})
     * @param isContainer whether this node can be a container of other nodes (i.e. a non-leaf node)
     * @return a registered {@link NodeType}
     */
    public NodeType type(Class<? extends Node> nodeClass, boolean isContainer) {
        String name = nodeClass.getSimpleName().substring(0, 1).toLowerCase() + nodeClass.getSimpleName().substring(1);
        NodeType nodeType = new NodeType(name, isContainer, nodeClass, this);
        nodeTypes.put(name, nodeType);
        nodeTypesByClass.put(nodeClass, nodeType);
        return nodeType;
    }

    public static NodeType getNodeType(String name) {
        return nodeTypes.get(name);
    }

    public static NodeType getNodeType(Class type) {
        return nodeTypesByClass.get(type);
    }

    public void init() {
        storage = getStorage();
        getRootType();
        getNodes();
    }

    protected TreeStorage getStorage() {
        TreeStorage storage = null;
        Class<?> storageClass = Play.classloader.loadApplicationClass("controllers.tree.JPATreeStorage");
        try {
            storage = (TreeStorage) storageClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return storage;
    }

    /**
     * The qualifier for this tree. By default, it is the classname starting with a lowercase.
     * By convention, the classname should end with "Tree".
     *
     * @return a qualifier for the tree, unique for all the application.
     */
    public String getName() {
        String className = this.getClass().getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    /**
     * Returns all the possible node types for this tree.<br>
     * Register a type with {@link #type(Class, boolean)}
     *
     * @return an Array of {@link NodeType}
     */
    protected abstract NodeType[] getNodes();

    /**
     * The default type of nodes (returned by jsTree when a default node is created)
     *
     * @return the {@link NodeType} of the default node to create
     */
    protected abstract NodeType getDefaultType();

    /**
     * The name of the root node that will contain all other nodes of the tree.
     * If it is not defined, no root node will be inserted in the tree. If it is defined, a root node with
     * the indicated name will be the only valid root node of the tree. The type of the root node is "root".
     *
     * @return The name of the root node for all nodes of the tree, <code>null</code> if there shouldn't be a root node.
     */
    protected String getRootName() {
        return null;
    }

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

    public GenericTreeNode getNode(Long objectId, String type, String treeId) {
        return storage.getTreeNode(objectId, type, getName());
    }

    public List<? extends JSTreeNode> getChildren(Long parentId, String type, Map<String, String> args) {
        if(getRootName() != null && (parentId == -1 || parentId == null)) {
            RootNode rootNode = new RootNode(getRootName(), this.getName(), -1l, true, true, "root", storage.getChildren(parentId, getName(), type));
            List<JSTreeNode> nodes = new ArrayList<JSTreeNode>();
            nodes.add(rootNode);
            return nodes;
        } else {
            return storage.getChildren(parentId, getName(), type);
        }
    }

    public F.Tuple<Long, String> create(Long parentId, String parentType, Long position, String name, String type, Map<String, String> args) {
        NodeType nt = null;
        if (type == null) {
            nt = getRootType();
        } else if (type.equals("default")) {
            nt = getDefaultType();
        } else {
            nt = getNodeType(type);
        }
        if (nt == null) {
            throw new RuntimeException("Could not find a registered NodeType for type '" + type + "'");
        }

        try {
            GenericTreeNode node = storage.getNewTreeNode();
            populateTreeNode(node, parentId, parentType, name, nt, this.getName());
            node.setPath(storage.computePath(storage.getTreeNode(parentId, parentType, getName()), node, node.getName()));

            // uniqueness check
            if(storage.exists(node)) {
                return null;
            }

            node = storage.persistTreeNode(node);

            Node object = createObjectNode(name, nt, args);
            if (object == null) {
                throw new RuntimeException(String.format("New instance for node '%s' of type '%s' is null", name, nt.getName()));
            }
            object = storage.persistObject(object);

            node.setNodeId(object.getId());

            node = storage.updateTreeNode(node);

            return new F.Tuple<Long, String>(object.getId(), nt.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean rename(Long id, String name, String type) {
        return storage.rename(id, name, getName(), type);
    }

    public boolean copy(Long id, Long target, Long position) {
        return storage.copy(id, target, true, getNodes(), getName());
    }

    public boolean move(Long id, String type, Long target, String targetType, Long position) {
        return storage.move(id, type, target, targetType, getName());
    }

    public boolean remove(Long id, Long parentId, String type, Map<String, String> args) {
        return storage.remove(id, isRemovalPropagated(), getName(), type);
    }

    /**
     * Renames all TreeNode-s bound to a given Node
     *
     * @param object the Node object to rename
     * @param name   the new name
     */
    public void rename(Node object, String name) {
        if (name == null) {
            return;
        }
        NodeType type = getNodeType(object.getClass());
        // may happen before the tree is initialized.
        if (type != null) {
            storage.renameTreeNodes(name, type.getName(), object.getId(), this.getName());
        }
    }

    public static void renameNode(Node object, String name) {
        AbstractTree.getNodeType(object.getClass()).getTree().rename(object, name);
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
            renameObject(object, name);
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

    private void renameObject(Node object, String name) {
        List<String> nameFields = TreePlugin.findNameFields(object);
        for (String field : nameFields) {
            try {
                Method setter = object.getClass().getMethod("set" + field.substring(0, 1).toUpperCase() + field.substring(1), String.class);
                setter.invoke(object, name);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException("Error (re)naming node " + object.getClass().getSimpleName());
            }
        }
    }

    private void populateTreeNode(GenericTreeNode n, Long parentObjectId, String parentType, String name, NodeType type, String treeId) {
        GenericTreeNode parent = getNode(parentObjectId, parentType, treeId);
        if (parent == null && parentObjectId != -1) {
            throw new RuntimeException("Could not find parent node with ID " + parentObjectId);
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

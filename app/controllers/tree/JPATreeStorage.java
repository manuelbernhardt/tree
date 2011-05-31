package controllers.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;

import models.tree.jpa.TreeNode;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import tree.JSTreeNode;
import tree.persistent.AbstractTree;
import tree.persistent.GenericTreeNode;
import tree.persistent.Node;
import tree.persistent.NodeType;
import tree.persistent.TreeStorage;

/**
 * JPA implementation of the TreeStorage.
 * By default, the {@link TreeNode} class is used to store all information necessary for node manipulation (path, ...).
 * It is possible to make use of a subclass of TreeNode by using the constructor {@link JPATreeStorage#JPATreeStorage(Class<TreeNode) treeNodeClass}
 * FIXME copying trees is broken, the tree information (paths) need to be re-computed recursively when copying hierarchies.
 * FIXME implement duplicate check on node creation
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class JPATreeStorage extends TreeStorage {

    private final Class<? extends GenericTreeNode> treeNodeClass;

    public JPATreeStorage() {
        this.treeNodeClass = TreeNode.class;
    }

    public JPATreeStorage(Class<? extends GenericTreeNode> treeNodeClass) {
        this.treeNodeClass = treeNodeClass;
    }

    @Override
    public GenericTreeNode getNewTreeNode() {
        return new TreeNode();
    }

    @Override
    public GenericTreeNode persistTreeNode(GenericTreeNode node) {
        TreeNode treeNode = (TreeNode) node;
        treeNode.create();
        return node;
    }

    @Override
    public boolean exists(GenericTreeNode node) {
        return findJSTreeNodes("from TreeNode n where n.path = ? and n.type = ? and n.treeId = ?", node.getPath(), node.getType(), node.getTreeId()).size() > 0;
    }

    @Override
    public Node persistObject(Node concrete) {
        ((Model) concrete).create();
        return concrete;
    }

    @Override
    public Node updateObject(Node node) {
        return (Node) ((Model) node).save();
    }

    @Override
    public GenericTreeNode updateTreeNode(GenericTreeNode node) {
        return (GenericTreeNode) ((Model) node).save();
    }

    @Override
    public GenericTreeNode getTreeNode(Long nodeId, String type, String treeId) {
        return findTreeNode(nodeId, treeId, type);
    }

    @Override
    public boolean remove(Long id, boolean removeObject, String treeId, String type) {
        try {
            GenericTreeNode parent = findTreeNode(id, treeId, type);

            String pathLike = parent.getPath() + "%";
            List<Long> kids = queryList("select n.id from TreeNode n where n.treeId = ? and n.path like ? and n.level > ? and n.threadRoot.id = ? order by n.path desc", treeId, pathLike, parent.getLevel(), parent.getThreadRoot().getId());
            if (!kids.isEmpty()) {
                if (removeObject) {
                    List<Object[]> nodes = queryList("select n.nodeId, n.type from TreeNode n where n.treeId = ? and n.path like ? and n.level > ? and n.threadRoot.id = ? order by n.type desc", treeId, pathLike, parent.getLevel(), parent.getThreadRoot().getId());
                    Map<String, List<Long>> byType = toTypeMap(nodes);
                    for (String t : byType.keySet()) {
                        NodeType nodeType = AbstractTree.getNodeType(t);
                        namedUpdateQuery("delete from " + nodeType.getNodeClass().getSimpleName() + " n where n.id in (:nodes)", "nodes", byType.get(nodeType));
                    }
                }
                namedUpdateQuery("delete from TreeNode n where n.treeId = '" + treeId + "' and n.id in (:kids)", "kids", kids);
            }

            if (removeObject) {
                updateQuery("delete from " + parent.getNodeType().getNodeClass().getSimpleName() + " n where n.id = ?", parent.getNodeId());
            }

            updateQuery("update TreeNode n set n.threadRoot = null where n.treeId = '" + treeId + "' and n.id = ?", parent.getId());
            updateQuery("delete from TreeNode n where n.treeId = '" + treeId + "' and n.id = ?", parent.getId());
        } catch(Throwable t) {
            t.printStackTrace();
            return false;
        }
        return true;
    }

    private Map<String, List<Long>> toTypeMap(List<Object[]> nodes) {
        Map<String, List<Long>> nodesByType = new HashMap<String, List<Long>>();
        for (Object[] pair : nodes) {
            Long nid = (Long) pair[0];
            String type = (String) pair[1];
            List<Long> ids = nodesByType.get(type);
            if (ids == null) {
                ids = new ArrayList<Long>();
                nodesByType.put(type, ids);
            }
            ids.add(nid);
        }
        return nodesByType;
    }

    @Override
    public List<JSTreeNode> getChildren(Long parentObjectId, String treeId, String type) {
        if (parentObjectId == null || parentObjectId == -1) {
            return findJSTreeNodes("from TreeNode n where n.treeId = '" + treeId + "' and n.threadRoot = n");
        } else {
            GenericTreeNode parent = findTreeNode(parentObjectId, treeId, type);
            return findJSTreeNodes("from TreeNode n where n.treeId = '" + treeId + "' and n.level = ? and n.path like ? and n.threadRoot = ?", parent.getLevel() + 1, parent.getPath() + "%", parent.getThreadRoot());
        }
    }

    @Override
    public boolean rename(Long objectId, String name, String treeId, String type) {
        try {
            GenericTreeNode n = findTreeNode(objectId, treeId, type);
            n.setName(name);
            n.setPath(computePath(n.getParent(), n, n.getName()));
            ((Model)n).save();

            // TODO this assumes there is a "name" field, whereas:
            // 1) it may be named differently
            // 1) there may be more than one (though unlikely)
            // use the @tree.persistent.NodeName annotation to figure out the fields (this needs to be done in the AbstractTree tough)
            updateQuery("update " + n.getNodeType().getNodeClass().getSimpleName() + " n set n.name = ? where n.id = ?", name, n.getNodeId());
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean move(Long objectId, String type, Long target, String targetType, String treeId) {
        try {
            GenericTreeNode node = findTreeNode(objectId, treeId, type);
            GenericTreeNode oldParent = node.getParent();
            GenericTreeNode parent = findTreeNode(target, treeId, targetType);

            String newPath = parent.getPath();
            Integer delta = parent.getLevel() - node.getLevel() + 1;

            if (node.getThreadRoot().getId().equals(node.getId())) {
                // if we are a root node
                updateQuery("update TreeNode set path = concat(?, path), level = level + ? where treeId = ? and threadRoot = ?", newPath, delta, treeId, parent.getThreadRoot());
            } else {
                String oldPath = node.getPath();
                Integer oldPathLength = oldParent.getPath().length();
                String pathLike = oldPath + "%";
                updateQuery("update TreeNode set path = concat(?, substring(path, ?, length(path))), level = level + ? where treeId = ? and threadRoot = ? and path like ?", newPath, oldPathLength + 1, delta, treeId, parent.getThreadRoot(), pathLike);
            }
        } catch(Throwable t) {
            t.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean copy(Long id, Long target, boolean copyObject, NodeType[] types, String treeId) {
        // TODO implement
        throw new RuntimeException("not implemented");
    }

    @Override
    public void renameTreeNodes(String name, String type, Long nodeId, String treeId) {
        List<GenericTreeNode> treeNodes = findTreeNodes("from TreeNode n where n.type = ? and n.nodeId = ? and n.treeId = ?", type, nodeId, treeId);
        for (GenericTreeNode n : treeNodes) {
            n.setName(name);
            n.setPath(computePath(n.getParent(), n, n.getName()));
            ((Model)n).save();
        }
    }

    public GenericTreeNode findTreeNode(Long nodeId, String treeId, String type) {
        List<GenericTreeNode> node = JPA.em().createQuery(transform("select n from TreeNode n where nodeId = :nodeId and type = :type and treeId = :treeId")).setParameter("nodeId", nodeId).setParameter("treeId", treeId).setParameter("type", type).getResultList();
        if(node.isEmpty()) {
            return null;
        } else {
            if(node.size() == 1) {
                return node.get(0);
            } else {
                throw new RuntimeException(String.format("Error: multiple tree nodes with the same id found, nodeId %s, treeId %s, type %s", nodeId, treeId, type));
            }
        }
    }

    public List<GenericTreeNode> findTreeNodes(String query, Object... arguments) {
        return queryList(query, arguments);
    }

    public List<JSTreeNode> findJSTreeNodes(String query, Object... arguments) {
        return queryList(query, arguments);
    }

    public <T> List<T> queryList(String query, Object... args) {
        Query q = JPA.em().createQuery(transform(query));
        for (int i = 0; i < args.length; i++) {
            q.setParameter(i + 1, args[i]);
        }
        return (List<T>) q.getResultList();
    }

    public void updateQuery(String query, Object... args) {
        Query q = JPA.em().createQuery(transform(query));
        for (int i = 0; i < args.length; i++) {
            q.setParameter(i + 1, args[i]);
        }
        q.executeUpdate();
    }

    public void namedUpdateQuery(String query, String argName, Object arg) {
        Query q = JPA.em().createQuery(transform(query));
        q.setParameter(argName, arg);
        q.executeUpdate();
    }


    /**
     * Transforms the query to use an alternate TreeNode class name, if necessary
     *
     * @param query the query to transform
     * @return a modified query string in which all occurences of "TreeNode" have been replaced with the name of the alternate TreeNode subclass
     */
    protected String transform(String query) {
        if (!treeNodeClass.equals(TreeNode.class)) {
            return query.replaceAll("\\bTreeNode\\b", treeNodeClass.getSimpleName());
        }
        return query;
    }
}

package controllers.tree;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Query;

import models.tree.jpa.AbstractNode;
import models.tree.jpa.TreeNode;
import play.db.jpa.JPA;
import play.db.jpa.JPABase;
import play.db.jpa.Model;
import tree.JSTreeNode;
import tree.persistent.AbstractTree;
import tree.persistent.GenericTreeNode;
import tree.persistent.Node;
import tree.persistent.NodeType;
import tree.persistent.TreeStorage;

/**
 * JPA implementation of the TreeStorage
 * FIXME copying trees is broken, the tree information (paths) need to be re-computed recursively when copying hierarchies.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class JPATreeStorage extends TreeStorage {

    public JPATreeStorage() {
        
    }

    @Override
    public GenericTreeNode getNewTreeNode() {
        return new TreeNode();
    }

    @Override
    public GenericTreeNode createTreeNode(GenericTreeNode node) {
        TreeNode treeNode = (TreeNode) node;
        treeNode.create();
        return node;
    }

    @Override
    public Node createObject(Node concrete) {
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
    public GenericTreeNode getTreeNode(Long id, String type, String treeId) {
        JPABase node = TreeNode.find(id, type, treeId);
        return (GenericTreeNode) node;
    }

    @Override
    public void remove(Long id, boolean removeObject, String treeId, String type) {
        TreeNode parent = TreeNode.find(id, type, treeId);

        String pathLike = parent.getPath() + "%";
        List<Long> kids = queryList("select n.id from TreeNode n where n.treeId = ? and n.path like ? and n.level > ? and n.threadRoot.id = ? order by n.path desc", Long.class, treeId, pathLike, parent.getLevel(), parent.getThreadRoot().getId());
        if (!kids.isEmpty()) {
            if (removeObject) {
                List<Object[]> nodes = queryList("select n.nodeId, n.type from TreeNode n where n.treeId = ? and n.path like ? and n.level > ? and n.threadRoot.id = ? order by n.type desc", Object[].class, treeId, pathLike, parent.getLevel(), parent.getThreadRoot().getId());
                Map<String, List<Long>> byType = toTypeMap(nodes);
                for(String t : byType.keySet()) {
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
    }

    private Map<String, List<Long>> toTypeMap(List<Object[]> nodes) {
        Map<String, List<Long>> nodesByType = new HashMap<String, List<Long>>();
        for(Object[] pair : nodes) {
            Long nid = (Long) pair[0];
            String type = (String) pair[1];
            List<Long> ids = nodesByType.get(type);
            if(ids == null) {
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
            return TreeNode.find("from TreeNode n where n.treeId = '" + treeId + "' and n.threadRoot = n").<JSTreeNode>fetch();
        } else {
            TreeNode parent = TreeNode.find(parentObjectId, type, treeId);
            return getChildren(parent.getLevel(), parent.getPath(), parent.getThreadRoot(), treeId);
        }
    }

    public static List<JSTreeNode> getChildren(Integer parentLevel, String parentPath, TreeNode parentThreadRoot, String treeId) {
        return TreeNode.find("from TreeNode n where n.treeId = '" + treeId + "' and n.level = ? and n.path like ? and n.threadRoot = ?", parentLevel + 1, parentPath + "%", parentThreadRoot).<JSTreeNode>fetch();
    }

    @Override
    public void rename(Long objectId, String name, String treeId, String type) {

        TreeNode n = TreeNode.find(objectId, type, treeId);
        n.setName(name);
        n.save();

        // TODO this assumes there is a "name" field, whereas:
        // 1) it may be named differently
        // 1) there may be more than one (though unlikely)
        // use the @tree.persistent.NodeName annotation to figure out the fields (this needs to be done in the AbstractTree tough)
        updateQuery("update " + n.getNodeType().getNodeClass().getSimpleName() + " n set n.name = ? where n.id = ?", name, n.getNodeId());
    }

    @Override
    public void move(Long objectId, String type, Long target, String targetType, String treeId) {
        TreeNode node = TreeNode.find(objectId, type, treeId);
        TreeNode oldParent = (TreeNode) node.getParent();
        TreeNode parent = TreeNode.find(target, targetType, treeId);

        String newPath = parent.getPath();
        Integer delta = parent.getLevel() - node.getLevel() + 1;

        if (node.getThreadRoot().getId().equals(node.getId())) {
            updateQuery("update TreeNode set path = concat(?, path), level = level + ? where n.treeId = '" + treeId + "' and threadRoot = ?", newPath + "____", delta, parent.getThreadRoot());
        } else {
            String oldPath = node.getPath();
            Integer oldPathLength = oldParent.getPath().length();
            String pathLike = oldPath + "%";
            updateQuery("update TreeNode set path = concat(?, substring(path, ?, length(path))), level = level + ? where n.treeId = '" + treeId + "' and threadRoot = ? and path like ?", newPath, oldPathLength + 1, delta, parent.getThreadRoot(), pathLike);
        }
    }


    @Override
    public void copy(Long id, Long target, boolean copyObject, NodeType[] types, String treeId) {
        TreeNode node = TreeNode.find(id, treeId);
        TreeNode parent = TreeNode.find(target, treeId );
        Integer delta = parent.getLevel() - node.getLevel() + 1;
        String oldPath = node.getPath();
        String newPath = parent.getThreadRoot().getId().equals(parent.getId()) ? parent.getPath() + "___" : parent.getPath(); // FIXME "___"

        // at this point we do not know yet what will be the ID of the newly inserted rows so we have to update the path afterwards with the correct ID
        // in order to identify which rows need to be updated, we prepend a unique transaction ID to the path
        String copyTransactionId = System.currentTimeMillis() + "###" + id + "###";
        newPath = copyTransactionId + newPath;

        Integer oldPathLength = node.getParent().getPath().length();
        String pathLike = oldPath + "%";

        // see http://opensource.atlassian.com/projects/hibernate/browse/HHH-2692
//            Query query = JPA.em().createQuery("insert into TreeNode (name, type, opened, level, path, threadRoot, abstractNode) " +
//                    "select c.name, c.type, c.opened, c.level + :delta, concat(:newPath, substring(path, :oldPathLength, length(path))), (select :newThreadRoot from AbstractNode), c.abstractNode " +
//                    "from TreeNode c " +
//                    "where c.threadRoot = :oldThreadRoot and c.path like :pathLike");
//            query.setParameter("delta", delta);
//            query.setParameter("newPath", newPath);
//            query.setParameter("oldPathLength", oldPathLength + 1);
//            query.setParameter("newThreadRoot", parent.getThreadRoot());
//            query.setParameter("oldThreadRoot", node.getThreadRoot());
//            query.setParameter("pathLike", pathLike);

        Query query = JPA.em().createNativeQuery("insert into TreeNode (name, type, opened, level, path, threadRoot_id, abstractNode_id) " +
                "select c.name, c.type, c.opened, c.level + ?, concat(?, substring(path, ?, length(path))), ?, c.abstractNode_id " +
                "from TreeNode c " +
                "where c.threadRoot_id = ? and c.path like ? and c.treeId = ?");
        query.setParameter(1, delta);
        query.setParameter(2, newPath);
        query.setParameter(3, oldPathLength + 1);
        query.setParameter(4, parent.getThreadRoot().getId());
        query.setParameter(5, node.getThreadRoot().getId());
        query.setParameter(6, pathLike);
        query.setParameter(7, treeId);

        query.executeUpdate();


        // FIXME recompute the paths per level
        // fetch the IDs for each level (until max(level) where path startsWith copyTransactionId)
        // for each level
        //   path = path(level -1) + id where path startsWith copyTransactionId
        // remove copyTransactionId
        /*
        Map<Integer, List<Long>> nodes = new HashMap<Integer, List<Long>>();
        Query toUpdate = JPA.em().createNativeQuery("select n.level, n.id from TreeNode n where n.level like ? order by level asc");
        toUpdate.setParameter(1, copyTransactionId + "%");
        List<Object[]> toUpdateIds = (List<Object[]>) toUpdate.getResultList();
        for(Object[] levelIds : toUpdateIds) {
            Integer level = (Integer) levelIds[0];
            List<Long> ids = nodes.get(level);
            if(ids == null) {
                ids = new ArrayList<Long>();
                nodes.put(level, ids);
            }
            ids.add((Long)levelIds[1]);
        }
        Integer max = query("select max(level) from TreeNode n where n.path like ? group by n.level", Integer.class, copyTransactionId + "%");
        String parentPath = parent.getPath() + "___";
        for(int i = 0; id < max; i++) {
            
            updateQuery("update TreeNode set path = substring(concat(?, id), ?) where path like ?", parentPath, copyTransactionId.length() + 1, copyTransactionId);

        }
        */

        // now, update the paths that are for the moment incorrect
        Query pathQuery = JPA.em().createNativeQuery("update TreeNode set path = concat(substring(path, ?, length(path) - ? - locate('___', reverse(path)) + 1), id) where path like ? and treeId = ?");
        pathQuery.setParameter(1, copyTransactionId.length() + 1);
        pathQuery.setParameter(2, copyTransactionId.length());
        pathQuery.setParameter(3, copyTransactionId + "%");
        pathQuery.setParameter(4, treeId);
        pathQuery.executeUpdate();

        if (copyObject) {
            // fetch all (type, (AbstractNode, TreeNode)) where TreeNode-s are the freshly copied nodes (they still point to the original AbstractNode)
            Map<String, Map<Long, Long>> nodeIdsByType = new HashMap<String, Map<Long, Long>>();
            Query copied = JPA.em().createNativeQuery("select n.type, n.abstractNode_id, n.id from TreeNode n join TreeNode o where n.abstractNode_id = o.abstractNode_id and n.id <> o.id and n.path not like ?");
            copied.setParameter(1, pathLike);
            List<Object[]> ids = (List<Object[]>) copied.getResultList();
            for (Object[] p : ids) {
                String type = (String) p[0];
                Map<Long, Long> i = nodeIdsByType.get(type);
                if (i == null) {
                    i = new HashMap<Long, Long>();
                    nodeIdsByType.put(type, i);
                }
                i.put(((BigInteger) p[1]).longValue(), ((BigInteger) p[2]).longValue());
            }

            // with the inheritance strategy in use we have no choice but to copy the concrete nodes by hand using reflection
            for (NodeType type : types) {
                Map<Long, Long> copyIds = new HashMap<Long, Long>();
                Map<Long, Long> i = nodeIdsByType.get(type.getName());
                if (i != null) {
                    try {
                        Query q = JPA.em().createQuery("from " + type.getNodeClass().getSimpleName() + " n where n.id in (:ids)");
                        q.setParameter("ids", i.keySet());
                        List<AbstractNode> original = q.getResultList();
                        for (AbstractNode n : original) {
                            AbstractNode copy = (AbstractNode) n.clone();
                            copy.id = null;
                            copy.create();
                            copyIds.put(i.get(n.getId()), copy.getId());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // TODO see if this can be optimized
                    for (Long tn : copyIds.values()) {
                        updateQuery("update TreeNode set abstractNode = (select a.id from AbstractNode a where a.id = ?) where id = ?", copyIds.get(tn), tn);
                    }
                }
            }
        }
    }

    private <T> T query(String query, Object id, Class<T> type) {
        Query q = JPA.em().createQuery(query);
        q.setParameter("id", id);
        return (T) q.getSingleResult();
    }

    private <T> T query(String query, Class<T> type, Object... args) {
        Query q = JPA.em().createQuery(query);
        for (int i = 0; i < args.length; i++) {
            q.setParameter(i + 1, args[i]);
        }
        List r = q.getResultList();
        if (r.isEmpty()) return null;
        return (T) r.get(0);
    }


    private <T> List<T> queryList(String query, Object id, Class<T> type) {
        Query q = JPA.em().createQuery(query);
        q.setParameter("id", id);
        return (List<T>) q.getResultList();
    }

    private <T> List<T> queryList(String query, Class<T> type, Object... args) {
        Query q = JPA.em().createQuery(query);
        for (int i = 0; i < args.length; i++) {
            q.setParameter(i + 1, args[i]);
        }
        return (List<T>) q.getResultList();
    }


    private void updateQuery(String query, Object... args) {
        Query q = JPA.em().createQuery(query);
        for (int i = 0; i < args.length; i++) {
            q.setParameter(i + 1, args[i]);
        }
        q.executeUpdate();
    }

    private void namedUpdateQuery(String query, String argName, Object arg) {
        Query q = JPA.em().createQuery(query);
        q.setParameter(argName, arg);
        q.executeUpdate();
    }
}

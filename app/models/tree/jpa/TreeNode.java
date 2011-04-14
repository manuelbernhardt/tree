package models.tree.jpa;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import controllers.tree.JPATreeStorage;
import tree.persistent.AbstractTree;
import tree.persistent.GenericTreeNode;
import tree.JSTreeNode;
import tree.persistent.NodeType;
import tree.persistent.Node;
import play.db.jpa.Model;

/**
 * TODO optimize this table (indexes)
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@Entity
public class TreeNode extends Model implements GenericTreeNode {

    public String treeId;
    public String name;
    public transient NodeType nodeType;
    public String type;
    public boolean opened;
    public int level;
    public Long nodeId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    public TreeNode threadRoot;

    // let's assume nobody creates such mad hierarchies
    @Column(length = 5000)
    public String path;

    public String getTreeId() {
        return treeId;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getNodeId() {
        return this.nodeId;
    }

    public void setNodeId(Long id) {
        this.nodeId = id;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType type) {
        this.nodeType = type;
    }

    public GenericTreeNode getParent() {
        if (this.threadRoot.getId().equals(this.getId())) {
            return this;
        } else {
            return find("from TreeNode n where level = ? and ? like concat(path, '%')", level - 1, path).first();
        }
    }

    public List<JSTreeNode> getChildren() {
        return JPATreeStorage.getChildren(level, path, threadRoot, treeId);
    }

    public boolean isOpen() {
        return opened;
    }

    public void setOpen(boolean opened) {
        this.opened = opened;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public TreeNode getThreadRoot() {
        return threadRoot;
    }

    public void setThreadRoot(GenericTreeNode threadRoot) {
        this.threadRoot = (TreeNode) threadRoot;
    }

    public boolean isContainer() {
        return nodeType.isContainer();
    }

    public String getType() {
        return type;
    }

    @PreUpdate
    @PrePersist
    public void doSave() {
        if (nodeType != null) {
            this.type = nodeType.getName();
        }
    }

    @PostLoad
    public void doLoad() {
        this.nodeType = AbstractTree.getNodeType(this.type);
    }

    public static TreeNode find(Long id, String treeId) {
        return TreeNode.find("from TreeNode n where id = ? and treeId = ?", id, treeId).first();
    }

    public static TreeNode find(Long objectId, String type, String treeId) {
        return TreeNode.find("from TreeNode n where nodeId = ? and type = ? and treeId = ?", objectId, type, treeId).first();
    }

    public static TreeNode findById(Long id) {
        throw new RuntimeException("Use find(id, treeId)");
    }

    public static void rename(Node object, String name) {
        if(name == null) {
            return;
        }
        NodeType type = AbstractTree.getNodeType(object.getClass());
        // may happen before the tree is initialized.
        if(type != null) {
            List<TreeNode> treeNodes = TreeNode.find("from TreeNode n where n.type = ? and n.nodeId = ?", type.getName(), object.getId()).fetch();
            for(TreeNode n : treeNodes) {
                n.name = name;
                n.save();
            }
        }
    }

}

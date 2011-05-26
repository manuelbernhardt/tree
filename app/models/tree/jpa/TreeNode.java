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

import play.db.jpa.JPA;
import play.db.jpa.JPABase;
import play.db.jpa.Model;
import tree.JSTreeNode;
import tree.persistent.AbstractTree;
import tree.persistent.GenericTreeNode;
import tree.persistent.NodeType;

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
        return JPA.em().createQuery("from TreeNode n where n.treeId = '" + treeId + "' and n.level = :level and n.path like :pathLike and n.threadRoot = :threadRoot").setParameter("level", level + 1).setParameter("pathLike", path + "%").setParameter("threadRoot", threadRoot).getResultList();
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

    @Override
    public boolean create() {
        this.type = nodeType.getName();
        return super.create();
    }

    @Override
    public JPABase save() {
        this.type = nodeType.getName();
        return super.save();
    }

    @PostLoad
    public void doLoad() {
        this.nodeType = AbstractTree.getNodeType(this.type);
    }
}

package controllers.tree;

import java.util.List;

import models.tree.JSTreeNode;

/**
 * A TreeHandler provides all the necessary functionality to retrieve trees and persist various operations (renaming, copying, etc.)
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface TreeDataHandler {

    String getName();

    List<? extends JSTreeNode> getChildren(Long parentId);

    Long create(Long parentId, Long position, String name, String type);

    boolean rename(Long id, String name, String type);

    void copy(Long id, Long target, Long position);

    void move(Long id, Long target, Long position);

    boolean remove(Long id) throws Exception;
}

package controllers.tree;

import java.util.List;
import java.util.Map;

import models.tree.JSTreeNode;

/**
 * A TreeHandler provides all the necessary functionality to retrieve trees and persist various operations (renaming, copying, etc.)
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface TreeDataHandler {

    String getName();

    List<? extends JSTreeNode> getChildren(Long parentId, Map<String, String> args);

    Long create(Long parentId, Long position, String name, String type, Map<String, String> args);

    boolean rename(Long id, String name, String type);

    void copy(Long id, Long target, Long position);

    void move(Long id, Long target, Long position);

    boolean remove(Long id, Long parentId, String type, Map<String, String> args);
}

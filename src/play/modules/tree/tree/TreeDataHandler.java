package tree;

import java.util.List;
import java.util.Map;

import play.libs.F;

/**
 * A TreeHandler provides all the necessary functionality to retrieve trees and persist various operations (renaming, copying, etc.)
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface TreeDataHandler {

    String getName();

    List<? extends JSTreeNode> getChildren(Long parentId, String type, Map<String, String> args);

    F.Tuple<Long, String> create(Long parentId, String parentType, Long position, String name, String type, Map<String, String> args);

    boolean rename(Long id, String name, String type);

    boolean copy(Long id, Long target, Long position);

    boolean move(Long id, String type, Long target, String targetType, Long position);

    boolean remove(Long id, Long parentId, String type, Map<String, String> args);
}

package controllers.tree;

import java.util.List;

import com.google.gson.JsonObject;
import models.tree.JSTreeNode;
import play.mvc.Controller;

/**
 * Generic controller for tree operations.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class TreeController extends Controller {

    public static void create(String treeId, Long parentId, Long position, String name, String type) {

        Long node = Tree.getTree(treeId).create(parentId, position, name, type);
        JsonObject status = null;
        if (node == null) {
            status = makeStatus(0, null);
        } else {
            status = makeStatus(1, node);
        }
        renderJSON(status.toString());
    }

    public static void remove(String treeId, Long id) {
        try {
            Tree.getTree(treeId).remove(id);
        } catch (Exception e) {
            e.printStackTrace();
            renderJSON(makeStatus(0, null).toString());
        }
        renderJSON(makeStatus(1, null).toString());
    }

    public static void rename(String treeId, Long id, String name, String type) {
        boolean renamed;
        renamed = Tree.getTree(treeId).rename(id, name, type);
        if (renamed) {
            renderJSON(makeStatus(1, null).toString());
        } else {
            renderJSON(makeStatus(0, null).toString());
        }
    }

    public static void move(String treeId, Long id, Long target, Long position, String name, boolean copy) {
        try {
            if (copy) {
                Tree.getTree(treeId).copy(id, target, position);
            } else {
                Tree.getTree(treeId).move(id, target, position);
            }
        } catch (Exception e) {
            e.printStackTrace();
            renderJSON(makeStatus(0, null).toString());
        }
        renderJSON(makeStatus(1, null).toString());
    }

    public static void getChildren(String treeId, Long id) {
        List<? extends JSTreeNode> children = Tree.getTree(treeId).getChildren(id);
        renderJSON(Tree.getGson().toJson(children));
    }

    public static JsonObject makeStatus(int status, Long id) {
        JsonObject r = new JsonObject();
        r.addProperty("status", status);
        if (id != null) {
            r.addProperty("id", id);
        }
        return r;
    }


}

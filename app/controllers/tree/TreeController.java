package controllers.tree;

import java.util.List;

import com.google.gson.JsonObject;
import models.tree.JSTreeNode;
import play.mvc.Controller;
import play.mvc.Util;

/**
 * Generic controller for tree operations.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class TreeController extends Controller {

    public static void create(String treeId, Long parentId, Long position, String name, String type, Long id) {
        createDirect(treeId, parentId, position, name, type, id);
    }

    @Util
    public static void createDirect(String treeId, Long parentId, Long position, String name, String type, Long id) {
        Long node = Tree.getTree(treeId).create(parentId, position, name, type, id);
        JsonObject status = null;
        if (node == null) {
            status = makeStatus(0, null);
        } else {
            status = makeStatus(1, node);
        }
        renderJSON(status.toString());
    }

    public static void remove(String treeId, Long id, Long parentId, String type) {
        removeDirect(treeId, id, parentId, type);
    }

    @Util
    public static void removeDirect(String treeId, Long id, Long parentId, String type) {
        try {
            Tree.getTree(treeId).remove(id, parentId, type);
        } catch (Throwable e) {
            e.printStackTrace();
            renderJSON(makeStatus(0, null).toString());
        }
        renderJSON(makeStatus(1, null).toString());
    }

    public static void rename(String treeId, Long id, String name, String type) {
        renameDirect(treeId, id, name, type);
    }

    @Util
    public static void renameDirect(String treeId, Long id, String name, String type) {
        boolean renamed;
        renamed = Tree.getTree(treeId).rename(id, name, type);
        if (renamed) {
            renderJSON(makeStatus(1, null).toString());
        } else {
            renderJSON(makeStatus(0, null).toString());
        }
    }

    public static void move(String treeId, Long id, Long target, Long position, String name, boolean copy) {
        moveDirect(treeId, id, target, position, name, copy);
    }

    @Util
    public static void moveDirect(String treeId, Long id, Long target, Long position, String name, boolean copy) {
        try {
            if (copy) {
                Tree.getTree(treeId).copy(id, target, position);
            } else {
                Tree.getTree(treeId).move(id, target, position);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            renderJSON(makeStatus(0, null).toString());
        }
        renderJSON(makeStatus(1, null).toString());
    }

    public static void getChildren(String treeId, Long id, String... args) {
        getChildrenDirect(treeId, id, args);
    }

    @Util
    public static void getChildrenDirect(String treeId, Long id, String[] args) {
        List<? extends JSTreeNode> children = Tree.getTree(treeId).getChildren(id, args);
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

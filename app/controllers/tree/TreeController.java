package controllers.tree;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import models.tree.jpa.TreeNode;
import tree.JSTreeNode;
import play.mvc.Controller;
import play.mvc.Util;
import tree.TreePlugin;
import tree.persistent.GenericTreeNode;
import tree.simple.SimpleNode;

/**
 * Generic controller for tree operations.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class TreeController extends Controller {

    private static Gson gson = null;

    public static void create(String treeId, Long parentId, Long position, String name, String type, Map<String, String> args) {
        createDirect(treeId, parentId, position, name, type, args);
    }

    @Util
    public static void createDirect(String treeId, Long parentId, Long position, String name, String type, Map<String, String> args) {
        Long node = TreePlugin.getTree(treeId).create(parentId, position, name, type, args);
        JsonObject status = null;
        if (node == null) {
            status = makeStatus(0, null);
        } else {
            status = makeStatus(1, node);
        }
        renderJSON(status.toString());
    }

    public static void remove(String treeId, Long id, Long parentId, String type, Map<String, String> args) {
        removeDirect(treeId, id, parentId, type, args);
    }

    @Util
    public static void removeDirect(String treeId, Long id, Long parentId, String type, Map<String, String> args) {
        boolean removed = TreePlugin.getTree(treeId).remove(id, parentId, type, args);
        if(!removed) {
            renderJSON(makeStatus(0, null).toString());
        } else {
            renderJSON(makeStatus(1, null).toString());
        }
    }

    public static void rename(String treeId, Long id, String name, String type) {
        renameDirect(treeId, id, name, type);
    }

    @Util
    public static void renameDirect(String treeId, Long id, String name, String type) {
        boolean renamed;
        renamed = TreePlugin.getTree(treeId).rename(id, name, type);
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
                TreePlugin.getTree(treeId).copy(id, target, position);
            } else {
                TreePlugin.getTree(treeId).move(id, target, position);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            renderJSON(makeStatus(0, null).toString());
        }
        renderJSON(makeStatus(1, null).toString());
    }

    public static void getChildren(String treeId, Long id, Map<String, String> args) {
        getChildrenDirect(treeId, id, args);
    }

    @Util
    public static void getChildrenDirect(String treeId, Long id, Map<String, String> args) {
        List<? extends JSTreeNode> children = TreePlugin.getTree(treeId).getChildren(id, args);
        renderJSON(getGson().toJson(children));
    }

    public static JsonObject makeStatus(int status, Long id) {
        JsonObject r = new JsonObject();
        r.addProperty("status", status);
        if (id != null) {
            r.addProperty("id", id);
        }
        return r;
    }


    public static Gson getGson() {
        if (gson == null) {
            final GsonBuilder builder = new GsonBuilder();
            final JSTreeNodeSerializer serializer = new JSTreeNodeSerializer();

            // workaround for gson not being smart enough (yet) to figure out type inheritance
            builder.registerTypeAdapter(JSTreeNode.class, serializer);
            builder.registerTypeAdapter(GenericTreeNode.class, serializer);
            builder.registerTypeAdapter(TreeNode.class, serializer);
            builder.registerTypeAdapter(SimpleNode.class, serializer);

            gson = builder.create();
        }
        return gson;
    }
}

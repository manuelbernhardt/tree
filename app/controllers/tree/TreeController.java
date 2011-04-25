package controllers.tree;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import play.Play;
import play.classloading.ApplicationClasses;
import play.mvc.Controller;
import play.mvc.Util;
import tree.JSTreeNode;
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

    public static void create(String treeId, Long parentId, String parentType, Long position, String name, String type, Map<String, String> args) {
        createDirect(treeId, parentId, parentType, position, name, type, args);
    }

    @Util
    public static void createDirect(String treeId, Long parentId, String parentType, Long position, String name, String type, Map<String, String> args) {
        Long objectId = TreePlugin.getTree(treeId).create(parentId, parentType, position, name, type, args);
        JsonObject status = null;
        if (objectId == null) {
            status = makeStatus(0, null);
        } else {
            status = makeStatus(1, objectId);
            status.addProperty("rel", type);
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

    public static void move(String treeId, Long id, String type, Long target, String targetType, Long position, String name, boolean copy) {
        moveDirect(treeId, id, type, target, targetType, position, name, copy);
    }

    @Util
    public static void moveDirect(String treeId, Long id, String type, Long target, String targetType, Long position, String name, boolean copy) {
        boolean success = false;
        try {
            if (copy) {
                success = TreePlugin.getTree(treeId).copy(id, target, position);
            } else {
                success = TreePlugin.getTree(treeId).move(id, type, target, targetType, position);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            success = false;
        }
        if(success) {
            renderJSON(makeStatus(1, null).toString());
        } else {
            renderJSON(makeStatus(0, null).toString());
        }
    }

    public static void getChildren(String treeId, Long id, String type, Map<String, String> args) {
        getChildrenDirect(treeId, id, type, args);
    }

    @Util
    public static void getChildrenDirect(String treeId, Long id, String type, Map<String, String> args) {
        List<? extends JSTreeNode> children = TreePlugin.getTree(treeId).getChildren(id, type, args);
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
            // TODO it does support it now (see http://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/GsonBuilder.html#registerTypeHierarchyAdapter(java.lang.Class, java.lang.Object))
            // TODO file ticket for Play! lib update
            builder.registerTypeAdapter(JSTreeNode.class, serializer);
            builder.registerTypeAdapter(GenericTreeNode.class, serializer);
            builder.registerTypeAdapter(SimpleNode.class, serializer);
            for(ApplicationClasses.ApplicationClass applicationClass : Play.classes.getAssignableClasses(GenericTreeNode.class)) {
                builder.registerTypeAdapter(applicationClass.javaClass, serializer);
            }

            gson = builder.create();
        }
        return gson;
    }
}

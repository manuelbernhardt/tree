package controllers.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import tree.JSTreeNode;
import tree.persistent.GenericTreeNode;
import tree.persistent.RootNode;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializer for JSTree nodes
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class JSTreeNodeSerializer implements JsonSerializer<JSTreeNode> {

    final boolean shouldRenderNodeState;

    public JSTreeNodeSerializer(boolean shouldRenderNodeState) {
      this.shouldRenderNodeState = shouldRenderNodeState;
    }

    public JsonElement serialize(JSTreeNode node, Type type, JsonSerializationContext context) {
        JsonObject o = new JsonObject();
        populateBasicProperties(node, context, o);

        if (node.isContainer()) {
            List<JSTreeNode> c = node.getChildren();
            JsonArray children = new JsonArray();
            if (node.isOpen() && !c.isEmpty()) {
                // render full children
                o.add("children", context.serialize(c));
            } else if (!c.isEmpty()) {
                // render "closed" children
                for (JSTreeNode n : c) {
                    JsonObject child = new JsonObject();
                    children.add(child);
                    populateBasicProperties(n, context, child);
                }
                o.add("children", children);
            }
        }
        return o;
    }

    private void populateBasicProperties(JSTreeNode node, JsonSerializationContext context, JsonObject o) {
        o.addProperty("data", node.getName());
        Map<String, Object> attributes = new HashMap<String, Object>();
        Long id = getNodeId(node);
        attributes.put("id", buildNodeId(node, id));
        attributes.put("rel", node.getType());
        o.add("attr", context.serialize(attributes));
        if (shouldRenderNodeState && node.isContainer()) {
            o.addProperty("state", state(node.isOpen()));
        }
    }

    public Long getNodeId(JSTreeNode node) {
        Long id = node.getId();
        if(node instanceof GenericTreeNode) {
            id = ((GenericTreeNode)node).getNodeId();
        }
        return id;
    }

    protected String buildNodeId(JSTreeNode node, Long id) {
        if(node instanceof GenericTreeNode) {
            return "node_" + ((GenericTreeNode)node).getTreeId() + "_" + node.getType() + "_" + id;
        }
        if(node instanceof RootNode) {
             return "node_" + ((RootNode)node).getTreeId() + "_" + node.getType() + "_" + id;
         }
         return "node_" + node.getType() + "_" + id;
    }

    private String state(boolean open) {
        return open ? "open" : "closed";
    }


}

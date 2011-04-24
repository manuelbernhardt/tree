package tree;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.templates.JavaExtensions;
import play.utils.Java;
import tree.persistent.AbstractTree;
import tree.persistent.Node;
import tree.persistent.NodeEnhancer;
import tree.persistent.NodeName;

/**
 * Plugin for the tree module.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class TreePlugin extends PlayPlugin {

    private final static Map<Node, List<String>> nameFieldCache = new HashMap<Node, List<String>>();
    private final static Map<String, TreeDataHandler> allTrees = new HashMap<String, TreeDataHandler>();

    @Override
    public void onApplicationStart() {
        init();
    }

    @Override
    public void onLoad() {
        allTrees.clear();
        init();
    }

    private static void init() {
        // initialize all trees
        List<ApplicationClasses.ApplicationClass> trees = Play.classes.getAssignableClasses(TreeDataHandler.class);
        for (ApplicationClasses.ApplicationClass tree : trees) {
            if (!Modifier.isAbstract(tree.javaClass.getModifiers())) {
                try {
                    Constructor c = tree.javaClass.getDeclaredConstructor();
                    TreeDataHandler t = (TreeDataHandler) c.newInstance();
                    String name = t.getName();
                    if (name == null) {
                        throw new RuntimeException("No valid name given for tree '" + tree.javaClass.getSimpleName() + "'. Are you sure you implemented getName() ?");
                    }
                    if (t instanceof AbstractTree) {
                        ((AbstractTree) t).init();
                    }
                    allTrees.put(name, t);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> findNameFields(Node node) {
        List<String> nameFields = new ArrayList<String>();
        Set<Field> fields = new HashSet<Field>();
        Java.findAllFields(node.getClass(), fields);
        for (Field f : fields) {
            if (f.isAnnotationPresent(NodeName.class)) {
                nameFields.add(f.getName());
            }
        }
        return nameFields;
    }

    public static TreeDataHandler getTree(String treeId) {
        TreeDataHandler tree = allTrees.get(treeId);
        if (tree == null) {
            throw new RuntimeException(String.format("Could not find implementation of tree '%s'.", treeId));
        }
        return tree;
    }

    @Override
    public void enhance(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
        new NodeEnhancer().enhanceThisClass(applicationClass);
    }

    @Override
    public void onEvent(String message, Object context) {

        // take care of updating associated TreeNode-s
        if (message.equals("JPASupport.objectUpdated") && Node.class.isAssignableFrom(context.getClass())) {
            Node node = (Node) context;
            List<String> nameFields = nameFieldCache.get(node);
            if (nameFields == null) {
                nameFields = findNameFields(node);
                nameFieldCache.put(node, nameFields);
            }
            if (!nameFields.isEmpty()) {
                // any of them will do, they all have the same value
                String nameField = nameFields.get(0);
                Class<?> treeNode = Play.classloader.loadApplicationClass("models.tree.jpa.TreeNode");
                try {
                    Method nameGetter = context.getClass().getMethod("get" + JavaExtensions.capFirst(nameField));
                    String name = (String) nameGetter.invoke(context);
                    AbstractTree.renameNode(node, name);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
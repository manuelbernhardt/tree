package tree;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Play;
import play.classloading.ApplicationClasses;
import tree.persistent.AbstractTree;

/**
 * Entry point for Tree operations.
 * /!\ Persistance operations via Hibernate rely on the Play! controller so any testing or outside operations need to take care of flushing and cache refresh.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class Tree {
    private final static Map<String, TreeDataHandler> allTrees = new HashMap<String, TreeDataHandler>();

    static {
        init();
    }

    /**
     * Initialization code called on Plugin load.
     */
    public static void init() {
        // initialize all trees
        List<ApplicationClasses.ApplicationClass> trees = Play.classes.getAssignableClasses(TreeDataHandler.class);
        for (ApplicationClasses.ApplicationClass tree : trees) {
            if (!tree.javaClass.getName().equals("tree.persistent.AbstractTree")) {
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

    public static TreeDataHandler getTree(String treeId) {
        TreeDataHandler tree = allTrees.get(treeId);
        if (tree == null) {
            throw new RuntimeException(String.format("Could not find implementation of tree '%s'.", treeId));
        }
        return tree;
    }


}

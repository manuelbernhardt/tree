import java.util.List;

import tree.persistent.AbstractTree;
import tree.JSTreeNode;
import tree.persistent.NodeType;
import tree.Tree;
import models.test.Folder;
import models.test.TestTree;
import tree.persistent.GenericTreeNode;
import models.tree.jpa.TreeNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.db.jpa.JPA;
import play.test.UnitTest;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class TreeTest extends UnitTest {

    private AbstractTree t;
    private Long c, data, games, admin, letters, d, movies, starwars, matrix;

    private static NodeType DRIVE;
    private static NodeType FOLDER;

    @Before
    public void setUp() {
        t = (AbstractTree) Tree.getTree("testTree");

        DRIVE = TestTree.getNodeType("drive");
        FOLDER = TestTree.getNodeType("folder");

        c = t.create(-1l, 0l, "C", DRIVE.getName(), null);
        data = t.create(c, 0l, "Data", FOLDER.getName(), null);
        games = t.create(c, 0l, "Games", FOLDER.getName(), null);
        admin = t.create(data, 0l, "Admin", FOLDER.getName(), null);
        letters = t.create(data, 0l, "Letters", FOLDER.getName(), null);

        d = t.create(-1l, 0l, "D", DRIVE.getName(), null);
        movies = t.create(d, 0l, "Movies", FOLDER.getName(), null);
        starwars = t.create(movies, 0l, "Starwars", FOLDER.getName(), null);
        matrix = t.create(movies, 0l, "The Matrix", FOLDER.getName(), null);
    }

    @After
    public void tearDown() throws Exception {
        AbstractTree t = (AbstractTree) Tree.getTree("testTree");
        List<? extends JSTreeNode> drives = t.getChildren(-1l, null);
        for(JSTreeNode d : drives) {
            t.remove(d.getId(), -1l, "", null);
        }

        t = null;
        c = d = data = games = admin = letters = movies = starwars = matrix = null;
    }

    @Test
    public void removeRecursively() {
        Long root = t.create(-1l, 0l, "Root", DRIVE.getName(), null);
        Long child1 = t.create(root, 0l, "Child 1", FOLDER.getName(), null);
        Long child2 = t.create(child1, 0l, "child 2", FOLDER.getName(), null);

        assertEquals(1, t.getChildren(root, null).size());
        assertEquals(1, t.getChildren(child1, null).size());

        try {
            t.remove(root, null, null, null);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        JPA.em().clear();

        assertNull(TreeNode.findById(root));
        assertNull(TreeNode.findById(child1));
        assertNull(TreeNode.findById(child2));
    }

    @Test
    public void rename() {
        t.rename(starwars, "Star Wars", null);
        // usually this is done by the controller
        JPA.em().clear();
        assertEquals("Star Wars", t.getChildren(movies, null).get(0).getName());
    }

    @Test
    public void testCopyWithObject() {
        TreeNode systemLibraries = TreeNode.findById(88l);
        TreeNode music = TreeNode.findById(81l);

        Tree.getTree("testTree").copy(data, starwars, 0l);

        JPA.em().flush();
        JPA.em().clear();

        List<? extends JSTreeNode> copied = Tree.getTree("testTree").getChildren(starwars, null);
        assertEquals(1, copied.size());

        Folder original = Folder.findById(data);
        Folder copy = Folder.findById((((GenericTreeNode)copied.get(0)).getNodeId()));
        assertEquals(original.getName(), copy.getName());
        assertNotSame(original.getId(), copy.getId());

        List<? extends JSTreeNode> children = Tree.getTree("testTree").getChildren(original.getId(), null);
        assertEquals(2, children.size());

        // TODO test if children are copied too

    }
}

package models.test;

import controllers.tree.AbstractTree;
import controllers.tree.NodeType;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class TestTree extends AbstractTree {

    @Override
    protected NodeType[] getNodes() {
        return new NodeType[]{type(Drive.class, true), type(Folder.class, true)};
    }

    @Override
    protected NodeType getDefaultType() {
        return getNodeType(Folder.class);
    }

    @Override
    protected NodeType getRootType() {
        return getNodeType(Drive.class);
    }
}

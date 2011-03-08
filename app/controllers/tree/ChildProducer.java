package controllers.tree;

import java.util.List;

import models.tree.JSTreeNode;

/**
 * Producer for children of tree nodes
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public interface ChildProducer {

    List<JSTreeNode> produce(Long id);
}

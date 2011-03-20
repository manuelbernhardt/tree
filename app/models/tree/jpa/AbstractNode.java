package models.tree.jpa;

import javax.persistence.MappedSuperclass;

import tree.persistent.Node;
import tree.persistent.NodeName;
import play.db.jpa.Model;

/**
 * Template class for a {@link Node}
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@MappedSuperclass
public class AbstractNode extends Model implements Node {

    @NodeName
    public String name;

    public String getName() {
        return name;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
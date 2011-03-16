package models.tree.jpa;

import javax.persistence.MappedSuperclass;

import models.tree.Node;
import play.db.jpa.Model;

/**
 * Template class for a {@link Node}
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@MappedSuperclass
public class AbstractNode extends Model implements Node {

    public String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
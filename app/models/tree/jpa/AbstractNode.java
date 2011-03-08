package models.tree.jpa;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import models.tree.Node;
import play.db.jpa.Model;

/**
 * Extend this template class if you don't want to re-implement the Node interface and are fine with a joined inheritance strategy
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
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
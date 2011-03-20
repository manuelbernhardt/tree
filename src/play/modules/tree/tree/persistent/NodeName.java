package tree.persistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a String field as being a name of a {@link Node}, thus causing it to be renamed when the tree.persistent.Node is renamed.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface NodeName {

}

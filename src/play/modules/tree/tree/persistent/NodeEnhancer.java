package tree.persistent;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import play.classloading.ApplicationClasses;
import play.classloading.enhancers.Enhancer;

/**
 * Enhancer that takes care of renaming TreeNodes associated to {@link Node} classes having a {@link NodeName} annotation on at least one of their properties
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class NodeEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        // enhance only tree.persistent.Node models that are JPA models
        boolean isNode = false;
        for (CtClass i : ctClass.getInterfaces()) {
            if (i.getName().equals("tree.persistent.Node")) {
                isNode = true;
                break;
            }
        }
        if (!isNode) {
            return;
        }
        if (!ctClass.subtypeOf(classPool.get("play.db.jpa.JPABase"))) {
            return;
        }
        if (!hasAnnotation(ctClass, "javax.persistence.Entity")) {
            return;
        }

        // locate fields annotated with @tree.persistent.NodeName and insert code that causes renaming of the associated TreeNode
        for (CtField f : ctClass.getDeclaredFields()) {
            if (hasAnnotation(f, "tree.persistent.NodeName")) {
                String propertyName = f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1);
                String setter = "set" + propertyName;

                try {
                    // we trust that we'll always find this because otherwise the PropertyEnhancer of Play would already have complained
                    CtMethod ctMethod = ctClass.getDeclaredMethod(setter);
                    ctMethod.insertBefore("models.tree.jpa.TreeNode.rename($0, $1);");

                } catch (NotFoundException noSetter) {
                    throw new NotFoundException("Could not find setter for property " + propertyName + " of Node class " + applicationClass.name);
                }
            }
        }

        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }
}

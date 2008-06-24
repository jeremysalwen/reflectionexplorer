package lardmaster.reflection_explorer;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeModel;
import java.lang.reflect.Field;
import java.util.Vector;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jun 23, 2008
 * Time: 8:00:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class lard_tree extends JTree {
    public String convertValueToText(Object value, boolean selected,
                                     boolean expanded, boolean leaf, int row,
                                     boolean hasFocus) {
        if (value == null) {
            return "-*null value*-";
        }
        if (value instanceof Field) {
            Field field = (Field) value;
            return field.getName();
        } else {
            return value.toString();
        }
    }

    public lard_tree() {
    }

    public lard_tree(Object[] value) {
        super(value);
    }

    public lard_tree(Vector<?> value) {
        super(value);
    }

    public lard_tree(Hashtable<?, ?> value) {
        super(value);
    }

    public lard_tree(TreeNode root) {
        super(root);
    }

    public lard_tree(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }

    public lard_tree(TreeModel newModel) {
        super(newModel);
    }
}

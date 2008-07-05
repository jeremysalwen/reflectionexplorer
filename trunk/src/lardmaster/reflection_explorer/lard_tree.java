package lardmaster.reflection_explorer;

import javax.swing.*;
import java.lang.reflect.Field;

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
    public void add_base_object(Object o) {
        ((lard_tree_model)treeModel).add_base_object(o);
    }
    public lard_tree(lard_tree_model newModel) {
        super(newModel);
    }
}

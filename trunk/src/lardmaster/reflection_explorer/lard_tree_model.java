package lardmaster.reflection_explorer;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jun 23, 2008
 * Time: 6:26:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class lard_tree_model implements TreeModel {
    Object root;  //could be a Class, Field, or Object

    public lard_tree_model(Object o) {
        root = o;
    }

    public Object getRoot() {
        return root;
    }

    private Class get_node_class(Object node) {
        if (node instanceof Field) {
            Field node_to_field = (Field) node;
            return node_to_field.getType();
        } else if (node instanceof Class) {
            return (Class) node;
        } else {
            return node.getClass();
        }
    }

    private Field[] get_node_fields(Object node) {
        ArrayList<Field> fields = new ArrayList<Field>();
        for (Class c = get_node_class(node); c != null; c = c.getSuperclass()) {
            fields.ensureCapacity(fields.size() + c.getDeclaredFields().length);
            for (Field f : c.getDeclaredFields()) {
                fields.add(f);
            }
        }
        Field[] result=new Field[fields.size()];
        fields.toArray(result);
        return result;
    }

    public Object getChild(Object parent, int index) {
        return get_node_fields(parent)[index];
    }

    public int getChildCount(Object parent) {
        int total = 0;
        for (Class c = get_node_class(parent); c != null; c = c.getSuperclass()) {
            total += c.getDeclaredFields().length;
        }
        return total;
    }

    public boolean isLeaf(Object node) {
        Class type = get_node_class(node);
        return type == String.class || type == Integer.class || type == Double.class || type == Long.class || type == Float.class || type == Short.class || type == Character.class || type == Boolean.class || type == Character.class || type.isPrimitive();
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        //no need
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) {
            return -1;
        }
        Field parent_to_field = (Field) parent;
        Field child_to_field = (Field) child;
        Field[] parent_fields = parent_to_field.getType().getFields();
        for (int i = 0; i < parent_fields.length; i++) {
            if (parent_fields[i] == child_to_field) {
                return i;
            }
        }
        return -1;
    }

    public void addTreeModelListener(TreeModelListener l) {
    }

    public void removeTreeModelListener(TreeModelListener l) {
    }
}

package lardmaster.reflection_explorer;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jun 23, 2008
 * Time: 6:26:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class lard_tree_model implements TreeModel {
    Vector root;  //could be a Class, Field, or Object

    public lard_tree_model(Object base) {
        root = new Vector();
        root.add(base);
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
                if (!(Modifier.isStatic(f.getModifiers()) ^ node instanceof Class)) {
                    fields.add(f);
                }
            }
        }
        Field[] result = new Field[fields.size()];
        fields.toArray(result);
        return result;
    }

    public Object getChild(Object parent, int index) {
        if (parent instanceof Vector) {
            System.out.println("getting child " + index);
            return ((Vector) parent).get(index);
        }
        return get_node_fields(parent)[index];
    }

    public int getChildCount(Object parent) {
        if (parent instanceof Vector) {
            System.out.println("root size is " + ((Vector) parent).size());
            return ((Vector) parent).size();
        }
        int total = 0;
        for (Class c = get_node_class(parent); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!(Modifier.isStatic(f.getModifiers()) ^ parent instanceof Class)) {
                    total++;
                }
            }
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
        if (parent instanceof Vector) {
            return ((Vector) parent).indexOf(child);
        }
        Field child_to_field = (Field) child;
        Field[] parent_fields = get_node_fields(parent);
        for (int i = 0; i < parent_fields.length; i++) {
            if (parent_fields[i] == child_to_field) {
                return i;
            }
        }
        return -1;
    }

    public void add_base_object(Object o) {
        if (root.contains(o)) {
            return;
        }
        //root is a Vector
        root.add(o);
        // fireTreeNodesInserted(new TreeModelEvent(this, new Object[]{root},new int[] {root.size()}, new Object[] {o}));
        fireTreeStructureChanged(new TreeModelEvent(this, new Object[]{root}));
    }

    private Vector<TreeModelListener> vector = new Vector<TreeModelListener>();

    public void addTreeModelListener(TreeModelListener listener) {
        if (listener != null && !vector.contains(listener)) {
            vector.addElement(listener);
        }
    }

    public void removeTreeModelListener(TreeModelListener listener) {
        if (listener != null) {
            vector.removeElement(listener);
        }
    }

    public void fireTreeNodesChanged(TreeModelEvent e) {
        Enumeration<TreeModelListener> listeners = vector.elements();
        while (listeners.hasMoreElements()) {
            TreeModelListener listener = listeners.nextElement();
            listener.treeNodesChanged(e);
        }
    }

    public void fireTreeNodesInserted(TreeModelEvent e) {
        Enumeration<TreeModelListener> listeners = vector.elements();
        while (listeners.hasMoreElements()) {
            TreeModelListener listener = listeners.nextElement();
            listener.treeNodesInserted(e);
        }
    }

    public void fireTreeNodesRemoved(TreeModelEvent e) {
        Enumeration<TreeModelListener> listeners = vector.elements();
        while (listeners.hasMoreElements()) {
            TreeModelListener listener = listeners.nextElement();
            listener.treeNodesRemoved(e);
        }
    }

    public void fireTreeStructureChanged(TreeModelEvent e) {
        Enumeration<TreeModelListener> listeners = vector.elements();
        while (listeners.hasMoreElements()) {
            TreeModelListener listener = listeners.nextElement();
            listener.treeStructureChanged(e);
        }
    }
}

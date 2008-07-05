package lardmaster.reflection_explorer;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Stack;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jun 21, 2008
 * Time: 1:18:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class reflection_explorer implements TreeSelectionListener, ActionListener, KeyListener {
    public reflection_explorer() {
        selectFieldTree.addTreeSelectionListener(this);
        callToStringOnSelectedButton.addActionListener(this);
        addButton.addActionListener(this);
        ClassSelectionFormattedTextField.addKeyListener(this);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("reflection_explorer");
        frame.setContentPane(new reflection_explorer().panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }


    private JPanel panel;
    private JTree selectFieldTree;
    private JTextPane valueTextPane;
    private JTextArea TypeTextArea;
    private JButton callToStringOnSelectedButton;
    private JTextField ClassSelectionFormattedTextField;
    private JButton addButton;
    private JSplitPane splitpane;
    private JRadioButton generatedToStringRadioButton;
    private JRadioButton classSToStringRadioButton;

    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = e.getPath();
        Object value = get_value(path);
        String text;
        Class value_class = value.getClass();
        try {
            if (classSToStringRadioButton.isSelected() || value_class == Boolean.class || value_class == Integer.class || value_class == String.class || value_class == Long.class || value_class == Short.class || value_class == Float.class || value_class == Byte.class || value_class == Character.class) {
                text = value.toString();
            } else {
                text = get_generated_tostring(value);
            }
        } catch (Exception exc) {
            text = exc.getMessage();
        }
        this.valueTextPane.setText(text);
        this.TypeTextArea.setText(value_class.toString());
    }

    private String get_generated_tostring(Object o) {
        StringBuilder result = new StringBuilder();
        append_generated_tostring(o, result, new Stack());
        return result.toString();
    }

    private void append_generated_tostring(Object o, StringBuilder stream, Stack parent_stack) {
        if (o == null) {
            stream.append("null");
            return;
        }
        Class o_class = o.getClass();
        if (o_class == Boolean.class || o_class == Integer.class || o_class == String.class || o_class == Long.class || o_class == Short.class || o_class == Float.class || o_class == Byte.class || o_class == Character.class) {
            stream.append(o);
            return;
        }
        parent_stack.push(o);
        stream.append("[\n");
        for (Class c = o_class; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    if (Modifier.isStatic(f.getModifiers()) ^ o_class == Class.class) {
                        continue;
                    }
                    stream.append(f.getName());
                    stream.append('=');
                    Object child_object = f.get(o);
                    int index = parent_stack.indexOf(child_object);
                    if (index == -1) {
                        append_generated_tostring(child_object, stream, parent_stack);
                    } else {
                        stream.append("reference to parent ");
                        stream.append(index);
                        stream.append(" iterations ago");
                    }
                    stream.append('\n');
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        stream.append(']');
        parent_stack.pop();
    }

    private Object get_value(TreePath path) {
        if (path.getLastPathComponent() instanceof Field) {
            Field f = (Field) path.getLastPathComponent();
            f.setAccessible(true);
            try {
                Object value = get_value(path.getParentPath());
                if (value instanceof String) {
                    return value;
                }
                value = f.get(value);
                if (value == null) {
                    value = f + " is null";
                }
                return value;
            } catch (IllegalAccessException e) {
                return e;
            }
        } else {
            return path.getLastPathComponent();         //if it is a class, then it will be a static field acess and the value wont matter anyways
        }
    }

    private void createUIComponents() {
        selectFieldTree = new lard_tree(new lard_tree_model(this));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.addButton) {
            try {
                ((lard_tree) this.selectFieldTree).add_base_object(Class.forName(this.ClassSelectionFormattedTextField.getText()));
            } catch (ClassNotFoundException e1) {
                valueTextPane.setText("");
                StringWriter swriter = new StringWriter();
                e1.printStackTrace(new PrintWriter(swriter, true));
                valueTextPane.setText(swriter.toString());
            }
        }
        if (e.getSource() == this.callToStringOnSelectedButton) {
            TreePath path = this.selectFieldTree.getSelectionPath();
            if (path != null) {
                valueTextPane.setText(get_value(path).toString());
            }
        }
    }

    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == '\n') {
            addButton.doClick();
        }
    }

    public void keyPressed(KeyEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void keyReleased(KeyEvent e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

package lardmaster.reflection_explorer;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jun 21, 2008
 * Time: 1:18:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class reflection_explorer implements TreeSelectionListener, ActionListener {
    public reflection_explorer() {
        selectFieldTree.addTreeSelectionListener(this);
        callToStringOnSelectedButton.addActionListener(this);
        setInspectedClassButton.addActionListener(this);
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
    private JCheckBox automaticToStringCallsCheckBox;
    private JButton callToStringOnSelectedButton;
    private JFormattedTextField ClassSelectionFormattedTextField;
    private JButton setInspectedClassButton;
    private JSplitPane splitpane;

    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = e.getPath();
        Object value = get_value(path);

        if (value == null) {
            this.valueTextPane.setText("null");
            if (e.getPath().getLastPathComponent() instanceof Field) {
                this.TypeTextArea.setText(((Field) e.getPath().getLastPathComponent()).getType().toString());
            } else {
                this.TypeTextArea.setText("Null type ??¿¿!!¡¡ :Þ");
            }
            return;
        }
        String text;
        Class value_class = value.getClass();
        try {
            if (automaticToStringCallsCheckBox.isSelected() || value_class == Boolean.class || value_class == Integer.class || value_class == String.class || value_class == Long.class || value_class == Short.class || value_class == Float.class || value_class == Byte.class || value_class == Character.class) {
                text = value.toString();
            } else {
                text = "ToString not called yet.  Try investigating lower fields or calling toString on this item";
            }
        } catch (Exception exc) {
            text = exc.getMessage();
        }
        this.valueTextPane.setText(text);
        this.TypeTextArea.setText(value_class.toString());
    }

    private Object get_value(TreePath path) {
        if (path.getLastPathComponent() instanceof Field) {
            Field f = (Field) path.getLastPathComponent();
            f.setAccessible(true);
            try {
                return f.get(get_value(path.getParentPath()));
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
        if (e.getSource() == this.setInspectedClassButton) {
            try {
                this.selectFieldTree = new lard_tree((new lard_tree_model(Class.forName(this.ClassSelectionFormattedTextField.getText()))));
                splitpane.setTopComponent(this.selectFieldTree);
                this.panel.updateUI();
            } catch (ClassNotFoundException e1) {
                valueTextPane.setText(e1.getMessage());
            }
        }
        if (e.getSource() == this.callToStringOnSelectedButton) {
            valueTextPane.setText(get_value(this.selectFieldTree.getSelectionPath()).toString());
        }
    }
}

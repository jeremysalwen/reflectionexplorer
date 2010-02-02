package org.jeremy.reflection_explorer;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jun 21, 2008
 * Time: 1:18:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class reflection_explorer implements TreeSelectionListener, ActionListener, KeyListener {
    public reflection_explorer() {
        safe_paths = new DefaultListModel();
        safe_paths.addElement("/System/Library/");  //Assuming we are not trying to avoid detection by system classes.
        safe_paths.addElement("/usr/lib/");
        selectFieldTree.addTreeSelectionListener(this);
        menu = new JMenuBar();
        JMenu settings = new JMenu("toString Settings");
        ButtonGroup StringGenerationMethod = new ButtonGroup();
        generatedToStringRadioButton = new JRadioButtonMenuItem("Generated toString");
        classSToStringRadioButton = new JRadioButtonMenuItem("Class's toString");
        generatedToStringRadioButton.addActionListener(this);
        classSToStringRadioButton.addActionListener(this);
        StringGenerationMethod.add(generatedToStringRadioButton);
        StringGenerationMethod.add(classSToStringRadioButton);
        generatedToStringRadioButton.setSelected(true);
        edit_safe_button = new JMenuItem("Edit \"safe\" classes");
        edit_safe_button.addActionListener(this);

        safePathsSelector = new safe_paths_selector();
        settings.add(generatedToStringRadioButton);
        settings.add(classSToStringRadioButton);
        settings.add(edit_safe_button);


        JMenu inspected_classes = new JMenu("Inpsected Classes");
        addButton = new JMenuItem("Inspect another class");
        addButton.addActionListener(this);
        inspected_classes.add(addButton);
        remove_selected = new JMenuItem("Stop inspecting selected item");
        remove_selected.addActionListener(this);
        inspected_classes.add(remove_selected);
        menu.add(settings);
        menu.add(inspected_classes);

        newSearchButton.addActionListener(this);
        refineSearchButton.addActionListener(this);

    }

    public static void main(String[] args) throws IllegalAccessException, UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("reflection_explorer");
        reflection_explorer r = new reflection_explorer();
        frame.setContentPane(r.panel);
        frame.setJMenuBar(r.menu);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public JMenuBar menu;
    private JPanel panel;
    private JTree selectFieldTree;
    private JTextPane valueTextPane;
    private JTextArea TypeTextArea;
    private JButton callToStringOnSelectedButton;
    private JMenuItem addButton;
    private JRadioButtonMenuItem generatedToStringRadioButton;
    private JRadioButtonMenuItem classSToStringRadioButton;
    private JTextArea SearchBox;
    private JButton newSearchButton;
    private JList FoundItemsList;
    private JButton refineSearchButton;
    private JSplitPane splitpane;
    static Pattern quoted = Pattern.compile("\".*\"", Pattern.DOTALL);
    private JMenuItem remove_selected;
    private safe_paths_selector safePathsSelector;
    private JMenuItem edit_safe_button;
    DefaultListModel searchResults;

    Object string_to_value(String s) {
        s = s.trim().toLowerCase();
        if ("true".equals(s))
            return true;
        if ("false".equals(s))
            return false;
        try {
            return NumberFormat.getNumberInstance().parse(s);
        } catch (NumberFormatException ignored) {
        } catch (ParseException ignored) {
        }
        Matcher m = quoted.matcher(s);
        if (m.matches()) {
            return m.replaceFirst("\\1");
        }
        valueTextPane.setText("Could not parse value:\n" + s);
        return null;
    }

    public void valueChanged(TreeSelectionEvent e) {
        TreePath path = e.getPath();
        Object value = get_value(path);
        String text;
        Class value_class = value.getClass();
        if (classSToStringRadioButton.isSelected() || value_class == Boolean.class || value_class == Integer.class || value_class == String.class || value_class == Long.class || value_class == Short.class || value_class == Float.class || value_class == Byte.class || value_class == Character.class) {
            text = value.toString();
        } else {
            text = getGeneratedString(value);
        }
        this.valueTextPane.setText(text);
        Object lastComponent = path.getLastPathComponent();
        Class type;
        if (lastComponent instanceof Field) {
            type = ((Field) lastComponent).getType();
        } else {
            type = lastComponent.getClass();
        }
        this.TypeTextArea.setText(type.getName());
    }

    private String getGeneratedString(Object o) {
        StringBuilder result = new StringBuilder();
        appendGeneratedString(o, result, new Stack<Object>());
        return result.toString();
    }

    //Thanks to avalys at stackoverflow.com

    public static URL getClassURL(Class klass) {
        String name = klass.getName();
        name = "/" + convertClassToPath(name);
        return klass.getResource(name);
    }

    public static String convertClassToPath(String className) {
        return className.replace('.', '/') + ".class";
    }

    static Pattern remove_URL_Junk = Pattern.compile(".*?:?(.*)", Pattern.DOTALL);

    public boolean safeToCall_toString(Class c) {
        URL resource = getClassURL(c);
        if (resource == null) {
            return false;
        }
        String loc = resource.getPath();
        if (loc.startsWith("file:")) {
            loc = loc.substring(5);
        }
        for (Object s : safe_paths.toArray()) {
            if (loc.startsWith((String) s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively prints out a string which describes the contents of object o.
     *
     * @param o            The object to examine
     * @param stream       The stream to which the text is appended
     * @param parent_stack The list of parent objects.  This is for cases in which fields reference the objects they are contained in.  By using a list of objects which this context is "contained in", we can catch self-referential loops early.
     */
    private void appendGeneratedString(Object o, StringBuilder stream, Stack<Object> parent_stack) {
        if (o == null) {
            stream.append("null");
            return;
        }
        Class o_class = o.getClass();
        if (o_class == Boolean.class || o_class == Integer.class || o_class == String.class || o_class == Long.class || o_class == Short.class || o_class == Float.class || o_class == Byte.class || o_class == Character.class) {
            stream.append(o);
            return;
        }
        if (safeToCall_toString(o_class)) {
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
                        appendGeneratedString(child_object, stream, parent_stack);
                    } else {
                        stream.append("reference to parent ");
                        stream.append(index);
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

    /**
     * This method converts a TreePath which is the item selected
     * in the reflection tree into the value of the object it is referring to.
     *
     * @param path
     * @return
     */
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

    public static boolean is_primitive(Class type) {
        return type == String.class || type == Integer.class || type == Double.class || type == Long.class || type == Float.class || type == Short.class || type == Character.class || type == Boolean.class || type == Character.class || type.isPrimitive();
    }

    public void search(Object value, DefaultListModel out) {
        Vector<Object> bases = ((reflection_tree_model) selectFieldTree.getModel()).root;
        for (int i = 0; i < bases.size(); i++) {
            search(bases.get(i), value, new Stack(), out, new SearchResult(i, new Stack<Field>()));
        }
    }

    int lol = 0;

    /**
     * This method searches recursively through the Object "root" for the value "value".
     *
     * @param root           The object to search through
     * @param value          The value to search for
     * @param parents        The list of parent objects, to prevent self-referential loops
     * @param out            The list to output the results to.
     * @param parent_history Extra stack information about the depth of the search,
     *                       so an absolute location of the found item can be given.
     */
    public void search(Object root, Object value, Stack parents, DefaultListModel out, SearchResult parent_history) {

        parents.push(root);
        for (Class c = root.getClass(); c != null; c = c.getSuperclass()) {
            field_loop:
            for (Field f : c.getDeclaredFields()) {
                if (!(Modifier.isStatic(f.getModifiers()) ^ root instanceof Class)) {
                    f.setAccessible(true);
                    try {
                        Object checking = f.get(root);
                        if (checking != null) {
                            System.out.println(lol++);
                        }
                        if (checking == null) {
                            continue;
                        }
                        if (f.getType().isArray()) {
                            for (int i = 0; i < Array.getLength(checking); i++) {
                                Object o = Array.get(checking, i);
                                if (value.equals(checking)) {
                                    //TODO  Clearly our SearchResult model doesn't apply to arrays.
                                }
                            }
                        }
                        if (is_primitive(f.getType())) {
                            if (value.equals(checking)) {

                                SearchResult result = new SearchResult(parent_history.item_number, (Stack<Field>) parent_history.fields.clone());
                                result.fields.add(f);
                                out.addElement(result);
                            }
                        } else {
                            for (Object o : parents) {
                                if (o == checking) {
                                    continue field_loop;
                                }
                            }
                            parent_history.fields.add(f);
                            search(checking, value, parents, out, parent_history);     //This is key!
                            parent_history.fields.remove(parent_history.fields.size() - 1);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        parents.pop();
    }

    private void createUIComponents
            () {
        selectFieldTree = new reflection_tree(new reflection_tree_model(this));
        searchResults = new DefaultListModel();
        this.FoundItemsList = new JList(searchResults);
    }

    public void actionPerformed
            (ActionEvent
                    e) {
        if (e.getSource() == this.addButton) {
            try {
                ((reflection_tree) this.selectFieldTree).add_base_object(Class.forName(JOptionPane.showInputDialog("Enter Class Name")));
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
        if (e.getSource() == this.classSToStringRadioButton) {
            callToStringOnSelectedButton.setVisible(false);
        }
        if (e.getSource() == this.generatedToStringRadioButton) {
            callToStringOnSelectedButton.setVisible(true);
        }
        if (e.getSource() == this.newSearchButton) {
            searchResults.clear();
            search();
        }
        if (e.getSource() == this.refineSearchButton) {
            refine_search();
        }
        if (e.getSource() == this.remove_selected) {
            int[] selected = this.selectFieldTree.getSelectionRows();
            if (selected == null) {
                JOptionPane.showMessageDialog(this.remove_selected, "No item selected to remove!");

            } else {
                int offset = 0;
                for (int i : selected) {
                    ((reflection_tree_model) ((reflection_tree) selectFieldTree).getModel()).root.remove(i - (offset++));
                }
            }
        }
        if (e.getSource() == this.edit_safe_button) {
            safePathsSelector.pack();
            this.safePathsSelector.setVisible(true);
        }

    }

    /**
     * This will remove all the results in the search result list which do not
     * currently match the value entered into the search box.
     */
    public void refine_search
            () {
        Object value = string_to_value(this.SearchBox.getText());
        Vector root = ((reflection_tree_model) selectFieldTree.getModel()).root;
        int offset = 0;
        for (int i = 0; i < searchResults.size(); i++) {
            SearchResult result = (SearchResult) searchResults.get(i);
            Object currentvalue = result.get_value(root);
            if (!value.equals(currentvalue)) {
                root.remove(i - offset++);
            }
        }
    }

    public void search
            () {
        Object value = string_to_value(this.SearchBox.getText());
        search(value, searchResults);
    }

    public void keyTyped
            (KeyEvent
                    e) {
        //Maybe
    }

    public void keyPressed
            (KeyEvent
                    e) {
    }

    public void keyReleased
            (KeyEvent
                    e) {
    }

    DefaultListModel safe_paths;

    /**
     * This class is the dialog which pops up allowing users to specify which locations in the filesystem
     * are considered "safe" i.e. any classes loaded from these locations will have their "toString" method
     * called automatically, without fear of detection
     */
    class safe_paths_selector extends JDialog implements ActionListener {
        JList safe_list;
        JButton remove;
        JButton add;

        public safe_paths_selector() {
            setTitle("Safe class locations");
            setLayout(new BorderLayout());
            remove = new JButton("Selection is not safe");
            remove.addActionListener(this);
            add = new JButton("Add safe class folder");
            add.addActionListener(this);
            JToolBar top = new JToolBar();
            top.add(add);
            top.add(remove);
            safe_list = new JList();
            safe_list.setModel(safe_paths);
            this.add(new JScrollPane(safe_list), BorderLayout.CENTER);
            this.add(top, BorderLayout.PAGE_START);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (actionEvent.getSource() == remove) {
                int[] indexes = safe_list.getSelectedIndices();
                int offset = 0;
                for (int i : indexes) {
                    safe_paths.remove(i - offset);
                    offset++;
                }
            }
            if (actionEvent.getSource() == this.add) {
                String safe = JOptionPane.showInputDialog("Enter file prefix for classes which it is safe to call toString on");
                safe_paths.addElement(safe);
            }
        }
    }
}

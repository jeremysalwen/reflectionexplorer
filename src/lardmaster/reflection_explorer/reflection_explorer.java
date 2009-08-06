package lardmaster.reflection_explorer;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * Created by IntelliJ IDEA.
 * User:
 * Date: Jun 21, 2008
 * Time: 1:18:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class reflection_explorer implements TreeSelectionListener, ActionListener, KeyListener {
    public reflection_explorer() {
        safe_paths=new DefaultListModel();
        safe_paths.addElement("/System/Library/");
        selectFieldTree.addTreeSelectionListener(this);
        menu = new JMenuBar();
        JMenu settings = new JMenu("toString Settings");
        ButtonGroup tostringmethod = new ButtonGroup();
        generatedToStringRadioButton = new JRadioButtonMenuItem("Generated toString");
        classSToStringRadioButton = new JRadioButtonMenuItem("Class's toString");
        generatedToStringRadioButton.addActionListener(this);
        classSToStringRadioButton.addActionListener(this);
        tostringmethod.add(generatedToStringRadioButton);
        tostringmethod.add(classSToStringRadioButton);
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
    private JSplitPane splitpane;
    private JTextArea SearchBox;
    private JButton newSearchButton;
    private JList FoundItemsList;
    private JButton refineSearchButton;
    List<List<Field>> searchResults;
    static Pattern quoted = Pattern.compile("\\\".*\\\"", Pattern.DOTALL);
    private JMenuItem remove_selected;
    private safe_paths_selector safePathsSelector;
    private JMenuItem edit_safe_button;

    Object string_to_value(String s) {
        s = s.trim().toLowerCase();
        if ("true".equals(s))
            return true;
        if ("false".equals(s))
            return false;
        try {
            return NumberFormat.getNumberInstance().parse(s);
        } catch (NumberFormatException e) {
        } catch (ParseException e) {
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
            text = get_generated_tostring(value);
        }
        this.valueTextPane.setText(text);
        Object lastcomponent=path.getLastPathComponent();
        Class type;
        if(lastcomponent instanceof Field) {
            type=((Field)lastcomponent).getType();
        } else {
            type=lastcomponent.getClass();
        }
        this.TypeTextArea.setText(type.getName());
    }

    private String get_generated_tostring(Object o) {
        StringBuilder result = new StringBuilder();
        append_generated_tostring(o, result, new Stack());
        return result.toString();
    }

    //Thanks to avalys at stackoverflow.com
    public static URL getClassURL(Class klass) {
        String name = klass.getName();
        name = "/" + convertClassToPath(name);
        URL url = klass.getResource(name);
        return url;
    }

    public static String convertClassToPath(String className) {
        String path = className.replace('.', '/') + ".class";
        return path;
    }

    static Pattern remove_URL_Junk = Pattern.compile(".*?:?(.*)", Pattern.DOTALL);

    public boolean safe_to_call_tostring(Class c) {
        URL resource = getClassURL(c);
        if (resource == null) {
            return false;
        }
        String loc = resource.getPath();
        if (loc.startsWith("file:")) {
            loc = loc.substring(5);
        }
        for (Object s : safe_paths.toArray()) {
            if (loc.startsWith((String)s)) {
                return true;
            }
        }
        return false;
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
        if (safe_to_call_tostring(o_class)) {
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

    public static boolean is_primitive(Class type) {
        return type == String.class || type == Integer.class || type == Double.class || type == Long.class || type == Float.class || type == Short.class || type == Character.class || type == Boolean.class || type == Character.class || type.isPrimitive();
    }

    public List<List<Field>> search(Object root, Object value) {
        return search(root, value, new Stack());
    }

    public List<List<Field>> search(Object root, Object value, Stack parents) {
        List<List<Field>> results = new ArrayList<List<Field>>();
        if (!(root instanceof Class)) {
            root = root.getClass();
        }
        parents.push(root);
        for (Class c = root.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!(Modifier.isStatic(f.getModifiers()) ^ root instanceof Class)) {
                    f.setAccessible(true);
                    try {
                        Object checking = f.get(root);
                        if (is_primitive(checking.getClass())) {
                            if (value.equals(checking)) {
                                List<Field> result = new ArrayList<Field>();
                                result.add(f);
                                results.add(result);
                            }
                        } else {
                            if (parents.contains(checking)) {
                                continue;
                            }
                            for (List<Field> l : search(checking, value, parents)) {
                                l.add(f);
                                results.add(l);
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        parents.pop();
        return results;
    }

    private void createUIComponents() {
        selectFieldTree = new lard_tree(new lard_tree_model(this));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.addButton) {
            try {
                ((lard_tree) this.selectFieldTree).add_base_object(Class.forName(JOptionPane.showInputDialog("Enter Class Name")));
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
            search();
        }
        if (e.getSource() == this.remove_selected) {
            int[] selected = this.selectFieldTree.getSelectionRows();
            if (selected == null) {
                JOptionPane.showMessageDialog(this.remove_selected, "No item selected to remove!");

            } else {
                int offset = 0;
                for (int i : selected) {
                    ((lard_tree_model) ((lard_tree) selectFieldTree).getModel()).root.remove(-(offset++));
                }
            }
        }
        if(e.getSource()==this.edit_safe_button) {
            safePathsSelector.pack();
            this.safePathsSelector.setVisible(true);
        }

    }
    public void get_value_reverse_field_list(List<>)
    public void refine_search() {
        Object value = string_to_value(this.SearchBox.getText());
        for (int i = 0; i < searchResults.size(); i++) {
            if(searchResults.get(i))
        }
    }

    public void search() {
        Object value = string_to_value(this.SearchBox.getText());

    }

    public void keyTyped(KeyEvent e) {
        //Maybe
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

   DefaultListModel safe_paths;
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
            this.add(new JScrollPane(safe_list),BorderLayout.CENTER);
            this.add(top,BorderLayout.PAGE_START);
        }

        public void actionPerformed(ActionEvent actionEvent) {
               if(actionEvent.getSource()==remove) {
                  int[] indexes= safe_list.getSelectedIndices();
                   int offset=0;
                   for(int i:indexes) {
                       safe_paths.remove(i-offset);
                       offset++;
                   }
               }
               if(actionEvent.getSource()== add) {
                   String safe=JOptionPane.showInputDialog("Enter file prefix for classes which it is safe to call tostring on");
                   safe_paths.addElement(safe);
               }
        }
    }
}

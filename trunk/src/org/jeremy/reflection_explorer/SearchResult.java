package org.jeremy.reflection_explorer;

import java.lang.reflect.Field;
import java.util.Stack;
import java.util.Vector;


public class SearchResult {
    public Stack<Field> fields;
    public int item_number;

    public SearchResult(int i, Stack<Field> fields) {
        this.fields = fields;
        this.item_number = i;
    }

    public Object get_value(Vector root) {
        Object base = root.get(item_number);
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                base = f.get(base);
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return base;
    }
}

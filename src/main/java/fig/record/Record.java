package fig.record;

import fig.basic.IOUtils;
import fig.basic.ListUtils;

import java.io.PrintWriter;
import java.util.List;

/**
 * Record is a static class for instrumenting the state of an execution. The calls record the state of variables at
 * certain points of program execution. The calls can be grouped hierarchically as well. Basic usage: init(record file
 * to output to) begin(key, value) add(key, value) end() finish() Caching feature (not fully supported): Recordable
 * objects can be cached, so when they are added a second time, only a pointer is written to file.
 * <p/>
 * The record file encodes a tree, where each node of the tree consists of a key and optionally a value, and a list of
 * children nodes. In the file, each line specifies a node: <\t * D, where D is depth of node><key>\t<value>
 */
public class Record {
    public static void init(String path) {
        out = IOUtils.openOutEasy(path);
    }

    public static void finish() {
        if (out != null) out.close();
    }

    public static void flush() {
        if (out != null) out.flush();
    }

    private static void print(Object o) {
        if (out == null) return;
        for (int i = 0; i < indent; i++) out.print('\t');
        out.println(o + "");
    }

    public static void setStruct(Object... keys) {
        addTabSepValues(".struct", keys); // Treated specially: enable the structure
    }

    public static void clearStruct() {
        add(".struct"); // Treated specially: disable the structure
    }

    public static void add(String key, Object... val) {
        addTabSepValues(key, val);
    }

    public static <T> void addArray(String key, int[] values) {
        addArray(key, ListUtils.toObjArray(values));
    }

    public static <T> void addArray(String key, double[] values) {
        addArray(key, ListUtils.toObjArray(values));
    }

    public static <T> void addArray(String key, T[] values) {
        StringBuilder buf = new StringBuilder();
        buf.append(".array\t");
        buf.append(key);
        for (T value : values) {
            buf.append('\t');
            buf.append(value);
        }
        print(buf.toString());
    }

    public static void addArray(String key, List values) {
        StringBuilder buf = new StringBuilder();
        buf.append(".array\t");
        buf.append(key);
        for (Object value : values) {
            buf.append('\t');
            buf.append(value);
        }
        print(buf.toString());
    }

    public synchronized static void begin(String key) {
        add(key);
        indent++;
    }

    public synchronized static void begin(String key, Object val) {
        add(key, val);
        indent++;
    }

    public synchronized static void end() {
        indent--;
        flush();
    }

    // Add a as the key, b as the list of things
    private static void addTabSepValues(String a, Object[] b) {
        StringBuilder buf = new StringBuilder();
        buf.append(a);
        for (Object o : b) {
            buf.append('\t');
            buf.append(o);
        }
        print(buf.toString());
    }

    private static int indent;
    private static PrintWriter out;
}

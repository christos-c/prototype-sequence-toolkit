package fig.basic;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListUtils {

    public static <T> ArrayList<T> newList(T... list) {
        return new ArrayList<T>(Arrays.asList(list));
    }

    public static <T> T get(List<T> l, int i, T defValue) {
        if (i < 0) i += l.size();
        if (i < 0 || i >= l.size()) return defValue;
        return l.get(i);
    }

    public static <T> T getLast(T[] l) {
        return get(l, -1);
    }

    public static <T> T get(T[] l, int i) {
        return get(l, i, null);
    }

    public static <T> T get(T[] l, int i, T defValue) {
        if (i < 0) i += l.length;
        if (i < 0 || i >= l.length) return defValue;
        return l[i];
    }

    public static double sum(double[] list) {
        double sum = 0;
        for (double x : list) sum += x;
        return sum;
    }

    public static Integer[] toObjArray(int[] v) {
        Integer[] newv = new Integer[v.length];
        for (int i = 0; i < v.length; i++) newv[i] = v[i];
        return newv;
    }

    public static Double[] toObjArray(double[] v) {
        Double[] newv = new Double[v.length];
        for (int i = 0; i < v.length; i++) newv[i] = v[i];
        return newv;
    }

    public interface Generator<T> {
        T generate(int i);
    }

    public static <T> T[] newArray(int n, Class c, Generator<T> gen) {
        T[] a = (T[]) Array.newInstance(c, n);
        for (int i = 0; i < n; i++) a[i] = gen.generate(i);
        return a;
    }

    public static <T> T[] newArray(int n, T x) {
        T[] a = (T[]) Array.newInstance(x.getClass(), n);
        for (int i = 0; i < n; i++) a[i] = x;
        return a;
    }

    public static void multMut(double[] vec, double f) {
        for (int i = 0; i < vec.length; i++)
            vec[i] *= f;
    }

    public static double[] subArray(double[] v, int start, int end) {
        double[] subv = new double[end - start];
        System.arraycopy(v, start, subv, 0, end - start);
        return subv;
    }

}

package fig.basic;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IOUtils {

    public static List<File> getFilesUnder(String path, String prefix, String extension, final boolean recursive) {
        final File root = new File(path);
        final Pattern p = Pattern.compile(String.format("^%s.*%s$", prefix, extension));
        FileFilter filter = new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return recursive || root.equals(pathname);
                }
                return p.matcher(pathname.getName()).matches();
            }
        };
        List<File> files = new ArrayList<File>();
        addFilesUnder(root, files, filter);
        return files;
    }


    public static void addFilesUnder(File root, List<File> files, FileFilter fileFilter) {
        if (!fileFilter.accept(root)) return;
        if (root.isFile()) {
            files.add(root);
            return;
        }
        if (root.isDirectory()) {
            File[] children = root.listFiles();
            assert children != null;
            for (File child : children) {
                addFilesUnder(child, files, fileFilter);
            }
        }
    }

    public static boolean createNewDirIfNotExistsEasy(String path) {
        return path != null && (new File(path).isDirectory() || new File(path).mkdir());
    }

    public static BufferedReader openIn(String path) throws IOException {
        return openIn(new File(path));
    }

    public static BufferedReader openIn(File path) throws IOException {
        InputStream is = new FileInputStream(path);
        if (path.getName().endsWith(".gz")) is = new GZIPInputStream(is);
        return new BufferedReader(CharEncUtils.getReader(is));
    }

    public static BufferedReader openInHard(String path) {
        return openInHard(new File(path));
    }

    public static BufferedReader openInHard(File path) {
        try { return openIn(path); } catch (Exception e) { throw new RuntimeException(e); }
    }

    // openOut
    public static PrintWriter openOut(String path) throws IOException {
        return openOut(new File(path));
    }

    public static PrintWriter openOut(File path) throws IOException {
        OutputStream os = new FileOutputStream(path);
        if (path.getName().endsWith(".gz")) os = new GZIPOutputStream(os);
        return new PrintWriter(CharEncUtils.getWriter(os));
    }

    public static PrintWriter openOutEasy(String path) {
        if (StrUtils.isEmpty(path)) return null;
        return openOutEasy(new File(path));
    }

    public static PrintWriter openOutEasy(File path) {
        if (path == null) return null;
        try { return openOut(path); } catch (Exception e) { return null; }
    }

    public static PrintWriter openOutHard(String path) {
        return openOutHard(new File(path));
    }

    public static PrintWriter openOutHard(File path) {
        try { return openOut(path); } catch (Exception e) { throw new RuntimeException(e); }
    }
    // }

    public static ObjectInputStream openObjIn(File path) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        return path.getName().endsWith(".gz") ? new ObjectInputStream(new GZIPInputStream(fis)) : new ObjectInputStream(
                fis);
    }

    public static ObjectOutputStream openObjOut(File path) throws IOException {
        FileOutputStream fos = new FileOutputStream(path);
        return path.getName().endsWith(".gz") ? new ObjectOutputStream(
                new GZIPOutputStream(fos)) : new ObjectOutputStream(fos);
    }

    // readObjFile
    public static Object readObjFile(String path) throws IOException, ClassNotFoundException {
        return readObjFile(new File(path));
    }

    public static Object readObjFile(File path) throws IOException, ClassNotFoundException {
        ObjectInputStream in = openObjIn(path);
        Object obj = in.readObject();
        in.close();
        return obj;
    }

    public static Object readObjFileHard(String path) {
        return readObjFileHard(new File(path));
    }

    public static Object readObjFileHard(File path) {
        try { return readObjFile(path); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static void writeObjFile(File path, Object obj) throws IOException {
        ObjectOutputStream out = openObjOut(path);
        out.writeObject(obj);
        out.close();
    }

    public static void writeObjFileHard(String path, Object obj) {
        writeObjFileHard(new File(path), obj);
    }

    public static void writeObjFileHard(File path, Object obj) {
        try { writeObjFile(path, obj); } catch (Exception e) { throw new RuntimeException(e); }
    }
    // }

    // Copying files {
    // Return number of bytes copied
    public static int copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[16384];
        int total = 0, n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            out.write(buf, 0, n);
        }
        out.flush();
        return total;
    }

    // Return number of characters copied
    public static int copy(Reader in, Writer out) throws IOException {
        char[] buf = new char[16384];
        int total = 0, n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            out.write(buf, 0, n);
        }
        out.flush();
        return total;
    }

    // Ordinary read lines function
    public static List<String> readLines(String path) throws IOException {

        BufferedReader in = IOUtils.openIn(path);
        List<String> list = readLines(in);
        in.close();
        return list;
    }

    public static void writeLinesHard(String path, List<String> lines) {
        try {
            PrintWriter out = IOUtils.openOut(path);
            for (String line : lines) {
                out.println(line);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Iterator<String> lineIterator(String path) throws IOException {
        final BufferedReader reader = IOUtils.openIn(path);
        return new Iterator<String>() {

            private String line;

            public boolean hasNext() {
                try {
                    return nextLine();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            private boolean nextLine() throws IOException {
                if (line != null) { return true; }
                line = reader.readLine();
                return line != null;
            }

            public String next() {
                try {
                    nextLine();
                    String retLine = line;
                    line = null;
                    return retLine;
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            }

            public void remove() {
                throw new RuntimeException("remove() not supported");
            }
        };
    }

    public static List<String> readLines(BufferedReader in) throws IOException {
        String line;
        List<String> lines = new ArrayList<String>();
        while ((line = in.readLine()) != null) lines.add(line);
        return lines;
    }

    public static List<String> readLinesHard(String path) {
        try { return readLines(path); } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void printLines(String path, List lines) throws IOException {
        PrintWriter out = IOUtils.openOut(path);
        printLines(out, lines);
        out.close();
    }

    public static void printLinesHard(String path, List lines) {
        try { printLines(path, lines); } catch (IOException e) { throw new RuntimeException(e); }
    }

    public static void printLines(PrintWriter out, List lines) {
        for (Object line : lines)
            out.println(StrUtils.toString(line));
    }

}

package fig.exec;

import fig.basic.*;
import fig.record.Record;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static fig.basic.LogInfo.*;

/**
 * Represents all the settings and output of an execution of a program. An execution is defined by all the options
 * registered with OptionsParser. Creates a directory for the execution in the execution pool dir.
 */
public class Execution {
    @Option(gloss = "Whether to create a directory for this run; if not, don't generate output files")
    public static boolean create = false;
    @Option(gloss = "Whether to create a thread to monitor the status.")
    public static boolean monitor = false;

    // How to create the execution directory
    @Option(gloss = "Directory to put all output files; if blank, use execPoolDir.")
    public static String execDir;
    @Option(gloss = "Directory which contains all the executions (or symlinks).")
    public static String execPoolDir;
    @Option(gloss = "Directory which actually holds the executions.")
    public static String actualExecPoolDir;
    @Option(gloss = "Overwrite the contents of the execDir if it doesn't exist (e.g., when running a thunk).")
    public static boolean overwriteExecDir;
    @Option(gloss = "Assume in the run directory, automatically set execPoolDir and actualExecPoolDir")
    public static boolean useStandardExecPoolDirStrategy = false;

    @Option(gloss = "Simply print options and exit.")
    public static boolean printOptionsAndExit = false;
    @Option(gloss = "Miscellaneous options (written to options.map and output.map, displayed in servlet); example: a=3 b=4")
    public static ArrayList<String> miscOptions = new ArrayList<String>();

    @Option(gloss = "Name of the view to add this execution to in the servlet")
    public static ArrayList<String> addToView = new ArrayList<String>();
    @Option(gloss = "Record file to write to")
    public static String recordPath;

    @Option(gloss = "Character encoding")
    public static String charEncoding;

    // Thunk
    @Option(gloss = "Make a thunk (a delayed computation).")
    public static boolean makeThunk;

    //Exception handling
    @Option(gloss = "Whether to catch exceptions (ignored when making a thunk)")
    public static boolean dontCatchExceptions;

    // Whether to print out start a main() track (LogInfo)
    public static boolean startMainTrack = true;

    // Execution directory that we write to (execDir is just a suggestion)
    // Could be a symlink to a directory in actualExecPoolDir
    private static String virtualExecDir;

    // Passed to the options parser
    public static boolean ignoreUnknownOpts = false;

    static OrderedStringMap inputMap = new OrderedStringMap(); // Accessed by monitor thread
    private static OrderedStringMap outputMap = new OrderedStringMap();
    private static OptionsParser parser;
    private static MonitorThread monitorThread; // Thread for monitoring
    static int exitCode = 0;

    static boolean shouldBail = false; // Set by monitor thread

    private static void mkdirHard(File f) {
        if (!f.mkdir()) {
            stderr.println("Cannot create directory: " + f);
            System.exit(1);
        }
    }

    public static String getVirtualExecDir() {
        return virtualExecDir;
    }

    /**
     * Return an unused directory in the execution pool directory. Set virtualExecDir
     */
    public static String createVirtualExecDir() {
        if (useStandardExecPoolDirStrategy) {
            // Assume we are in the run directory, so set the standard paths
            execPoolDir = new File(SysInfoUtils.getcwd(), "state/execs").toString();
            actualExecPoolDir = new File(SysInfoUtils.getcwd(), "state/hosts/" + SysInfoUtils.getShortHostName())
                    .toString();
            if (!new File(actualExecPoolDir).isDirectory()) actualExecPoolDir = null;
        }
        if (!StrUtils.isEmpty(execPoolDir) && !new File(execPoolDir).isDirectory())
            throw Exceptions.bad("Execution pool directory '" + execPoolDir + "' doesn't exist");
        if (!StrUtils.isEmpty(actualExecPoolDir) && !new File(actualExecPoolDir).isDirectory())
            throw Exceptions.bad("Actual execution pool directory '" + actualExecPoolDir + "' doesn't exist");

        if (!StrUtils.isEmpty(execDir)) { // Use specified execDir
            boolean exists = new File(execDir).isDirectory();
            if (exists && !overwriteExecDir)
                throw Exceptions.bad("Directory already exists and overwrite flag is false");
            if (!exists) mkdirHard(new File(execDir));
            else {
                // This part looks at actualExecPoolDir
                // This case is overwriting an existing execution directory, which
                // happens when we are executing a thunk.  We have to be careful here
                // because the actual symlinked directory that was created when thunking
                // might be using a different actualPoolDir.  If this happens, we need
                // to move the actual thunked symlinked directory into the actual
                // execution pool directory requested.  In fact, we always do this for simplicity.
                String oldActualExecDir = Utils.systemGetStringOutputEasy("readlink " + execDir);
                if (oldActualExecDir == null) { // Not symlink
                    if (!StrUtils.isEmpty(actualExecPoolDir)) throw Exceptions
                            .bad("The old execution directory was not created with actualExecPoolDir but now we want an actualExecPoolDir");
                    // Do nothing, just use the directory as is
                } else { // Symlink
                    oldActualExecDir = oldActualExecDir.trim();
                    if (StrUtils.isEmpty(actualExecPoolDir)) throw Exceptions
                            .bad("The old execution directory was created with actualExecPoolDir but now we don't want an actualExecPoolDir");
                    // Note that now the execution numbers might not correspond between the
                    // actual and virtual execution pool directories.
                    File newActualExecDir;
                    for (int i = 0; ; i++) {
                        newActualExecDir = new File(actualExecPoolDir, i + "a.exec");
                        if (!newActualExecDir.exists()) break;
                    }
                    // Move the old directory to the new directory
                    Utils.systemHard(String.format("mv %s %s", oldActualExecDir, newActualExecDir));
                    // Update the symlink (execDir -> newActualExecDir)
                    Utils.systemHard(String.format("ln -sf %s %s", newActualExecDir.getAbsolutePath(), execDir));
                }
            }
            return virtualExecDir = execDir;
        }

        // execDir hasn't been specified, so we need to pick one from a pool directory
        // execPoolDir must exist; actualExecPoolDir is optional

        // Get a list of files that already exists
        Set<String> files = new HashSet<String>();
        Collections.addAll(files, new File(execPoolDir).list());

        // Go through and pick out a file that doesn't exist
        int numFailures = 0;
        for (int i = 0; numFailures < 3; i++) {
            // Either the virtual file (a link) or the actual file
            File f = new File(execPoolDir, i + ".exec");
            // Actual file
            File g = StrUtils.isEmpty(actualExecPoolDir) ? null : new File(actualExecPoolDir, i + ".exec");

            if (!files.contains(i + ".exec") && (g == null || !g.exists())) {
                if (g == null || g.equals(f)) {
                    mkdirHard(f);
                    return virtualExecDir = f.toString();
                }
                // Create symlink before mkdir to try to reserve the name and avoid race conditions
                if (Utils.createSymLink(g.getAbsolutePath(), f.getAbsolutePath())) {
                    mkdirHard(g);
                    return virtualExecDir = f.toString();
                }

                // Probably because someone else already linked to it
                // in the race condition: so try again
                stderr.println("Cannot create symlink from " + f + " to " + g);
                numFailures++;
            }
        }
        throw Exceptions.bad("Failed many times to create execution directory");
    }

    // Get the path of the file (in the execution directory)
    public static String getFile(String file) {
        if (StrUtils.isEmpty(virtualExecDir)) return null;
        if (StrUtils.isEmpty(file)) return null;
        return new File(virtualExecDir, file).toString();
    }

    public synchronized static void putOutput(String s, Object t) {
        outputMap.put(s, StrUtils.toString(t));
    }

    public synchronized static void printOutputMapToStderr() {
        outputMap.print(stderr);
    }

    public synchronized static void printOutputMap(String path) {
        if (StrUtils.isEmpty(path)) return;
        // First write to a temporary directory and then rename the file
        String tmpPath = path + ".tmp";
        if (outputMap.printEasy(tmpPath)) new File(tmpPath).renameTo(new File(path));
    }

    public static void setExecStatus(String newStatus, boolean override) {
        String oldStatus = outputMap.get("exec.status");
        if (oldStatus == null || oldStatus.equals("running")) override = true;
        if (override) putOutput("exec.status", newStatus);
    }

    static OrderedStringMap getInfo() {
        OrderedStringMap map = new OrderedStringMap();
        map.put("Date", SysInfoUtils.getCurrentDateStr());
        map.put("Host", SysInfoUtils.getHostName());
        map.put("CPU speed", SysInfoUtils.getCPUSpeedStr());
        map.put("Max memory", SysInfoUtils.getMaxMemoryStr());
        map.put("Num CPUs", SysInfoUtils.getNumCPUs());
        return map;
    }

    public static void init(String[] args, Object... objects) {
        // Parse options
        parser = new OptionsParser();
        parser.doRegister("log", LogInfo.class);
        parser.doRegister("exec", Execution.class);
        parser.doRegisterAll(objects);
        // These options are specific to the execution, so we don't want to overwrite them
        // with a previous execution's.
        parser.setDefaultDirFileName("options.map");
        parser.setIgnoreOptsFromFileName("options.map", ListUtils
                .newList("log.file", "exec.execDir", "exec.execPoolDir", "exec.actualPoolDir", "exec.makeThunk"));
        if (ignoreUnknownOpts) parser.ignoreUnknownOpts();
        if (!parser.doParse(args)) System.exit(1);

        // Set character encoding
        if (charEncoding != null) CharEncUtils.setCharEncoding(charEncoding);

        if (printOptionsAndExit) { // Just print options and exit
            parser.doGetOptionPairs().print(stdout);
            System.exit(0);
        }

        // Create a new directory
        if (create) {
            createVirtualExecDir();
            //stderr.println(virtualExecDir);
            if (!makeThunk) LogInfo.file = getFile("log");
        } else {
            LogInfo.file = "";
        }

        // Handle miscOptions
        for (String opt : miscOptions) {
            String[] tokens = opt.split("=");
            if (tokens.length == 2) putOutput(tokens[0], tokens[1]);
        }

        if (!makeThunk) {
            LogInfo.init();
            if (startMainTrack) track("main()", true);
        }

        // Output options
        if (!makeThunk && virtualExecDir != null) logs("Execution directory: " + virtualExecDir);
        if (!makeThunk) getInfo().printEasy(getFile("info.map"));
        printOptions();
        if (create && addToView.size() > 0) IOUtils.printLinesHard(Execution.getFile("addToView"), addToView);

        // Start monitoring
        if (!makeThunk && monitor) {
            monitorThread = new MonitorThread();
            monitorThread.start();
        }

        if (!makeThunk) Record.init(!StrUtils.isEmpty(recordPath) ? recordPath : Execution.getFile("record"));
    }

    // Might want to call this again after some command-line options were changed.
    public static void printOptions() {
        boolean saveMakeThunk = makeThunk;
        makeThunk = false;
        parser.doGetOptionPairs().printEasy(getFile("options.map"));
        parser.doGetOptionStrings().printEasy(getFile("options.help"));
        makeThunk = saveMakeThunk;
    }

    public static void raiseException(Throwable t) {
        error(t + ":\n" + StrUtils.join(t.getStackTrace(), "\n"));
        t = t.getCause();
        if (t != null) error("Caused by " + t + ":\n" + StrUtils.join(t.getStackTrace(), "\n"));
        putOutput("exec.status", "exception");
        exitCode = 1;
    }

    public static void finish() {
        if (!makeThunk) {
            Record.finish();

            if (monitor) monitorThread.finish();
            setExecStatus(shouldBail ? "bailed" : "done", false);
            outputMap.printEasy(getFile("output.map"));
            StopWatchSet.getStats().printEasy(getFile("time.map"));
            if (!makeThunk && virtualExecDir != null) logs("Execution directory: " + virtualExecDir);
            if (makeThunk && virtualExecDir != null) stderr.println(virtualExecDir);
            if (LogInfo.getNumErrors() > 0 || LogInfo.getNumWarnings() > 0)
                stderr.printf("%d errors, %d warnings\n", LogInfo.getNumErrors(), LogInfo.getNumWarnings());
            if (startMainTrack) end_track();
        }

        System.exit(exitCode);
    }

    // This should be all we need to put in a main function.
    // args are the commandline arguments
    // First object is the Runnable object to call run on.
    // All of them are objects whose options args is to supposed to populate.
    public static void run(String[] args, Object... objects) {
        init(args, objects);

        Object mainObj;
        if (objects[0] instanceof String) mainObj = objects[1];
        else mainObj = objects[0];
        if (dontCatchExceptions) {
            ((Runnable) mainObj).run();
        } else {
            try {
                ((Runnable) mainObj).run();
            } catch (Throwable t) {
                raiseException(t);
            }
        }
        finish();
    }
}

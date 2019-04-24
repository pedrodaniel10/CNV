import BIT.highBIT.*;
import java.io.*;
import java.util.*;


public class ICountParallel {
    private static PrintStream out = null;
    private static Map<Long, Long> countersThreads = new HashMap<>();
    private static final String INFO_STRING = "[Info] ";
    private static final String FILE_PATH = "instrumentation.txt";
    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();
        
        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.equals("Solver.class") || infilename.equals("SolverArgumentParser$SolverParameters.class") || infilename.equals("SolverArgumentParser.class")) {
                // create class info object
				ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);
				
                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    
                    routine.addBefore("ICountParallel", "count", new Integer(routine.getInstructionCount()));
                    
                    if (routine.getMethodName().equals("solveImage")) { 
                        routine.addAfter("ICountParallel", "printICount", ci.getClassName());
                    }
                }
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);
            }
        }
    }
    
    public static synchronized void printICount(String foo) {
        Long threadId = Thread.currentThread().getId();
        String countString = "Thread " + threadId + ": " + countersThreads.get(threadId) + " instructions";
        System.out.print(INFO_STRING);
        System.out.println(countString);

        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(FILE_PATH, true)))) {
            out.println(countString);
        } catch (IOException e) {
            System.err.println(e);
        }

        //threads are reused
        countersThreads.put(threadId, new Long(0));
    }

    public static synchronized void count(int incr) {
        Long threadId = Thread.currentThread().getId();
        if (!countersThreads.containsKey(threadId)) {
            countersThreads.put(threadId, new Long(incr));
        } else {
            countersThreads.put(threadId, countersThreads.get(threadId) + incr);
        }
    }

}


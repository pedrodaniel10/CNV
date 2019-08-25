import BIT.highBIT.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.cnv.databaselib.HcRequest;
import pt.ulisboa.tecnico.cnv.databaselib.DatabaseUtils;

public class ICountParallel {
    private static PrintStream out = null;
    private static Map<Long, HcRequest> threadsRequests = new ConcurrentHashMap<>();
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
    
    public static void printICount(String foo) {
        Long threadId = Thread.currentThread().getId();
        String countString = "Thread " + threadId + ": " + threadsRequests.get(threadId).getMetrics() + " instructions";
        System.out.print(INFO_STRING);
        System.out.println(countString);

        HcRequest hcRequest = threadsRequests.get(threadId);

        if (!hcRequest.isCompleted()) {
            hcRequest.setCompleted(true);
            DatabaseUtils.save(hcRequest);
        }
    }

    public static void count(int incr) {
        Long threadId = Thread.currentThread().getId();

        HcRequest hcRequest = threadsRequests.get(threadId);

        if (!hcRequest.isCompleted()) {
            hcRequest.setMetrics(hcRequest.getMetrics() + incr);
        }
    }

    public static void initCounter(HcRequest hcRequest) {
        Long threadId = Thread.currentThread().getId();
        threadsRequests.put(threadId, hcRequest);
    }

}


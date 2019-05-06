package pt.ulisboa.tecnico.cnv.loadbalancer;

import java.util.Map;

public class AutoScaler {

    private static final long SCALE_INTERVAL = 60000;
    private static final double CPU_UTILIZATION_MAX = 80;
    private static final double CPU_UTILIZATION_MIN = 40;

    public static void execute() throws InterruptedException {
        while (true) {
            Thread.sleep(SCALE_INTERVAL);
            scale();
        }
    }

    private static void scale() {
        System.out.println("--------------------------------------");
        System.out.println("Auto-Scaling...");

        AwsUtils.updateCpuMetrics();
        checkMarkedInstances();

        double average = 0;
        int size = 0;
        boolean freshLaunch = false;
        for (Map.Entry<String, InstanceInfo> entry : AwsUtils.runningInstanceInfos.entrySet()) {
            if (!entry.getValue().willTerminate()) {
                average += entry.getValue().getLastCpuMeasured();
                size++;
                if (entry.getValue().isFresh()) {
                    freshLaunch = true;
                }
            }
        }

        if (size > 0) {
            average /= size;

            System.out.println("Total average CPU utilization (" + size + " instances): " + average);

            if (average > CPU_UTILIZATION_MAX) {
                AwsUtils.launchInstance();

            } else if (average < CPU_UTILIZATION_MIN && !freshLaunch) {
                markLeastUsedInstance();
            }
        }
        System.out.println("--------------------------------------");
    }

    private static void markLeastUsedInstance() {
        String instanceId = AwsUtils.getLeastUsedValidInstanceInfo().getInstance().getInstanceId();
        AwsUtils.markInstance(instanceId);
        System.out.println("Instance " + instanceId + " marked to terminate");
    }

    private static void checkMarkedInstances() {
        for (final Map.Entry<String, InstanceInfo> entry : AwsUtils.runningInstanceInfos.entrySet()) {
            if (entry.getValue().willTerminate() && entry.getValue().getNumCurrentRequests() == 0) {
                new Thread(new Runnable() {
                    public void run() {
                        AwsUtils.terminateInstance(entry.getKey());
                    }
                }).start();
            }
        }
    }

}

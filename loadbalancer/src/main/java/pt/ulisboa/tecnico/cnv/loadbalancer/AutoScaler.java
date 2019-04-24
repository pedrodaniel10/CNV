package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.services.ec2.model.Instance;
import java.util.Map;

public class AutoScaler {

    private static final long SCALE_INTERVAL = 60000;
    private static final double CPU_UTILIZATION_MAX = 60;
    private static final double CPU_UTILIZATION_MIN = 40;


    public static void execute() throws InterruptedException {
        while (true) {
            Thread.sleep(SCALE_INTERVAL);
            scale();
        }
    }

    private static void scale() {
        System.out.println("Scaling...");
        Map<Instance, Double> cpuMetrics = AwsUtils.getCpuMetricsPerInstance();

        if (cpuMetrics.isEmpty()) {
            AwsUtils.launchInstance();
            return;
        }

        double average = 0;
        for (Double value : cpuMetrics.values()) {
            average += value;
        }

        average /= cpuMetrics.size();

        System.out.println("Total average CPU utilization (" + cpuMetrics.size() + " instances): " + average);

        if (average > CPU_UTILIZATION_MAX) {
            AwsUtils.launchInstance();
        } else if (average < CPU_UTILIZATION_MIN && cpuMetrics.size() > 1) {
            terminateLeastUsedInstance();
            //TODO Safe terminate
        }
    }

    private static void terminateLeastUsedInstance() {
        AwsUtils.terminateInstance(AwsUtils.getLeastUsedInstance().getInstanceId());
    }

}

package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class AwsUtils {

    public static final int RUNNING_CODE = 16;
    public static final int PENDING_CODE = 0;
    private static String awsImageId;
    private static String awsKeyName;
    private static String awsSecGroup;
    private static String awsInstanceType;

    private static AmazonEC2 ec2;
    private static AmazonCloudWatch cloudWatch;

    private static final String PROPERTIES_PATH = "/home/ec2-user/.aws/webserver.properties";

    // Initialize variables
    static {
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            System.err.println("Cannot load the credentials from the credential profiles file. " +
                "Please make sure that your credentials file is at the correct " +
                "location (~/.aws/credentials), and is in valid format.");
            e.printStackTrace();
            System.exit(1);
        }
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(PROPERTIES_PATH));
        } catch (IOException e) {
            System.err.println("Cannot load the properties from the file. " +
                "Please make sure that your properties file is at the correct " +
                "location (~/.aws/webserver.properties), and is in valid format.");
            e.printStackTrace();
            System.exit(1);
        }

        String awsServerRegion = properties.getProperty("server-region");
        awsImageId = properties.getProperty("image-id");
        awsKeyName = properties.getProperty("key-name");
        awsSecGroup = properties.getProperty("sec-group");
        awsInstanceType = properties.getProperty("instace-type");

        ec2 = AmazonEC2ClientBuilder.standard().withRegion(awsServerRegion).withCredentials(
            new AWSStaticCredentialsProvider(credentials)).build();
        cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(awsServerRegion).withCredentials(
            new AWSStaticCredentialsProvider(credentials)).build();
    }

    private AwsUtils() {
    }

    public static Set<Instance> getPendingInstances() {
        return getInstances(PENDING_CODE);
    }

    public static Set<Instance> getRunningInstances() {
        return getInstances(RUNNING_CODE);
    }

    private static Set<Instance> getInstances(int code) {
        Set<Instance> instances = new HashSet<>();
        String loadBalancerId = EC2MetadataUtils.getInstanceId();
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances();
        List<Reservation> reservations = describeInstancesResult.getReservations();

        for (Reservation reservation : reservations) {
            List<Instance> foundInstances = reservation.getInstances();
            for (Instance inst : foundInstances) {
                if (!inst.getInstanceId().equals(loadBalancerId) && inst.getState().getCode() == code) {
                    instances.add(inst);
                }
            }
        }
        return instances;

    }

    public static Instance launchInstance() {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(awsImageId)
            .withInstanceType(awsInstanceType)
            .withMinCount(1)
            .withMaxCount(1)
            .withKeyName(awsKeyName)
            .withSecurityGroups(awsSecGroup)
            .withMonitoring(true);

        RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
        System.out.println("Launching instance...");

        return runInstancesResult.getReservation().getInstances().get(0);
    }

    public static void terminateInstance(String instanceId) {
        TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
        terminateInstancesRequest.withInstanceIds(instanceId);
        ec2.terminateInstances(terminateInstancesRequest);
        System.out.println("Terminating instance " + instanceId + "...");
    }

    public static int getInstanceStatus(String instanceId) {
        DescribeInstancesRequest describeInstanceRequest = new DescribeInstancesRequest().withInstanceIds(instanceId);
        DescribeInstancesResult describeInstanceResult = ec2.describeInstances(describeInstanceRequest);
        InstanceState state = describeInstanceResult.getReservations().get(0).getInstances().get(0).getState();
        return state.getCode();
    }

    public static Map<Instance, Double> getCpuMetricsPerInstance() {
        Map<Instance, Double> cpuMetricsPerInstance = new HashMap<>();

        long offsetInMilliseconds = 1000 * 60 * 10;
        Dimension instanceDimension = new Dimension();
        instanceDimension.setName("InstanceId");

        for (Instance instance : getRunningInstances()) {
            String name = instance.getInstanceId();
            instanceDimension.setValue(name);

            GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - offsetInMilliseconds))
                .withNamespace("AWS/EC2")
                .withPeriod(60)
                .withMetricName("CPUUtilization")
                .withStatistics("Average")
                .withDimensions(instanceDimension)
                .withEndTime(new Date());

            GetMetricStatisticsResult getMetricStatisticsResult =
                cloudWatch.getMetricStatistics(request);
            List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();

            Double average;

            if (!datapoints.isEmpty()) {
                average = datapoints.get(datapoints.size() - 1).getAverage();
            } else {
                average = 0.0;
            }

            cpuMetricsPerInstance.put(instance, average);
        }
        return cpuMetricsPerInstance;
    }

    public static Instance getLeastUsedInstance() {
        Instance instance = null;
        Map<Instance, Double> cpuMetrics = AwsUtils.getCpuMetricsPerInstance();
        Double min = 100.0;
        for (Map.Entry<Instance, Double> entry : cpuMetrics.entrySet()) {
            if (entry.getValue() <= min) {
                min = entry.getValue();
                instance = entry.getKey();
            }
        }
        return instance;
    }

}
package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import pt.ulisboa.tecnico.cnv.databaselib.DatabaseUtils;
import pt.ulisboa.tecnico.cnv.databaselib.HcRequest;

public class LoadBalancerHandler implements HttpHandler {

    private static final int WS_PORT = 8000;
    private static final long WS_WAIT_TIME = 2000;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        final String query = httpExchange.getRequestURI().getQuery();
        System.out.println("Received request: " + query);

        final String[] params = query.split("&");

        double requestWork;

        HcRequest hcRequest = new HcRequest();
        hcRequest.buildRequestId(params);
        List<HcRequest> queryResult = DatabaseUtils.getRequestById(hcRequest);

        if (!queryResult.isEmpty() && queryResult.get(0).isCompleted()) {
            System.out.println("Request found in DB!");
            hcRequest = queryResult.get(0);
            requestWork = hcRequest.getMetrics();
        } else {
            System.out.println("Request not found in DB, estimating work");
            hcRequest = new HcRequest(params);
            requestWork = calculateEstimatedWork(hcRequest);
        }

        InstanceInfo instanceInfo = reserveLeastUsedInstance(requestWork);
        String instanceIpAddr = instanceInfo.getInstance().getPublicIpAddress();
        String instanceId = instanceInfo.getInstance().getInstanceId();

        String serverUrl = "http://" + instanceIpAddr + ":" + WS_PORT + httpExchange.getRequestURI();
        System.out.println("Sending request to " + instanceId);

        byte[] response = tryToGetResponse(serverUrl, instanceInfo, requestWork);
        instanceInfo.decrementNumCurrentRequests();
        instanceInfo.decrementWork(requestWork);
        System.out.println("Received response from " + instanceId);

        httpExchange.sendResponseHeaders(200, 0);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response);
        outputStream.close();
    }

    private byte[] tryToGetResponse(String serverUrl, InstanceInfo instanceInfo, double requestWork)
        throws IOException {
        InputStream is;
        while (true) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
                is = connection.getInputStream();
                byte[] response = toByteArray(is);
                connection.disconnect();
                return response;
            } catch (IOException e) {
                if (AwsUtils.getInstanceStatus(instanceInfo.getInstance().getInstanceId()) == 16) { //running
                    try {
                        Thread.sleep(WS_WAIT_TIME);
                    } catch (InterruptedException e1) {
                        instanceInfo.decrementNumCurrentRequests();
                        instanceInfo.decrementWork(requestWork);
                        Thread.currentThread().interrupt();
                    }
                } else {
                    instanceInfo.decrementNumCurrentRequests();
                    instanceInfo.decrementWork(requestWork);
                    throw e;
                }
            }
        }
    }

    private byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

    private double calculateEstimatedWork(HcRequest hcRequest) {
        double estimatedWork = 0;

        List<HcRequest> queryResult = DatabaseUtils.getCompletedRequestsWithSameMap(hcRequest);
        if (!queryResult.isEmpty()) {
            System.out.println(hcRequest.getRequestId() + ": Found requests with same map!");
            estimatedWork = estimateFromSameMapRequests(hcRequest, queryResult);
        }

        if (estimatedWork == 0) {
            queryResult = getCompletedRequestsWithSimilarSize(hcRequest, DatabaseUtils.getCompletedRequests());
            if (!queryResult.isEmpty()) {
                System.out.println(hcRequest.getRequestId() + ": Found requests with similar size!");
                estimatedWork = estimateFromSimilarSizeRequests(hcRequest, queryResult);
            }
        }

        if (estimatedWork == 0) {
            queryResult = DatabaseUtils.getCompletedRequestsWithSameStrategy(hcRequest);
            if (!queryResult.isEmpty()) {
                System.out.println(hcRequest.getRequestId() + ": Found requests with same strategy!");
                estimatedWork = calculateAverageFromRequests(hcRequest, queryResult);
            }
        }

        if (estimatedWork == 0) {
            queryResult = DatabaseUtils.getCompletedRequests();
            if (!queryResult.isEmpty()) {
                System.out.println(hcRequest.getRequestId() + ": Estimating from average of all requests");
                estimatedWork = calculateAverageFromRequests(hcRequest, queryResult);
            }
        }

        if (estimatedWork == 0) {
            estimatedWork = 200000000; //estimated average
        }

        System.out.println("Estimated work for request(" + hcRequest.getRequestId() + "): " + estimatedWork);
        return estimatedWork;
    }

    private List<HcRequest> getCompletedRequestsWithSimilarSize(HcRequest hcRequest,
        List<HcRequest> completedRequests) {
        List<HcRequest> queryResult = new ArrayList<>();

        int requestXSize = hcRequest.getX1() - hcRequest.getX0();
        int requestYSize = hcRequest.getX1() - hcRequest.getX0();
        int maxXDiff = requestXSize / 2;
        int maxYDiff = requestYSize / 2;

        for (HcRequest resultRequest : completedRequests) {
            int currentXDiff = Math.abs((resultRequest.getX1() - resultRequest.getX0()) - requestXSize);
            int currentYDiff = Math.abs((resultRequest.getY1() - resultRequest.getY0()) - requestYSize);

            if (currentXDiff < maxXDiff && currentYDiff < maxYDiff) {
                queryResult.add(resultRequest);
            }
        }
        return queryResult;
    }

    private double estimateFromSameMapRequests(HcRequest hcRequest, List<HcRequest> queryResult) {
        int minXDiff = (hcRequest.getX1() - hcRequest.getX0()) / 2;
        int minYDiff = (hcRequest.getY1() - hcRequest.getY0()) / 2;
        List<HcRequest> similarRequests = new ArrayList<>();

        for (HcRequest resultRequest : queryResult) {
            int currentX0Diff = Math.abs(hcRequest.getX0() - resultRequest.getX0());
            int currentX1Diff = Math.abs(hcRequest.getX1() - resultRequest.getX1());
            int currentY0Diff = Math.abs(hcRequest.getY0() - resultRequest.getY0());
            int currentY1Diff = Math.abs(hcRequest.getY1() - resultRequest.getY1());
            int currentStartingXDiff = Math.abs(hcRequest.getxS() - resultRequest.getxS());
            int currentStartingYDiff = Math.abs(hcRequest.getyS() - resultRequest.getyS());

            if (currentStartingXDiff < minXDiff && currentStartingYDiff < minYDiff
                && currentX0Diff < minXDiff && currentX1Diff < minXDiff && currentY0Diff < minYDiff
                && currentY1Diff < minYDiff) {

                similarRequests.add(resultRequest);
            }
        }

        if (!similarRequests.isEmpty()) {
            return estimateFromSimilarSizeRequests(hcRequest, similarRequests);
        } else {
            return 0;
        }
    }

    private double estimateFromSimilarSizeRequests(HcRequest hcRequest, List<HcRequest> queryResult) {
        List<HcRequest> sameStrategyRequests = new ArrayList<>();

        int requestSize = (hcRequest.getX1() - hcRequest.getX0()) * (hcRequest.getY1() - hcRequest.getY0());
        int minDiff = requestSize;

        for (HcRequest resultRequest : queryResult) {
            if (hcRequest.getStrategy().equals(resultRequest.getStrategy())) {
                sameStrategyRequests.add(resultRequest);
            }

            int resultSize =
                (resultRequest.getX1() - resultRequest.getX0()) * (resultRequest.getY1() - resultRequest.getY0());
            int diff = Math.abs(resultSize - requestSize);
            if (diff < minDiff) {
                minDiff = diff;
            }
        }

        if (!sameStrategyRequests.isEmpty()) {
            return calculateAverageFromRequests(hcRequest, sameStrategyRequests);
        } else {
            return calculateAverageFromRequests(hcRequest, queryResult);
        }
    }

    private double calculateAverageFromRequests(HcRequest hcRequest, List<HcRequest> queryResult) {
        double average = 0;
        int averageSize = 0;
        for (HcRequest resultRequest : queryResult) {
            average += resultRequest.getMetrics();
            averageSize +=
                ((resultRequest.getX1() - resultRequest.getX0()) + (resultRequest.getY1() - resultRequest.getY0()))/2;
        }
        average /= queryResult.size();
        averageSize /= queryResult.size();

        int hcRequestSize = ((hcRequest.getX1() - hcRequest.getX0()) + (hcRequest.getY1() - hcRequest.getY0()))/2;

        average = average*((hcRequestSize/averageSize)*(hcRequestSize/averageSize));

        return average;
    }

    private InstanceInfo reserveLeastUsedInstance(double requestWork) {
        InstanceInfo instanceInfo = AwsUtils.getLeastUsedValidInstanceInfo();

        if (instanceInfo == null) {
            synchronized (this) {
                if (AwsUtils.liveInstancesCounter.get() == 0) {
                    AwsUtils.launchInstance();
                    waitForInstance();
                }
            }
        }

        synchronized (this) {
            if (AwsUtils.getLeastUsedValidInstanceInfo().getNumCurrentRequests()
                >= 4) {
                AwsUtils.launchInstance();
                waitForInstance();
            }
            instanceInfo = AwsUtils.getLeastUsedValidInstanceInfo();
            instanceInfo.incrementNumCurrentRequests();
            instanceInfo.incrementWork(requestWork);
        }

        return instanceInfo;
    }

    private void waitForInstance() {
        InstanceInfo instanceInfo = null;
        while (instanceInfo == null || instanceInfo.getNumCurrentRequests() >= 4) {
            try {
                Thread.sleep(WS_WAIT_TIME);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            instanceInfo = AwsUtils.getLeastUsedValidInstanceInfo();
        }
    }

}

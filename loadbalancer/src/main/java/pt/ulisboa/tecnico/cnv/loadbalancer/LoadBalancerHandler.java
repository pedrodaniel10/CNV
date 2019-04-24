package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

public class LoadBalancerHandler implements HttpHandler {

    private static final int WS_PORT = 8000;
    private static final long WS_WAIT_TIME = 5000;
    private static boolean firstInstanceLaunched = false;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        System.out.println("Received request: " + httpExchange.getRequestURI().getQuery());

        Instance instance = null;
        try {
            instance = getInstanceFromAws();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String serverUrl = "http://" + instance.getPublicIpAddress() + ":" + WS_PORT + httpExchange.getRequestURI();
        System.out.println("Sending request to " + instance.getPublicIpAddress());

        byte[] response = tryGetResponse(serverUrl, instance);
        System.out.println("Received response from " + instance.getPublicIpAddress());

        httpExchange.sendResponseHeaders(200, 0);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response);
        outputStream.close();
    }

    private byte[] tryGetResponse(String serverUrl, Instance instance) throws IOException {
        InputStream is;
        while (true) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(serverUrl).openConnection();
                is = connection.getInputStream();
                byte[] response = toByteArray(is);
                connection.disconnect();
                return response;
            } catch (IOException e) {
                if (AwsUtils.getInstanceStatus(instance.getInstanceId()) == 16) { //running
                    try {
                        Thread.sleep(WS_WAIT_TIME);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                } else {
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

    private Instance getInstanceFromAws() throws InterruptedException {
        Instance instance = AwsUtils.getLeastUsedInstance();

        // No instances running, program is starting up / receiving first requests
        if (instance == null) {
            synchronized (this) {
                if (firstInstanceLaunched) {
                    Set<Instance> pendingInstances = AwsUtils.getPendingInstances();
                    if (!pendingInstances.isEmpty()) {
                        instance = pendingInstances.iterator().next();
                    } else {
                        instance = AwsUtils.getRunningInstances().iterator().next();
                    }
                } else {
                    instance = AwsUtils.launchInstance();
                    firstInstanceLaunched = true;
                }
            }

            int instanceState = AwsUtils.PENDING_CODE;
            while (instanceState != AwsUtils.RUNNING_CODE) {
                instanceState = AwsUtils.getInstanceStatus(instance.getInstanceId());
                Thread.sleep(WS_WAIT_TIME);
            }

            return AwsUtils.getRunningInstances().iterator().next();
        }

        return instance;
    }

}

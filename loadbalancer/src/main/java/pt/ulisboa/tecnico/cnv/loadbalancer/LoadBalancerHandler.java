package pt.ulisboa.tecnico.cnv.loadbalancer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoadBalancerHandler implements HttpHandler {

    private static final int WS_PORT = 8000;
    private static final long WS_WAIT_TIME = 2000;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        System.out.println("Received request: " + httpExchange.getRequestURI().getQuery());

        InstanceInfo instanceInfo = reserveLeastUsedInstance();
        String instanceIpAddr = instanceInfo.getInstance().getPublicIpAddress();
        String instanceId = instanceInfo.getInstance().getInstanceId();

        String serverUrl = "http://" + instanceIpAddr + ":" + WS_PORT + httpExchange.getRequestURI();
        System.out.println("Sending request to " + instanceId);

        byte[] response = tryToGetResponse(serverUrl, instanceInfo);
        instanceInfo.decrementNumCurrentRequests();
        System.out.println("Received response from " + instanceId);

        httpExchange.sendResponseHeaders(200, 0);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(response);
        outputStream.close();
    }

    private byte[] tryToGetResponse(String serverUrl, InstanceInfo instanceInfo) throws IOException {
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
                        Thread.currentThread().interrupt();
                    }
                } else {
                    instanceInfo.decrementNumCurrentRequests();
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

    private InstanceInfo reserveLeastUsedInstance() {
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

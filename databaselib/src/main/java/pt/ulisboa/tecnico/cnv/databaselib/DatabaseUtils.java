package pt.ulisboa.tecnico.cnv.databaselib;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.comprehendmedical.model.AttributeName;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class DatabaseUtils {

    private static final String PROPERTIES_PATH = "/home/ec2-user/.aws/webserver.properties";
    private static final String CREDENTIALS_PATH = "/home/ec2-user/.aws/credentials";
    private static AmazonDynamoDB dynamoDB;
    private static DynamoDBMapper dynamoDBMapper;
    public static final String tableName = "hc_requests";

    static {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider(CREDENTIALS_PATH, null);
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                e);
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

        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion(awsServerRegion)
            .build();
        dynamoDBMapper = new DynamoDBMapper(dynamoDB);

        createTable();
    }

    private DatabaseUtils() {
    }

    private static void createTable() {

        // Create a table with a primary hash key named 'name', which holds a string
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
            .withKeySchema(new KeySchemaElement().withAttributeName("request_id").withKeyType(KeyType.HASH))
            .withAttributeDefinitions(new AttributeDefinition().withAttributeName("request_id").withAttributeType(
                ScalarAttributeType.S))
            .withProvisionedThroughput(
                new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
        // wait for the table to move into ACTIVE state
        try {
            TableUtils.waitUntilActive(dynamoDB, tableName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void save(HcRequest hcRequest) {
        dynamoDBMapper.save(hcRequest);
    }

    public static List<HcRequest> getRequestById(HcRequest hcRequest) {
        DynamoDBQueryExpression<HcRequest> queryExpression = new DynamoDBQueryExpression<HcRequest>()
            .withHashKeyValues(hcRequest);
        return dynamoDBMapper.query(HcRequest.class, queryExpression);
    }

    public static List<HcRequest> getCompletedRequestsWithSameMap(HcRequest hcRequest) {
        List<HcRequest> queryResult = new ArrayList<>();

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withN(String.valueOf(1)));
        eav.put(":val2", new AttributeValue().withS(hcRequest.getMap()));

        Map<String, String> names = new HashMap<>();
        names.put("#map", "map");

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("completed = :val1 and #map = :val2").withExpressionAttributeNames(names)
            .withExpressionAttributeValues(eav);

        return dynamoDBMapper.scan(HcRequest.class, scanExpression);
    }

    public static List<HcRequest> getCompletedRequestsWithSameStrategy(HcRequest hcRequest) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withN(String.valueOf(1)));
        eav.put(":val2", new AttributeValue().withS(hcRequest.getStrategy()));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("completed = :val1 and strategy = :val2")
            .withExpressionAttributeValues(eav);

        return dynamoDBMapper.scan(HcRequest.class, scanExpression);
    }

    public static List<HcRequest> getCompletedRequests() {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":val1", new AttributeValue().withN(String.valueOf(1)));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("completed = :val1")
            .withExpressionAttributeValues(eav);

        return dynamoDBMapper.scan(HcRequest.class, scanExpression);
    }
}

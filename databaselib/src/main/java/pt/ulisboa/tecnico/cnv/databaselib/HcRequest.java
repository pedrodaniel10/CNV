package pt.ulisboa.tecnico.cnv.databaselib;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = DatabaseUtils.tableName)
public class HcRequest {

    private String requestId;
    private int width;
    private int height;
    private int x0;
    private int x1;
    private int y0;
    private int y1;
    private int xS;
    private int yS;
    private String strategy;
    private String map;
    private double metrics;

    @DynamoDBHashKey(attributeName = "request_id")
    public String getId() {
        return requestId;
    }

    public void setId(String requestId) {
        this.requestId = requestId;
    }

    @DynamoDBAttribute(attributeName = "width")
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @DynamoDBAttribute(attributeName = "height")
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @DynamoDBAttribute(attributeName = "x0")
    public int getX0() {
        return x0;
    }

    public void setX0(int x0) {
        this.x0 = x0;
    }

    @DynamoDBAttribute(attributeName = "x1")
    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    @DynamoDBAttribute(attributeName = "y0")
    public int getY0() {
        return y0;
    }

    public void setY0(int y0) {
        this.y0 = y0;
    }

    @DynamoDBAttribute(attributeName = "y1")
    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    @DynamoDBAttribute(attributeName = "xS")
    public int getxS() {
        return xS;
    }

    public void setxS(int xS) {
        this.xS = xS;
    }

    @DynamoDBAttribute(attributeName = "yS")
    public int getyS() {
        return yS;
    }

    public void setyS(int yS) {
        this.yS = yS;
    }

    @DynamoDBAttribute(attributeName = "strategy")
    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @DynamoDBAttribute(attributeName = "map")
    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    @DynamoDBAttribute(attributeName = "metrics")
    public double getMetrics() {
        return metrics;
    }

    public void setMetrics(double metrics) {
        this.metrics = metrics;
    }
}

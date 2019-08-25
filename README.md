# CNV - 2018/2019
Cloud Computing &amp; Virtualization project - HillClimbing@Cloud


## Requirements
Install the following tools:

- Maven 3.x.x
- Java Development Kit 7 (JDK 7)


## Structure and organization

The project is organized in 3 modules: 
- **databaselib**, a library that contains code to interact with DynamoDB.
- **loadbalancer**, which contains the code to run in the Load Balancer server.
- **web-server**, which contains the code to run in the Web server.

The first two modules use maven, and the last already has all the files compiled.

## How to compile

Execute the following command:

```
./create_archives.sh
```


## How to run

### WebServer

Place the .dat and .png files from the dataset in **web-server/datasets**.

Run **create_archives.sh** script.

Create an AWS ec2 server instance, with port 8000 open for TCP, and java 7 installed.
Download the [AWS Java SDK](http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip)
, and unzip it in the home directory.

Copy the **web-server.zip** archive to the instance, and unzip it:
```
scp -i CNV-AWS.pem ./web-server.zip ec2-user@<IP_ADDRESS>:~
```

Append the following, to **etc/rc.d/rc.local** (change the aws package version, if needed):

```
export CLASSPATH="$CLASSPATH:/home/ec2-user/web-server/pt/ulisboa/tecnico/cnv/server:/home/ec2-user/web-server:/home/ec2-user/db-lib:/home/ec2-user/aws-java-sdk-1.11.546/lib/aws-java-sdk-1.11.546.jar:/home/ec2-user/aws-java-sdk-1.11.546/third-party/lib/*:."
export _JAVA_OPTIONS="-XX:-UseSplitVerifier "$_JAVA_OPTIONS
cd /home/ec2-user/web-server
java WebServer
```

Create the **~/.aws** folder and put the following files in it:
 
- webserver.properties (example)
    ```
    server-region=us-east-1
    ```
- credentials (example)
    ```
    [default]

    aws_access_key_id=<ACCESS_KEY>

    aws_secret_access_key=<SECRET_ACCESS_KEY>
    ```

Create an image of this instance.

### LoadBalancer

Run **create_archives.sh** script (if you haven't already).

Create an AWS ec2 server instance, with port 8080 open for TCP, and java 7 installed. 
Download the [AWS Java SDK](http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip)
, and unzip it in the home directory.

Copy the **lb-bin.zip** archive to the instance, and unzip it:
```
scp -i CNV-AWS.pem ./lb-bin.zip ec2-user@<IP_ADDRESS>:~
```

Run the following (change the aws package version, if needed):

```
export CLASSPATH=$CLASSPATH:/home/ec2-user/db-lib:/home/ec2-user/aws-java-sdk-1.11.538/lib/aws-java-sdk-1.11.538.jar:/home/ec2-user/aws-java-sdk-1.11.538/third-party/lib/*:.
```

Create the **~/.aws** folder and put the following files in it:
 
- webserver.properties (example)
    ```
    server-region=us-east-1
    image-id=ami-0e38c8854738d9646
    key-name=CNV-lab-AWS
    sec-group=CNV-ssh+http
    instace-type=t2.micro
    subnet-id=subnet-ac760882
    ```
- credentials (example)
    ```
    [default]

    aws_access_key_id=<ACCESS_KEY>

    aws_secret_access_key=<SECRET_ACCESS_KEY>
    ```

To startup the LoadBalancer run the following command inside **lb-bin/**:

```
java pt.ulisboa.tecnico.cnv.loadbalancer.LoadBalancerApplication
```

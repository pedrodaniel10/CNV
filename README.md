# CNV
Cloud Computing &amp; Virtualization - 2018/19

## How to run

### WebServer

Run **create_archives.sh** script.

Create an AWS ec2 server instance (with port 8000 open for TCP), with java 7 installed.

Copy the **web-server.zip** archive to the instance, and unzip it:
```
scp -i CNV-AWS.pem ./web-server.zip ec2-user@<IP_ADDRESS>:~
```

Append the following, to **etc/rc.d/rc.local**:

```
export CLASSPATH="$CLASSPATH:/home/ec2-user/web-server/pt/ulisboa/tecnico/cnv/instrumentation"
export _JAVA_OPTIONS="-XX:-UseSplitVerifier "$_JAVA_OPTIONS
cd /home/ec2-user/web-server
java pt.ulisboa.tecnico.cnv.server.WebServer
```

Create an image of this instance.

### LoadBalancer

Run **create_archives.sh** script (if you haven't already).

Create an AWS ec2 server instance (with port 8080 open for TCP), with java 7 installed. 
Download the [AWS Java SDK](http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip)
, and unzip it in the home directory.

Copy the **lb-bin.zip** archive to the instance, and unzip it:
```
scp -i CNV-AWS.pem ./lb-bin.zip ec2-user@<IP_ADDRESS>:~
```

Append the following, to **etc/rc.d/rc.local**:

```
export CLASSPATH=$CLASSPATH:/home/ec2-user/aws-java-sdk-1.11.538/lib/aws-java-sdk-1.11.538.jar:/home/ec2-user/aws-java-sdk-1.11.538/third-party/lib/*:.
```

Create the **~/.aws** folder and put the following files in it:
 
- webserver.properties (example)
    ```
    server-region=us-east-1
    image-id=ami-060b9886531d37dea
    key-name=CNV-lab-AWS
    sec-group=CNV-ssh+http
    instace-type=t2.micro
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
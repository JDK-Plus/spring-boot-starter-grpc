<h3 align="center">A Springboot extension that integrates GRpc dependencies</h3>
<p align="center">
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/blob/master/LICENSE"><img src="https://img.shields.io/github/license/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/releases"><img src="https://img.shields.io/github/release/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/stargazers"><img src="https://img.shields.io/github/stars/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/network/members"><img src="https://img.shields.io/github/forks/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
</p>


## Import dependencies

```xml
<dependency>
    <groupId>plus.jdk.grpc</groupId>
    <artifactId>spring-boot-starter-grpc</artifactId>
    <version>1.1.07</version>
</dependency>
```

## Configuration items that need to be added

```
# Whether to open grpc server
plus.jdk.grpc.enabled=true

plus.jdk.grpc.client.enabled=true


# designated port
plus.jdk.grpc.port=10400

# Specifies the listening service address
plus.jdk.grpc.address=*

# Whether to support long connection
plus.jdk.grpc.enable-keep-alive=true

# long connection timeout disconnection time
plus.jdk.grpc.keep-alive-timeout=111

# NioEventLoopGroup master number of core threads
plus.jdk.grpc.master-thread-num=1

# NioEventLoopGroup worker number of core threads
plus.jdk.grpc.worker-thread-num=10

# The maximum number of bytes in a packet
plus.jdk.grpc.max-inbound-message-size=100000

# Maximum limit of request headers sent
plus.jdk.grpc.max-inbound-metadata-size=100000
```
## How to use after introduction

### Add Protobuf as follows：

```proto3
syntax = "proto3";

package plus.jdk.websocket.protoc;

option java_multiple_files = true;
option java_package = "plus.jdk.websocket.broadcast.test.protoc";
option java_outer_classname = "GreeterService";
option optimize_for = CODE_SIZE;


// The greeting service definition.
service Greeter {
    // Sends a greeting
    rpc SayHello (HelloRequest) returns (HelloReply) {}

    rpc SayHelloAgain (HelloRequest) returns (HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
    string name = 1;
}

// The response message containing the greetings
message HelloReply {
    string message = 1;
}
```
### Specify the global ServiceInterceptor.

You need to implement ` GrpcServiceGlobalInterceptorConfigurer ` and should be declared as a bean instance

```java

import org.springframework.stereotype.Component;

@Component
public class GrpcServiceGlobalInterceptorConfigurer implements GrpcServiceInterceptorConfigurer {

    private final RSACipherService rsaCipherService;

    @Override
    public void configureServerInterceptors(List<ServerInterceptor> interceptors) {
        GrpcAuthServerInterceptor grpcAuthServerInterceptor =
                new GrpcAuthServerInterceptor(rsaCipherService);
        interceptors.add(grpcAuthServerInterceptor);
    }
}
```


### How to define a Grpc service according to the above Protobuf structure

```java
package plus.jdk.grpc.test.grpc;

import io.grpc.stub.StreamObserver;
import plus.jdk.grpc.annotation.GrpcService;
import plus.jdk.grpc.test.grpc.interceptor.AuthServerInterceptor;
import plus.jdk.grpc.test.protoc.GreeterGrpc;
import plus.jdk.grpc.test.protoc.HelloReply;
import plus.jdk.grpc.test.protoc.HelloRequest;

@GrpcService(interceptors = {AuthServerInterceptor.class})
public class GreeterImplService extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void sayHelloAgain(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello again " + request.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
```

### How to call the Grpc service just defined

#### The definition declares a remote cluster of servers

```bash

# Example Start the configuration of the client
plus.jdk.grpc.client.enabled=true

# Specify a default connection address, which the @GrpcClient annotation uses by default
plus.jdk.grpc.client.default-service=MyGrpc://grpc-service-prod

# scheme address of the user-defined service
plus.jdk.grpc.client.resolvers[0].scheme=MyGrpc

# Specify the host address of the service
plus.jdk.grpc.client.resolvers[0].service-name=grpc-service-prod

# Specifies the list of remote GRPC services
plus.jdk.grpc.client.resolvers[0].hosts[0]=192.168.1.108:10202
plus.jdk.grpc.client.resolvers[0].hosts[1]=192.168.1.107:10202
```
#### Service Register

When you use k8s clusters, your cluster information must be registered and removed as nodes are started and destroyed, and you can do this by implementing the `IGrpcServiceRegister` interface

```java
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import plus.jdk.etcd.global.EtcdClient;
import plus.jdk.grpc.common.IGrpcServiceRegister;

@Slf4j
@AllArgsConstructor
public class GrpcServerServiceRegister implements IGrpcServiceRegister {

    private final EtcdClient etcdClient;

    @Override
    public void registerServiceNode() {
        log.info("registerServiceNode");
    }

    @Override
    public void updateNodeStatus() {
        log.info("updateNodeStatus");
    }

    @Override
    public void deregisterServiceNode() {
        log.info("deregisterServiceNode");
    }
}
```



#### Read the cluster configuration information from the configuration center (such as zookeeper, etcd, redis)

In many cases, to ensure high availability of services, cluster information is stored in the configuration center and
delivered in a unified manner, which facilitates the rapid addition of a node when a node fails or capacity is expanded.

You can do this by implementing the 'INameResolverConfigurer' interface. An example of reading a configuration from redis is shown below:

```java
import com.google.gson.Gson;
import io.etcd.jetcd.kv.TxnResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import plus.jdk.etcd.global.EtcdClient;
import plus.jdk.grpc.common.IGrpcServiceRegister;
import plus.jdk.grpc.config.GrpcPlusProperties;
import plus.jdk.grpc.model.GrpcNameResolverModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcServerServiceRegister extends WebApplicationObjectSupport implements IGrpcServiceRegister {

    private final EtcdClient etcdClient;

    private final BrandGrpcServerProperties properties;

    private URI serviceUri;

    private Integer port;

    private String registerPath;

    private final Gson gson;

    public GrpcServerServiceRegister(EtcdClient etcdClient, BrandGrpcServerProperties properties, Gson gson) {
        this.etcdClient = etcdClient;
        this.properties = properties;
        this.gson = gson;
    }

    protected void initRegisterInfo() {
        if (this.serviceUri != null) {
            return;
        }
        String serviceName = SpringContext.getProperty(properties.getServiceUriKey(), String.class);
        assert serviceName != null;
        this.serviceUri = URI.create(serviceName);
        this.port = SpringContext.getProperty(properties.getServicePortKey(), Integer.class);
        this.registerPath = String.join("/", new String[]{
                properties.getServiceRegisterPath(), serviceUri.getHost(), Helper.getIpAddress()
        });
    }

    private GrpcNameResolverModel getGrpcNameResolverModel() {
        initRegisterInfo();
        String userIp = Helper.getIpAddress();
        if(StringUtil.isEmpty(userIp)) {
            throw new RuntimeException("cat not get machine ip address");
        }
        GrpcNameResolverModel resolverModel = new GrpcNameResolverModel();
        resolverModel.setServiceName(serviceUri.getHost());
        resolverModel.setScheme(serviceUri.getScheme());
        resolverModel.setHosts(new ArrayList<>());
        resolverModel.getHosts().add(String.format("%s:%s", userIp, port));
        return resolverModel;
    }

    private Long computeNodeExpire() {
        GrpcPlusProperties grpcPlusProperties = SpringContext.getBean(GrpcPlusProperties.class);
        if (grpcPlusProperties == null) {
            return properties.getNodeExpire();
        }
        return grpcPlusProperties.getServiceRegisterInterval().getSeconds() * 2;
    }


    @Override
    public void registerServiceNode() {
        try {
            if(SpringContext.isDevelopment() || Boolean.parseBoolean(System.getProperty("grpc.service.not.register"))) {
                return;
            }
            initRegisterInfo();
            GrpcNameResolverModel resolverModel = getGrpcNameResolverModel();
            Long expire = computeNodeExpire();
            CompletableFuture<TxnResponse> future =
                    etcdClient.put(registerPath, resolverModel, expire * 2);
            log.info("registerServiceNode success, resolverModel:{}, ret:{}", resolverModel, future.get());
        } catch (Exception e) {
            log.info("registerServiceNode failed, msg:{}", e.getMessage());
        }
    }

    @Override
    public void updateNodeStatus() {
        registerServiceNode();
    }

    @Override
    public void deregisterServiceNode() {
        etcdClient.delete(registerPath);
    }
}
```

In addition, you can specify the cluster instance list synchronization period with the following configuration:

```bash
# Specify a refresh every 15 seconds
plus.jdk.grpc.client.name-refresh-rate=15
```

#### Service Discovery

In the previous text, we completed service registration through 'etcd', now let's do service discovery. Service discovery is actually implemented here by the caller, for example, the calling address is: `myGrpc://ouer-domain.service`, So the client needs to know what the IP list of all instances in the cluster corresponding to this scheme is.

There are two types of mechanisms here.

- One way is to update the local `name service` name resolution through timed scans, which means continuously reading this information from `ectd` or other storage locations where cluster configuration information is stored, such as pulling updates every 3 seconds
- Although scheduled pull is good, there is always a delay, so it is also possible to subscribe to changes in nodes in the cluster through a watch etcd key in the implementation to update data in real time

In this framework, the service discovers the need to implement the `INameResolverConfigurer` interface, which is defined as follows:

```java
public interface INameResolverConfigurer {

    /**
     * Refresh the instance list corresponding to the corresponding uri, which is executed every 10 seconds by default. Here, the logic of scheduled updates is implemented by default
     * The refresh cycle can be specified by configuring plus. jdk. grpc. client. name refresh rate=15
     */
    List<EquivalentAddressGroup> configurationName(URI targetUri);

    /**
     * Initialize all custom addresses, where you can subscribe to (watch) data from other configuration centers such as ectd and zookeeper to update the content under the name service in real-time
     */
    void configureNameResolvers(List<GrpcNameResolverModel> resolverModels);
}
```

Here is an example of implementing service discovery through ETCD:

```java
@Slf4j
public class GrpcGlobalNameResolverConfigurer implements INameResolverConfigurer {

    private final BrandGrpcClientProperties properties;

    private final RSACipherService rsaCipherService;

    private final CommonRedisService commonRedisService;

    private final EtcdClient etcdClient;

    private List<GrpcNameResolverModel> grpcNameResolverModels;

    private Watch.Watcher watcher;

    private final ScheduledExecutorService scheduledExecutorService;

    public GrpcGlobalNameResolverConfigurer(BrandGrpcClientProperties properties,
                                            RSACipherService rsaCipherService,
                                            CommonRedisService commonRedisService,
                                            EtcdClient etcdClient) {
        this.properties = properties;
        this.rsaCipherService = rsaCipherService;
        this.commonRedisService = commonRedisService;
        this.etcdClient = etcdClient;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(2);
        this.scheduledExecutorService.scheduleAtFixedRate(this::mergeAndScanService,
                5, 5, TimeUnit.SECONDS);
    }

    @Override
    public List<EquivalentAddressGroup> configurationName(URI targetUri) {
        HashMap<String, GrpcNameResolverModel> grpcNameResolverMap = CollectionUtil.toHashMap(grpcNameResolverModels, data -> String.format("%s://%s", data.getScheme(), data.getServiceName()));
        GrpcNameResolverModel nameResolverModel = grpcNameResolverMap.get(targetUri.toString());
        if (nameResolverModel == null) {
            return new ArrayList<>();
        }
        return nameResolverModel.toEquivalentAddressGroups();
    }

    @Override
    public void configureNameResolvers(List<GrpcNameResolverModel> resolverModels) {
        try {
            TablePrinter tablePrinter = new TablePrinter();
            mergeAndScanService();
            grpcNameResolverModels = mergeAndScanService();
            resolverModels.addAll(grpcNameResolverModels);
            tablePrinter.printTable(grpcNameResolverModels, GrpcNameResolverModel.class);
            log.info("configureNameResolvers success, grpcNameResolverModels:{}", grpcNameResolverModels);
        } catch (Exception | Error e) {
            log.info("configureNameResolvers failed, msg:{}", e.getMessage());
        }
    }

    private List<GrpcNameResolverModel> mergeAndScanService() {
        try {
            String configKey = "your etcd or zk config path";
            KeyValuePair<GrpcNameResolverModel[]> nameResolverModels =
                    etcdClient.getFirstKV(configKey, GrpcNameResolverModel[].class).get();
            this.grpcNameResolverModels = Arrays.asList(nameResolverModels.getValue());
            HashMap<String, GrpcNameResolverModel> grpcNameResolverMap =
                    CollectionUtil.toHashMap(grpcNameResolverModels, GrpcNameResolverModel::buildScheme);
            List<GrpcNameResolverModel> resolverModels = new ArrayList<>(grpcNameResolverMap.values());
            CompletableFuture<List<KeyValuePair<GrpcNameResolverModel>>> future =
                    etcdClient.scanByPrefix(properties.getServiceRegisterPath(), GrpcNameResolverModel.class);
            List<KeyValuePair<GrpcNameResolverModel>> keyValuePairs = future.get();
            for (KeyValuePair<GrpcNameResolverModel> keyValuePair : keyValuePairs) {
                GrpcNameResolverModel resolverModel = keyValuePair.getValue();
                if (resolverModel == null) {
                    continue;
                }
                if (grpcNameResolverMap.containsKey(resolverModel.buildScheme())) {
                    grpcNameResolverMap.get(resolverModel.buildScheme()).getHosts().addAll(resolverModel.getHosts());
                }
                resolverModels.add(keyValuePair.getValue());
            }
            this.grpcNameResolverModels = resolverModels;
            return resolverModels;
        } catch (Exception e) {
        }
        return new ArrayList<>();
    }
}
```


#### Specify the global 'GrpcClientInterceptor'

With the above, you need to implement `GrpcClientInterceptorConfigurer` method, add the corresponding Interceptor

```java
import org.springframework.stereotype.Component;

@Component
public class GrpcClientInterceptorGlobalConfigurer implements GrpcClientInterceptorConfigurer {
    

    @Override
    public void configureClientInterceptors(List<ClientInterceptor> interceptors) {
        // do something
        interceptors.add(new GrpcClientRSAInterceptor(rsaCipherService));
    }
}
```

#### Write code to make the remote call：

```java
import org.springframework.stereotype.Component;
import plus.jdk.grpc.annotation.GrpcClient;

import javax.annotation.Resource;

@Component
public class GRpcRunner implements ApplicationRunner {
    
    @Resource
    private GrpcSubClientFactory grpcSubClientFactory;

    @GrpcClient("MyGrpc://grpc-service-prod")
    private GreeterGrpc.GreeterBlockingStub greeterBlockingStub;

    /**
     * Here @GrpcClient default `plus.jdk.grpc.client.default-service` configuration items specified value
     */
    @GrpcClient
    private GreeterGrpc.GreeterBlockingStub greeterBlockingStubDefault;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        
        // send request,say hello
        HelloRequest request = HelloRequest.newBuilder().setName("jdk-plus").build();
        HelloReply reply = greeterBlockingStub.sayHello(request);
        log.info("sayHello data:{}, receive:{}", request, reply);
        
        // send request again
        reply = blockingStub.sayHelloAgain(request);
        log.info("sayHelloAgain data:{}, receive:{}", request, reply);
        TimeUnit.SECONDS.sleep(1);
    }
}
```

### About the compilation and packaging of protobuf files

Highly recommended here: `[protobuf-maven-plugin](https://github.com/xolstice/protobuf-maven-plugin/)`,

This component can be used to automatically build and generate the compiled Java code corresponding to Protobuf at compile time：`[usage](https://www.xolstice.org/protobuf-maven-plugin/usage.html)`
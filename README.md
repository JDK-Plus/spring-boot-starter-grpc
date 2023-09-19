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
import io.grpc.EquivalentAddressGroup;
import org.springframework.stereotype.Component;
import plus.jdk.grpc.client.INameResolverConfigurer;
import plus.jdk.grpc.model.GrpcNameResolverModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class GrpcGlobalNameResolverConfigurer implements INameResolverConfigurer {

    private final RSACipherService rsaCipherService;

    private final CommonRedisService commonRedisService;

    public GrpcGlobalNameResolverConfigurer(RSACipherService rsaCipherService, CommonRedisService commonRedisService) {
        this.rsaCipherService = rsaCipherService;
        this.commonRedisService = commonRedisService;
    }

    protected String getGrpcNameResolverKeys() {
        return "GrpcNameResolverKeys";
    }

    /**
     * This method is used to update the cluster list under the corresponding uri. 
     * By default, this method is performed every 10 seconds
     */
    @Override
    public List<EquivalentAddressGroup> configurationName(URI targetUri) {
        GrpcNameResolverModel nameResolverModel =
                commonRedisService.hget(getGrpcNameResolverKeys(), targetUri.toString(), GrpcNameResolverModel.class);
        if (nameResolverModel == null) {
            return new ArrayList<>();
        }
        return nameResolverModel.toEquivalentAddressGroups();
    }

    /**
     * This method is used to initialize the cluster list when the service is started
     */
    @Override
    public void configureNameResolvers(List<GrpcNameResolverModel> resolverModels) {
        commonRedisService.hScan(getGrpcNameResolverKeys(), "*", GrpcNameResolverModel.class, (result) -> {
            if (result == null || result.getData() == null) {
                return true;
            }
            resolverModels.add(result.getData());
            return true;
        });
    }
}
```

In addition, you can specify the cluster instance list synchronization period with the following configuration:

```bash
# Specify a refresh every 15 seconds
plus.jdk.grpc.client.name-refresh-rate=15
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
        int port = Integer.parseInt(grpcPort);
//        ManagedChannel channel = ManagedChannelBuilder.forTarget("MyGrpc://grpc-service-prod")
//                .usePlaintext().build();
//        GreeterGrpc.GreeterBlockingStub blockingStub = grpcSubClientFactory.createStub(GreeterGrpc.GreeterBlockingStub.class, channel);
        HelloRequest request = HelloRequest.newBuilder().setName("jdk-plus").build();
        HelloReply reply = greeterBlockingStub.sayHello(request);
        log.info("sayHello data:{}, receive:{}", request, reply);
        reply = blockingStub.sayHelloAgain(request);
        log.info("sayHelloAgain data:{}, receive:{}", request, reply);
        TimeUnit.SECONDS.sleep(1);
    }
}
```
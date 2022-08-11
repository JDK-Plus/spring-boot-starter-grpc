
<h3 align="center">一个集成GRpc依赖的Springboot扩展</h3>
<p align="center">
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/blob/master/LICENSE"><img src="https://img.shields.io/github/license/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/releases"><img src="https://img.shields.io/github/release/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/stargazers"><img src="https://img.shields.io/github/stars/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-grpc/network/members"><img src="https://img.shields.io/github/forks/JDK-Plus/spring-boot-starter-grpc.svg" /></a>
</p>


## 引入依赖

```xml
<dependency>
    <groupId>plus.jdk.grpc</groupId>
    <artifactId>spring-boot-starter-grpc</artifactId>
    <version>1.0.3</version>
</dependency>
```

## 需要添加的配置项

```
# 是否开启grpc server
plus.jdk.grpc.enabled=true

# 指定端口
plus.jdk.grpc.port=10400

# 指定监听的服务地址
plus.jdk.grpc.address=*

# 是否支持长连接
plus.jdk.grpc.enable-keep-alive=true

# 长连接超时断开时间
plus.jdk.grpc.keep-alive-timeout=111

# NioEventLoopGroup master核心线程数
plus.jdk.grpc.master-thread-num=1

# NioEventLoopGroup worker线程数
plus.jdk.grpc.worker-thread-num=10

# 数据包最大多少字节
plus.jdk.grpc.max-inbound-message-size=100000

# 发送的请求头最大限制
plus.jdk.grpc.max-inbound-metadata-size=100000
```
## 引入后如何使用

### 添加Protobuf如下：

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

### 如何根据上述的Protobuf结构定义一个Grpc service

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

### 如何调用上文中定义的GRPC服务

```java
public class GRpcRunner implements ApplicationRunner {

    @Value("${plus.jdk.grpc.port}")
    private String grpcPort;

    @Resource
    private GrpcSubClientFactory grpcSubClientFactory;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int port = Integer.parseInt(grpcPort);
        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", port)
                .usePlaintext().build();
        GreeterGrpc.GreeterBlockingStub blockingStub = grpcSubClientFactory.createStub(GreeterGrpc.GreeterBlockingStub.class, channel);
        HelloRequest request = HelloRequest.newBuilder().setName("jdk-plus").build();
        HelloReply reply = blockingStub.sayHello(request);
        log.info("sayHello data:{}, receive:{}", request, reply);
        reply = blockingStub.sayHelloAgain(request);
        log.info("sayHelloAgain data:{}, receive:{}", request, reply);
        TimeUnit.SECONDS.sleep(1);
    }
}
```
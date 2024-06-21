
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
    <version>1.1.07</version>
</dependency>
```

## 需要添加的配置项

```
# 是否开启grpc server
plus.jdk.grpc.enabled=true

plus.jdk.grpc.client.enabled=true

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

### 指定全局的 ServiceInterceptor.

你需要实现 `GrpcServiceGlobalInterceptorConfigurer`， 并将其声明为一个bean实例

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

### 如何调用上文中定义的GRPC服务（客户端调用）


#### 定义声明一个远端的服务器集群

```bash

# 启动客户端的配置
plus.jdk.grpc.client.enabled=true

# 指定一个默认的连接地址, 指定后 @GrpcClient 注解就默认使用该值
plus.jdk.grpc.client.default-service=MyGrpc://grpc-service-prod

# 指定服务的scheme地址
plus.jdk.grpc.client.resolvers[0].scheme=MyGrpc

# 指定服务的host地址
plus.jdk.grpc.client.resolvers[0].service-name=grpc-service-prod

# 指定远端的GRPC服务列表
plus.jdk.grpc.client.resolvers[0].hosts[0]=192.168.1.108:10202
plus.jdk.grpc.client.resolvers[0].hosts[1]=192.168.1.107:10202
```

#### 服务注册

当你在使用k8s集群的时候，你的集群信息必须随着节点的启动、销毁执行相应的注册、摘除工作，你可以通过实现`IGrpcServiceRegister`接口来完成这个事情

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

##### 服务注册。从配置配置中心（如zookeeper、etcd、redis）读取集群配置信息

在很多情况下，为了保障服务的高可用性，我们会将集群信息存储在配置中心中统一下发，便于某个节点出现故障或扩容时快速新增节点.

你可以通过实现 `IGrpcServiceRegister` 接口来实现上述功能。下文中将给出一个从redis中读取配置的示例：

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

另外，你可以通过如下配置来指定集群实例列表同步周期：


```bash
# 指定每15秒刷新一次
plus.jdk.grpc.client.name-refresh-rate=15
```

#### 服务发现

在上文中我们通过`etcd`来完成了服务注册，现在来做一下服务发现。服务发现其实是在调用方这里来实现的，例如，调用地址是: `myGrpc://ouer-domain.service`，那么客户端就需要知道这个`scheme`后面对应的集群中的所有实例的ip列表是什么。

这里的机制一共有两种。

- 一种是通过定时扫来更新本地的`name service`名称解析，即不断的定时上`ectd`或其他存储集群配置信息的地方去读取这个信息，比如每3秒去拉取更新一次
- 定时拉取虽好，但是总有延迟，所以也可以在实现里面通过watch etcd key的方式来订阅集群中节点的变化来实时更新数据

在这个框架中，服务发现需要实现 `INameResolverConfigurer`接口，该接口定义如下：

```java
public interface INameResolverConfigurer {

    /**
     * 刷新对应的uri对应的实例列表, 默认每10秒执行一次， 这里默认实现了定时更新的逻辑
     * 刷新周期可以通过 plus.jdk.grpc.client.name-refresh-rate=15 配置来指定
     */
    List<EquivalentAddressGroup> configurationName(URI targetUri);

    /**
     * 初始化所有自定义的地址， 可以在这里订阅(watch) ectd、zookeeper等其他配置中心的数据来实时更新name service下的内容
     */
    void configureNameResolvers(List<GrpcNameResolverModel> resolverModels);
}
```


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

#### 指定全局的`GrpcClientInterceptor`

同上文，你需要实现 `GrpcClientInterceptorConfigurer` 方法，添加对应的Interceptor

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

#### 编写代码执行远程调用：

你可以使用 `@GrpcClient` 注解来申明一个Grpc 调用的 client， 示例如下：

```java
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

@Component
public class GRpcRunner implements ApplicationRunner {

    @Value("${plus.jdk.grpc.port}")
    private String grpcPort;

    @Resource
    private GrpcSubClientFactory grpcSubClientFactory;

    @GrpcClient("MyGrpc://grpc-service-prod")
    private GreeterGrpc.GreeterBlockingStub greeterBlockingStub;

    /**
     * 这里 @GrpcClient 默认使用 `plus.jdk.grpc.client.default-service` 配置项指定的值
     */
    @GrpcClient
    private GreeterGrpc.GreeterBlockingStub greeterBlockingStubDefault;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        HelloRequest request = HelloRequest.newBuilder().setName("jdk-plus").build();
        HelloReply reply = greeterBlockingStub.sayHello(request);
        log.info("sayHello data:{}, receive:{}", request, reply);
        reply = blockingStub.sayHelloAgain(request);
        log.info("sayHelloAgain data:{}, receive:{}", request, reply);
        TimeUnit.SECONDS.sleep(1);
    }
}
```

### 关于protobuf文件的编译以及打包

此处强烈推荐 `[protobuf-maven-plugin](https://github.com/xolstice/protobuf-maven-plugin/)`,

对应的该组件可以直接在编译时自动构建生成protobuf对应的编译后的java代码，详细的使用方法请参见：`[usage](https://www.xolstice.org/protobuf-maven-plugin/usage.html)`
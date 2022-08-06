package plus.jdk.grpc.global;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.unit.DataSize;
import plus.jdk.grpc.annotation.GrpcService;
import plus.jdk.grpc.common.GrpcUtils;
import plus.jdk.grpc.config.GrpcPlusProperties;
import plus.jdk.grpc.model.GrpcServiceDefinition;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static plus.jdk.grpc.common.GrpcUtils.DOMAIN_SOCKET_ADDRESS_PREFIX;
import static plus.jdk.grpc.config.GrpcPlusProperties.ANY_IP_ADDRESS;

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;


@Slf4j
public class GrpcServerFactory {

    private final ApplicationContext applicationContext;

    private final BeanFactory beanFactory;

    private final GrpcPlusProperties properties;

    public Server createServer() {
        NettyServerBuilder builder = newServerBuilder();
        if (this.properties.isEnableKeepAlive()) {
            builder.keepAliveTime(this.properties.getKeepAliveTime().toNanos(), TimeUnit.NANOSECONDS)
                    .keepAliveTimeout(this.properties.getKeepAliveTimeout().toNanos(), TimeUnit.NANOSECONDS);
        }
        builder.permitKeepAliveTime(this.properties.getPermitKeepAliveTime().toNanos(), TimeUnit.NANOSECONDS)
                .permitKeepAliveWithoutCalls(this.properties.isPermitKeepAliveWithoutCalls());
        final Set<String> serviceNames = new LinkedHashSet<>();

        for (final GrpcServiceDefinition service : this.findGrpcServices()) {
            final String serviceName = service.getDefinition().getServiceDescriptor().getName();
            if (!serviceNames.add(serviceName)) {
                throw new IllegalStateException("Found duplicate service implementation: " + serviceName);
            }
            log.info("Registered gRPC service: " + serviceName + ", bean: " + service.getBeanName() + ", class: "
                    + service.getBeanClazz().getName());
            builder.addService(service.getDefinition());
        }
        final DataSize maxInboundMessageSize = this.properties.getMaxInboundMessageSize();
        if (maxInboundMessageSize != null) {
            builder.maxInboundMessageSize((int) maxInboundMessageSize.toBytes());
        }
        final DataSize maxInboundMetadataSize = this.properties.getMaxInboundMetadataSize();
        if (maxInboundMetadataSize != null) {
            builder.maxInboundMetadataSize((int) maxInboundMetadataSize.toBytes());
        }
        return builder.build();
    }

    protected NettyServerBuilder newServerBuilder() {
        final String address = properties.getAddress();
        final int port = properties.getPort();
        if (address.startsWith(DOMAIN_SOCKET_ADDRESS_PREFIX)) {
            final String path = GrpcUtils.extractDomainSocketAddressPath(address);
            return NettyServerBuilder.forAddress(new DomainSocketAddress(path))
                    .channelType(EpollServerDomainSocketChannel.class)
                    .bossEventLoopGroup(new EpollEventLoopGroup(properties.getMasterThreadNum()))
                    .workerEventLoopGroup(new EpollEventLoopGroup(properties.getWorkerThreadNum()));
        } else if (ANY_IP_ADDRESS.equals(address)) {
            return NettyServerBuilder.forPort(port);
        } else {
            return NettyServerBuilder.forAddress(new InetSocketAddress(InetAddresses.forString(address), port));
        }
    }

    public GrpcServerFactory(GrpcPlusProperties properties,
                             BeanFactory beanFactory,
                             ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.beanFactory = beanFactory;
        this.properties = properties;
    }

    protected Collection<GrpcServiceDefinition> findGrpcServices(){
        Collection<String> beanNames =
                Arrays.asList(this.applicationContext.getBeanNamesForAnnotation(GrpcService.class));
        List<GrpcServiceDefinition> definitions = Lists.newArrayListWithCapacity(beanNames.size());
        GlobalGrpcInterceptorRegistry globalServerInterceptorRegistry =
                applicationContext.getBean(GlobalGrpcInterceptorRegistry.class);
        for (String beanName : beanNames) {
            BindableService bindableService = this.applicationContext.getBean(beanName, BindableService.class);
            ServerServiceDefinition serviceDefinition = bindableService.bindService();
            GrpcService grpcServiceAnnotation = applicationContext.findAnnotationOnBean(beanName, GrpcService.class);
            if(grpcServiceAnnotation == null) {
                continue;
            }
            serviceDefinition =
                    bindInterceptors(serviceDefinition, grpcServiceAnnotation, globalServerInterceptorRegistry);
            definitions.add(new GrpcServiceDefinition(beanName, bindableService.getClass(), serviceDefinition));
            log.info("Had found gRPC service: {}, bean: {}, class: {}", serviceDefinition.getServiceDescriptor().getName(),
                    beanName, bindableService.getClass().getName());
        }
        return definitions;
    }

    private ServerServiceDefinition bindInterceptors(final ServerServiceDefinition serviceDefinition,
                                                     final GrpcService grpcServiceAnnotation,
                                                     final GlobalGrpcInterceptorRegistry globalServerInterceptorRegistry) {
        final List<ServerInterceptor> interceptors = Lists.newArrayList();
        interceptors.addAll(globalServerInterceptorRegistry.getServerInterceptors());
        for (final Class<? extends ServerInterceptor> interceptorClass : grpcServiceAnnotation.interceptors()) {
            final ServerInterceptor serverInterceptor;
            if (this.applicationContext.getBeanNamesForType(interceptorClass).length > 0) {
                serverInterceptor = this.applicationContext.getBean(interceptorClass);
            } else {
                try {
                    serverInterceptor = interceptorClass.getConstructor().newInstance();
                } catch (final Exception e) {
                    throw new BeanCreationException("Failed to create interceptor instance", e);
                }
            }
            interceptors.add(serverInterceptor);
        }
        if (grpcServiceAnnotation.sortInterceptors()) {
            globalServerInterceptorRegistry.sortInterceptors(interceptors);
        }
        return ServerInterceptors.interceptForward(serviceDefinition, interceptors);
    }
}

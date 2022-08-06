package plus.jdk.grpc.global;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import com.google.common.collect.ImmutableList;

import io.grpc.ServerInterceptor;
import plus.jdk.grpc.config.GrpcPlusProperties;


public class GlobalGrpcInterceptorRegistry {

    private final ApplicationContext applicationContext;

    private final GrpcPlusProperties grpcPlusProperties;

    private ImmutableList<ServerInterceptor> sortedServiceInterceptors;

    public GlobalGrpcInterceptorRegistry(GrpcPlusProperties properties, ApplicationContext applicationContext){
        this.applicationContext = applicationContext;
        this.grpcPlusProperties = properties;
    }

    public ImmutableList<ServerInterceptor> getServerInterceptors() {
        if (this.sortedServiceInterceptors == null) {
            this.sortedServiceInterceptors = ImmutableList.copyOf(initServerInterceptors());
        }
        return this.sortedServiceInterceptors;
    }

    protected List<ServerInterceptor> initServerInterceptors() {
        final List<ServerInterceptor> interceptors = new ArrayList<>();
        for (GrpcServiceInterceptorConfigurer configurer : this.applicationContext
                .getBeansOfType(GrpcServiceInterceptorConfigurer.class).values()) {
            configurer.configureServerInterceptors(interceptors);
        }
        sortInterceptors(interceptors);
        return interceptors;
    }

    public void sortInterceptors(final List<? extends ServerInterceptor> interceptors) {
        interceptors.sort(AnnotationAwareOrderComparator.INSTANCE);
    }
}

package plus.jdk.grpc.config;

import io.grpc.ClientInterceptor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import plus.jdk.grpc.model.GrpcNameResolver;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "plus.jdk.grpc.client")
public class GrpcPlusClientProperties {

    /**
     * 是否开启客户端
     */
    private boolean enabled = false;

    /**
     * 定义集群相关明细
     */
    private List<GrpcNameResolver> resolvers = new ArrayList<>();
}

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
     * 默认连接的服务地址。如果你要连接的grpc服务只有一个，那就用这个指定吧。
     * 这样子一来就不用每次都在GrpcClient注解上声明了
     */
    private String defaultService = "";

    /**
     * 定义集群相关明细
     */
    private List<GrpcNameResolver> resolvers = new ArrayList<>();
}

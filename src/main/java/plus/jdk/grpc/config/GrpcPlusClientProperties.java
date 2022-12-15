package plus.jdk.grpc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import plus.jdk.grpc.model.GrpcNameResolverModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
     * 多久刷新一次集群对应的ip列表
     */
    private Integer nameRefreshRate = 10;

    /**
     * 定义集群相关明细
     */
    private List<GrpcNameResolverModel> resolvers = new ArrayList<>();
}

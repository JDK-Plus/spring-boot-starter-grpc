package plus.jdk.grpc.config;

import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Data
@ConfigurationProperties(prefix = "plus.jdk.grpc")
public class GrpcPlusProperties {

    public static final String ANY_IP_ADDRESS = "*";

    public static final String ANY_IPv4_ADDRESS = "0.0.0.0";

    public static final String ANY_IPv6_ADDRESS = "::";


    private boolean enabled = false;

    /**
     * 提供的grpc服务监听哪个端口
     */
    private Integer port = 10240;

    /**
     * 服务名称, 保留字段，目前暂时没用
     */
    private String serviceUri = "my-grpc://service-name";

    /**
     * 主线程核心线程数
     */
    private Integer masterThreadNum = 1;

    /**
     * 主线程核心线程数
     */
    private Integer workerThreadNum = 0;

    /**
     * 服务地址
     */
    private String address = ANY_IP_ADDRESS;


    private boolean enableKeepAlive;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration keepAliveTime = Duration.of(2, ChronoUnit.HOURS);

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration keepAliveTimeout = Duration.of(20, ChronoUnit.SECONDS);

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration permitKeepAliveTime = Duration.of(5, ChronoUnit.MINUTES);

    @DurationUnit(ChronoUnit.SECONDS)
    private boolean permitKeepAliveWithoutCalls = false;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration maxConnectionIdle = null;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration maxConnectionAge = null;

    @DurationUnit(ChronoUnit.SECONDS)
    private Duration maxConnectionAgeGrace = null;

    @DataSizeUnit(DataUnit.BYTES)
    private DataSize maxInboundMessageSize = null;

    @DataSizeUnit(DataUnit.BYTES)
    private DataSize maxInboundMetadataSize = null;

    private boolean healthServiceEnabled = true;

    private boolean reflectionServiceEnabled = true;


}

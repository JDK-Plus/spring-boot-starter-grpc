package plus.jdk.grpc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "plus.jdk.grpc.client")
public class GrpcPlusClientProperties {
    private boolean enabled = false;
}

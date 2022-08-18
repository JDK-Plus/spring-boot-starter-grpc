package plus.jdk.grpc.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import plus.jdk.grpc.annotation.EnableGrpcClient;
import plus.jdk.grpc.config.GrpcPlusClientProperties;

@Slf4j
@Configuration
@EnableGrpcClient
@ConditionalOnProperty(prefix = "plus.jdk.grpc.client", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GrpcPlusClientProperties.class)
public class GrpcClientAutoConfiguration {

}

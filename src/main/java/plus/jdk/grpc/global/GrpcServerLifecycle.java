package plus.jdk.grpc.global;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import plus.jdk.grpc.common.IGrpcServiceRegister;

@Slf4j
public class GrpcServerLifecycle implements SmartLifecycle {

    private Server server;

    private final GrpcServerFactory grpcServerFactory;

    public GrpcServerLifecycle(GrpcServerFactory grpcServerFactory) {
        this.grpcServerFactory = grpcServerFactory;
    }

    @Override
    public void start() {
        try {
            final Server localServer = this.grpcServerFactory.createServer();
            this.server = localServer;
            localServer.start();
            grpcServerFactory.updateGrpcServiceStatus(IGrpcServiceRegister::registerServiceNode);
        }catch (Exception e) {
            log.error("Failed to start grpc server");
        }
    }

    @Override
    public void stop() {
        grpcServerFactory.updateGrpcServiceStatus(IGrpcServiceRegister::deregisterServiceNode);
        this.server.shutdown();
    }

    @Override
    public boolean isRunning() {
        return this.server != null && !this.server.isShutdown();
    }
}

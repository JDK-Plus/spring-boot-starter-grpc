package plus.jdk.grpc.client;

import com.google.common.collect.Lists;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;
import plus.jdk.grpc.annotation.GrpcClient;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

public class GrpcClientBeanPostProcessor implements BeanPostProcessor {

    private final ApplicationContext applicationContext;

    private final GrpcSubClientFactory grpcSubClientFactory;

    public GrpcClientBeanPostProcessor(ApplicationContext context, GrpcSubClientFactory grpcSubClientFactory) {
        this.applicationContext = context;
        this.grpcSubClientFactory = grpcSubClientFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        processFields(bean.getClass(), bean);
        processMethods(bean.getClass(), bean);
        return bean;
    }

    private void processFields(final Class<?> clazz, final Object bean) {
        for (final Field field : clazz.getDeclaredFields()) {
            final GrpcClient annotation = AnnotationUtils.findAnnotation(field, GrpcClient.class);
            if (annotation == null) {
                continue;
            }
            if (!AbstractStub.class.isAssignableFrom(field.getType())) {
                throw new BeanInstantiationException(field.getType(), "Unsupported grpc stub or channel type");
            }
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean,
                    processInjectionPoint((Class<? extends AbstractStub>) field.getType(), annotation));
        }
    }

    private void processMethods(final Class<?> clazz, final Object bean) {
        for (final Method method : clazz.getDeclaredMethods()) {
            final GrpcClient annotation = AnnotationUtils.findAnnotation(method, GrpcClient.class);
            if (annotation == null) {
                continue;
            }
            final Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new BeanDefinitionStoreException(
                        "Method " + method + " doesn't have exactly one parameter.");
            }
            for(Class<?> paramType: paramTypes) {
                if (!AbstractStub.class.isAssignableFrom(paramType)) {
                    throw new BeanInstantiationException(paramType, "Unsupported grpc stub or channel type");
                }
                ReflectionUtils.makeAccessible(method);
                ReflectionUtils.invokeMethod(method, bean,
                        processInjectionPoint((Class<? extends AbstractStub>) paramTypes[0], annotation));
            }
        }
    }


    protected <T extends AbstractStub<T>>
    T processInjectionPoint(final Class<T> injectionType,
                                          final GrpcClient annotation) {
        Environment environment = applicationContext.getEnvironment();
        final List<ClientInterceptor> interceptors = interceptorsFromAnnotation(annotation);
        String address = annotation.value();
        try{
            URI uri = URI.create(address);
        }catch (Exception e) {
            address = environment.getProperty(address);
        }
        return grpcSubClientFactory.createStub(injectionType, address, interceptors.toArray(new ClientInterceptor[]{}));
    }

    protected List<ClientInterceptor> interceptorsFromAnnotation(GrpcClient grpcClient) {
        final List<ClientInterceptor> list = Lists.newArrayList();
        for (final Class<? extends ClientInterceptor> interceptorClass : grpcClient.interceptors()) {
            final ClientInterceptor clientInterceptor;
            if (this.applicationContext.getBeanNamesForType(interceptorClass).length > 0) {
                clientInterceptor = this.applicationContext.getBean(interceptorClass);
            } else {
                try {
                    clientInterceptor = interceptorClass.getConstructor().newInstance();
                } catch (final Exception e) {
                    throw new BeanCreationException("Failed to create interceptor instance", e);
                }
            }
            list.add(clientInterceptor);
        }
        return list;
    }
}

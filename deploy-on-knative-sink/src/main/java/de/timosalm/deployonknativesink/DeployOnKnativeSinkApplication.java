package de.timosalm.deployonknativesink;

import io.cloudevents.spring.messaging.CloudEventMessageConverter;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;

@SpringBootApplication
public class DeployOnKnativeSinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeployOnKnativeSinkApplication.class, args);
    }

    @Configuration
    public static class CloudEventMessageConverterConfiguration {
        @Bean
        public CloudEventMessageConverter cloudEventMessageConverter() {
            return new CloudEventMessageConverter();
        }
    }

    @Configuration
    public static class CloudEventHandlerConfiguration implements CodecCustomizer {
        @Override
        public void customize(CodecConfigurer configurer) {
            configurer.customCodecs().register(new CloudEventHttpMessageReader());
            configurer.customCodecs().register(new CloudEventHttpMessageWriter());
        }
    }

    @Bean
    public DefaultKnativeClient knativeClient() {
        return new DefaultKnativeClient();
    }

}

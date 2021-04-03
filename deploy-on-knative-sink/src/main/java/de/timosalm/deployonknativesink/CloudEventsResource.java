package de.timosalm.deployonknativesink;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.function.Consumer;

@RestController
public class CloudEventsResource {

    private static final Logger log = LoggerFactory.getLogger(CloudEventsResource.class);

    private final DefaultKnativeClient knativeClient;

    public CloudEventsResource(final DefaultKnativeClient knativeClient) {
        this.knativeClient = knativeClient;
    }

    @Bean
    public Consumer<CloudEvent> deployOnKnative() {
        return event -> {
            log.info("CloudEvent with id " + event.getId() + " and subject " + event.getSubject() + " received");
            if ("Image".equals(event.getExtension("kind"))) {
                try {
                    var payload = getCloudEventPayload(event);
                    log.info("Payload of CloudEvent " + event.getId() + ": " + payload.toString());

                    if (payload.isValid()) {
                        deployOnKnative(payload);
                    } else {
                        log.error("Payload of CloudEvent " + event.getId() + " is invalid");
                    }
                } catch (IOException e) {
                    log.error("Deserialization of CloudEvent with id " + event.getId() + " failed.");
                }
            } else {
                log.error("CloudEvent payload Kubernetes object has to be of kind Image");
            }
        };
    }

    private CloudEventPayload getCloudEventPayload(CloudEvent event) throws IOException {
        return new ObjectMapper().readValue(event.getData().toBytes(), CloudEventPayload.class);
    }

    private void deployOnKnative(CloudEventPayload payload) {
        var service = knativeClient.inNamespace(payload.getNamespace())
                .services().withName(payload.getName()).get();
        if (service != null && containerImagesMatch(payload.getLatestImage(), service)) {
            log.info("No update for service " + payload.getName() + "Container image up tp date");
        } else {
            var newService = createService(payload);
            knativeClient.services().inNamespace(payload.getNamespace()).createOrReplace(newService);
        }
    }

    private Service createService(CloudEventPayload payload) {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(payload.getName())
                .withNamespace(payload.getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addToContainers(new ContainerBuilder().withImage(payload.getLatestImage()).build())
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private boolean containerImagesMatch(String containerImage, Service service) {
        return service.getSpec().getTemplate().getSpec().getContainers().stream()
                .filter(c -> containerImage.equals(c.getImage())).count() == 1;
    }
}

package de.timosalm.deployonknativesink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.util.StringUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudEventPayload {

    private Status status;
    private Metadata metadata;

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getName() {
        return metadata == null ? null : metadata.name;
    }

    public String getNamespace() {
        return metadata == null ? "default" : metadata.namespace;
    }

    public String getLatestImage() {
        return status == null ? null : status.latestImage;
    }

    @Override
    public String toString() {
        return "CloudEventPayload [name=" + getName() + ", namespace=" + getNamespace()
                + ", latestImage=" + getLatestImage() + "]";
    }

    public boolean isValid() {
        return StringUtils.hasText(getName()) && StringUtils.hasText(getNamespace())
                && StringUtils.hasText(getLatestImage());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private String latestImage;

        public void setLatestImage(String latestImage) {
            this.latestImage = latestImage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String name;
        private String namespace;

        public void setName(String name) {
            this.name = name;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }

}

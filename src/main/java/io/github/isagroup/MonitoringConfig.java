package io.github.isagroup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringConfig {
    private long delayAfterCompletion;
    private long fixedRateStore;
    private long fixedRateExport;

    // Getters and Setters
    public long getDelayAfterCompletion() {
        return delayAfterCompletion;
    }

    public void setDelayAfterCompletion(long delayAfterCompletion) {
        this.delayAfterCompletion = delayAfterCompletion;
    }

    public long getFixedRateStore() {
        return fixedRateStore;
    }

    public void setFixedRateStore(long fixedRateStore) {
        this.fixedRateStore = fixedRateStore;
    }

    public long getFixedRateExport() {
        return fixedRateExport;
    }

    public void setFixedRateExport(long fixedRateExport) {
        this.fixedRateExport = fixedRateExport;
    }
}
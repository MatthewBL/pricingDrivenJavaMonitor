package io.github.isagroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

@Component
public class MonitoringInterceptor implements HandlerInterceptor {
    private final ConcurrentMap<String, RequestData> ongoingRequests = new ConcurrentHashMap<>();
    private final Map<String, String> accumulatedData = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private PricingContext pricingContext;

    @Value("${monitoring.delay.afterCompletion}")
    private long delayAfterCompletion;

    @Value("${monitoring.individualMonitoring.enabled}")
    private boolean individualMonitoringEnabled;

    @Value("${monitoring.enabled}")
    private boolean monitoringEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestId = UUID.randomUUID().toString();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        long startTime = System.currentTimeMillis();

        ongoingRequests.put(requestId, new RequestData(uri, method, pricingContext.getUserPlan(), startTime));

        request.setAttribute("requestId", requestId);

        System.out.println("Before handling the request: " + request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        System.out.println("After completing the request: " + request.getRequestURI());

        String requestId = (String) request.getAttribute("requestId");

        if (individualMonitoringEnabled) {
            addDataToAccumulatedData();
            writeAccumulatedDataToCsv();
        }

        // Schedule the removal of the ongoing request after the configured delay
        scheduler.schedule(() -> ongoingRequests.remove(requestId), delayAfterCompletion, TimeUnit.MILLISECONDS);
    }

    @Scheduled(fixedRateString = "${monitoring.fixedRate.store}") // runs every configured interval
    public void storeOngoingRequestsInMap() {
        if (individualMonitoringEnabled) {
            return;
        }

        addDataToAccumulatedData();
    }

    private void addDataToAccumulatedData() {
        String timestamp = DATE_FORMAT.format(new Date());

        if (monitoringEnabled) {
            // Measure CPU usage
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            long totalPhysicalMemorySize = osBean.getTotalPhysicalMemorySize();
            long freePhysicalMemorySize = osBean.getFreePhysicalMemorySize();
            long usedPhysicalMemorySize = totalPhysicalMemorySize - freePhysicalMemorySize;
            double usedMemoryInGB = (double) usedPhysicalMemorySize / (1024 * 1024 * 1024);

            for (Map.Entry<String, RequestData> entry : ongoingRequests.entrySet()) {
                RequestData requestData = entry.getValue();
                long elapsedTime = System.currentTimeMillis() - requestData.getStartTime();
                accumulatedData.put(timestamp + 
                                    ", " + entry.getKey(), 
                                    requestData.getUri() + ", " + 
                                    requestData.getMethod() + ", " + 
                                    requestData.getUserPlan() + ", " + 
                                    elapsedTime + "ms, " + 
                                    "CPU Load: " + String.format("%.2f", cpuLoad) + "%" +
                                    ", Memory Load: " + String.format("%.6f", usedMemoryInGB) + "GB");
            }
            System.out.println("Stored ongoing requests in map at " + timestamp + " with CPU load: " + String.format("%.2f", cpuLoad) + "%");
            System.out.println("Stored ongoing requests in map at " + timestamp + " with Memory load: " + String.format("%.6f", usedMemoryInGB) + "GB");
        }
        else {
            for (Map.Entry<String, RequestData> entry : ongoingRequests.entrySet()) {
                RequestData requestData = entry.getValue();
                long elapsedTime = System.currentTimeMillis() - requestData.getStartTime();
                accumulatedData.put(timestamp + 
                                    ", " + entry.getKey(), 
                                    requestData.getUri() + ", " + 
                                    requestData.getMethod() + ", " + 
                                    requestData.getUserPlan() + ", " + 
                                    elapsedTime + "ms");
            }
            System.out.println("Stored ongoing requests in map at " + timestamp);
        }

        if (individualMonitoringEnabled) {
            ongoingRequests.clear();
        }
    }

    @Scheduled(fixedRateString = "${monitoring.fixedRate.export}") // runs every configured interval
    public void exportAccumulatedDataToCsv() {
        if (individualMonitoringEnabled) {
            return;
        }

        writeAccumulatedDataToCsv();
    }

    public void writeAccumulatedDataToCsv() {
        try (FileWriter writer = new FileWriter("ongoing_requests.csv", true)) {
            if (monitoringEnabled) {
                writer.append("Timestamp, Request ID, URI, Method, Pricing, Elapsed Time, CPU Load, Memory Load\n");
            }
            else {
                writer.append("Timestamp, Request ID, URI, Method, Pricing, Elapsed Time\n");
            }
            for (Map.Entry<String, String> entry : accumulatedData.entrySet()) {
                writer.append(entry.getKey())
                      .append(", ")
                      .append(entry.getValue())
                      .append("\n");
            }
            accumulatedData.clear();
            System.out.println("Exported accumulated data to CSV and cleared the map");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void warmUpCpuLoad() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = 0.0;
        int warmUpTries = 0;
        
        // Keep trying until we get a value that isn't 0% or 100% (max 100 tries)
        while ((cpuLoad == 0.0 || cpuLoad == 1.0) && warmUpTries < 100) {
            cpuLoad = osBean.getSystemCpuLoad();
            warmUpTries++;
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("CPU Load warmed up: " + cpuLoad * 100 + "% after " + warmUpTries + " tries.");
    }

    @Scheduled(fixedRateString = "${monitoring.CPU.update}")
    public void scheduleCpuLoadMonitoring() {
        if (!monitoringEnabled) {
            return;
        }
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        osBean.getSystemCpuLoad();
    }

    private static class RequestData {
        private final String uri;
        private final String method;
        private final String userPlan;
        private final long startTime;

        public RequestData(String uri, String method, String userPlan, long startTime) {
            this.uri = uri;
            this.method = method;
            this.userPlan = userPlan;
            this.startTime = startTime;
        }

        public String getUri() {
            return uri;
        }

        public String getMethod() {
            return method;
        }

        public String getUserPlan() {
            return userPlan;
        }

        public long getStartTime() {
            return startTime;
        }
    }
}
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
    private final ConcurrentMap<String, String> ongoingRequests = new ConcurrentHashMap<>();
    private final Map<String, String> accumulatedData = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private PricingContext pricingContext;

    @Value("${monitoring.delay.afterCompletion}")
    private long delayAfterCompletion;

    @Value("${monitoring.individualMonitoring.enabled}")
    private boolean individualMonitoringEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = UUID.randomUUID().toString();

        String uri = request.getRequestURI();
        String method = request.getMethod();

        ongoingRequests.put(requestId, uri + ", " + method + ", " + pricingContext.getUserPlan());

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

        // Measure CPU usage
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = osBean.getSystemCpuLoad();

        for (Map.Entry<String, String> entry : ongoingRequests.entrySet()) {
            accumulatedData.put(timestamp + ", " + entry.getKey() + ", CPU Load: " + String.format("%.2f", cpuLoad) + "%", entry.getValue());
        }
        System.out.println("Stored ongoing requests in map at " + timestamp + " with CPU load: " + cpuLoad + "%");
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
            writer.append("Timestamp, Request ID, URI, Method, CPU Load\n");
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
}
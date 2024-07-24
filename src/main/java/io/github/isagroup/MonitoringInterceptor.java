package io.github.isagroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.isagroup.services.jwt.PricingJwtUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MonitoringInterceptor implements HandlerInterceptor {
    private final ConcurrentMap<String, String> ongoingRequests = new ConcurrentHashMap<>();
    private final Map<String, String> accumulatedData = new HashMap<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PricingContext pricingContext;

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

        ongoingRequests.remove(requestId);
    }
    
    @Scheduled(fixedRate = 5000) // runs every 5 seconds
    public void storeOngoingRequestsInMap() {
        String timestamp = DATE_FORMAT.format(new Date());
        for (Map.Entry<String, String> entry : ongoingRequests.entrySet()) {
            accumulatedData.put(timestamp + ", " + entry.getKey(), entry.getValue());
        }
        System.out.println("Stored ongoing requests in map at " + timestamp);
    }

    @Scheduled(fixedRate = 120000) // runs every 2 minutes
    public void exportAccumulatedDataToCsv() {
        try (FileWriter writer = new FileWriter("ongoing_requests.csv", true)) {
            writer.append("Timestamp, Request ID, URI, Method\n");
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

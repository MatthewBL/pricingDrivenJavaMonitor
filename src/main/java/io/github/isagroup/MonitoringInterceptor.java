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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MonitoringInterceptor implements HandlerInterceptor {
    private final ConcurrentMap<String, String> ongoingRequests = new ConcurrentHashMap<>();
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
    public void exportOngoingRequestsToCsv() {
        try (FileWriter writer = new FileWriter("ongoing_requests.csv")) {
            writer.append("Timestamp, Request ID, URI, Method\n");
            String timestamp = DATE_FORMAT.format(new Date());
            for (Map.Entry<String, String> entry : ongoingRequests.entrySet()) {
                writer.append(timestamp)
                      .append(", ")
                      .append(entry.getKey())
                      .append(", ")
                      .append(entry.getValue())
                      .append("\n");
            }
            System.out.println("Exported ongoing requests to CSV at " + timestamp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

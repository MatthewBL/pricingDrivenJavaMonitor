package io.github.isagroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.github.isagroup.PricingContext;

@Component
public class MonitoringInterceptor implements HandlerInterceptor {
    private final ConcurrentMap<String, String> ongoingRequests = new ConcurrentHashMap<>();

    @Autowired
    private PricingContext pricingContext;

    public MonitoringInterceptor(PricingContext pricingContext) {
        this.pricingContext = pricingContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestId = UUID.randomUUID().toString();

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String pricingPlan = pricingContext.getUserPlan();

        ongoingRequests.put(requestId, uri + ", " + method + ", " + pricingPlan);

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
}

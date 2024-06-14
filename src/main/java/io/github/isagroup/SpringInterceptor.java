import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.web.servlet.HandlerInterceptor;
import io.opentelemetry.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class SpringInterceptor implements HandlerInterceptor {

    private final TextMapPropagator textFormat;
    private final Tracer tracer;

    public SpringInterceptor() {
        textFormat = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
        tracer = GlobalOpenTelemetry.getTracer("TracingInterceptor");
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        TextMapGetter<Map<String, String>> getter = new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }

            @Override
            public String get(Map<String, String> carrier, String key) {
                return carrier.get(key);
            }
        };

        Context extractedContext = textFormat.extract(Context.current(), headers, getter);
        SpanContext parentContext = Span.fromContext(extractedContext).getSpanContext();

        SpanBuilder spanBuilder = tracer.spanBuilder("spanName").setParent(extractedContext).setSpanKind(SpanKind.SERVER);
        Span span = spanBuilder.startSpan();

        // Store the span in the request so it can be accessed in the controller method
        request.setAttribute("span", span);

        return true;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Span span = (Span) request.getAttribute("span");
        if (ex != null) {
            span.setStatus(StatusCode.ERROR, "Error handling request");
        }
        span.end();
    }
}
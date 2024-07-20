import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.github.isagroup.MonitoringInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MonitoringInterceptor monitoringInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(monitoringInterceptor);
    }
}

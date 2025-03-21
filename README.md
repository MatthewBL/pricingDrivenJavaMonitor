# Instructions

1.- Installing the package

mvn install:install-file -Dfile="C:\path\to\pricingDrivenJavaMonitor\target\PricingDrivenJavaMonitor.jar" -DgroupId="io.github.isagroup" -DartifactId="monitoring-interceptor" -Dversion="1.0" -Dpackaging=jar


2.- Adding the dependency

    <dependency>
      <groupId>io.github.isagroup</groupId>
      <artifactId>monitoring-interceptor</artifactId>
      <version>1.0</version>
    </dependency>


3.- Enabling Component Scanning and Scheduling, and call the warmUpCpuLoad method

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"io.github.isagroup", "org.springframework.samples.petclinic"})
@EnableScheduling
public class PetclinicApplication {
	public static void main(String[] args) {
		SpringApplication.run(PetclinicApplication.class, args);
		MonitoringInterceptor.warmUpCpuLoad();
	}
}


4.- Configure Properties

monitoring.individualMonitoring.enabled=true
monitoring.delay.afterCompletion=500
monitoring.fixedRate.store=5000
monitoring.fixedRate.export=30000
monitoring.CPU.update=100
monitoring.enabled=true


5.- Use interceptor

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MonitoringInterceptor monitoringInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(monitoringInterceptor);
    }
}

package io.sourceripple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SourceRippleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SourceRippleApplication.class, args);
    }
}

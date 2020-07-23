package apache.rs_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;


@SpringBootApplication
@EnableAutoConfiguration(exclude = { FreeMarkerAutoConfiguration.class })
public class SampleRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleRestApplication.class, args);
    }
}

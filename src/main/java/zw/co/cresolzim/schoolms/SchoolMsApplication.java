package zw.co.cresolzim.schoolms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchoolMsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchoolMsApplication.class, args);
    }
}

package backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // Image re-encoding uses java.awt; servers have no display.
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(BackendApplication.class, args);
    }
}

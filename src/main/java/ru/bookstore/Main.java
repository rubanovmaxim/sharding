package ru.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.bookstore.config.YamlConfig;


@RestController
@SpringBootApplication
@EnableConfigurationProperties(YamlConfig.class)
public class Main {

    @GetMapping("/")
    public String home() {
        return "Sharding service";
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

}

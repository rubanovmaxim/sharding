package ru.bookstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties
public class YamlConfig {
    private List<String> shards = new ArrayList<>();

    public List<String> getShards() {
        return shards;
    }
}

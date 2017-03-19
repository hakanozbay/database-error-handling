package com.hakanozbay.database.error.handling.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ComponentScan("com.hakanozbay.database")
@PropertySource("classpath:application.properties")
public class TestConfig {

}

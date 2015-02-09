package org.edng.wellframe.spamfilter.configuration;

import org.edng.wellframe.spamfilter.tool.FileHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * Created by ed on 12/4/14.
 */
@Configuration
@ComponentScan(basePackages = {"org.edng.wellframe.spamfilter.tool"})
@PropertySource("classpath:spamfilter.properties")
public class ApplicationConfiguration {
    @Resource
    private Environment environment;

    @Bean
    public FileHandler fileHandler() {
        return new FileHandler(
                environment.getProperty("datadir.messages"),
                environment.getProperty("datadir.stats"));
    }

}
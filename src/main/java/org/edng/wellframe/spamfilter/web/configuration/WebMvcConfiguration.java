package org.edng.wellframe.spamfilter.web.configuration;

import org.edng.wellframe.spamfilter.tool.FileHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import javax.annotation.Resource;

/**
 * @author ed
 */
@EnableWebMvc
@Configuration
@ComponentScan(basePackages = {"org.edng.wellframe.spamfilter.web.controller", "org.edng.wellframe.spamfilter.tool"})
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    @Resource
    private Environment environment;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("/resources/");
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setViewClass(JstlView.class);
        viewResolver.setPrefix("/WEB-INF/jsp/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    @Bean
    public FileHandler fileHandler() {
        return new FileHandler(
                environment.getProperty("datadir.messages"),
                environment.getProperty("datadir.stats"));
    }
}

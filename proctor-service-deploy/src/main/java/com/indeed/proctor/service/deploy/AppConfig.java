package com.indeed.proctor.service.deploy;

import com.indeed.proctor.service.core.config.CoreConfig;
import com.indeed.proctor.service.core.config.JsonServiceConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;
import java.io.IOException;

@Configuration
@EnableWebMvc
@EnableScheduling
@Import(CoreConfig.class)
@ComponentScan("com.indeed.proctor.service.core")
public class AppConfig extends WebMvcConfigurerAdapter {

    @Bean
    public JsonServiceConfig jsonServiceConfig(@Value("${proctor.service.config.path}") final String serviceConfigPath) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(serviceConfigPath), JsonServiceConfig.class);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        // this is *required* to get ${...} replacements to work
        return new PropertySourcesPlaceholderConfigurer();
    }
}

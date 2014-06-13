package com.indeed.proctor.service.deploy;

import com.indeed.proctor.service.core.config.CoreConfig;
import com.indeed.proctor.service.core.config.JsonServiceConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
    public JsonServiceConfig jsonServiceConfig() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File("/var/lucene/proctor/service-config.json"), JsonServiceConfig.class);
    }
}

package com.indeed.proctor.service.deploy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.service.core.config.CoreConfig;
import com.indeed.proctor.service.core.config.JsonServiceConfig;
import com.indeed.proctor.service.core.config.VariableConfigurationJsonParser;
import com.indeed.proctor.service.core.var.VariableConfiguration;
import com.indeed.proctor.service.deploy.useragent.UserAgentValueConverter;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @Bean
    @Autowired
    public VariableConfiguration variableConfiguration(final JsonServiceConfig jsonServiceConfig) {
        return VariableConfigurationJsonParser.newParser()
            .registerStandardConverters()
            // Custom types for UserAgent
            .registerValueConverterByCanonicalName(UserAgentValueConverter.userAgentValueConverter())
            .registerValueConverterBySimpleName(UserAgentValueConverter.userAgentValueConverter())
            .buildFrom(jsonServiceConfig);
    }

    @Bean(destroyMethod = "shutdownNow")
    @Autowired
    public ScheduledExecutorService scheduledExecutorService(
            final AbstractProctorLoader loader,
            @Value("${proctor.service.reload.seconds}") final int proctorReloadSeconds) {

        final ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        final ScheduledExecutorService executorService =
                Executors.newScheduledThreadPool(4, threadFactoryBuilder.build());

        executorService.scheduleWithFixedDelay(loader, 0, proctorReloadSeconds, TimeUnit.SECONDS);

        return executorService;
    }
}

package com.indeed.proctor.pipet.deploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.pipet.core.config.CoreConfig;
import com.indeed.proctor.pipet.core.config.JsonPipetConfig;
import com.indeed.proctor.pipet.core.config.VariableConfigurationJsonParser;
import com.indeed.proctor.pipet.core.var.VariableConfiguration;
import com.indeed.proctor.pipet.deploy.useragent.UserAgentValueConverter;
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
@ComponentScan("com.indeed.proctor.pipet.core")
public class AppConfig extends WebMvcConfigurerAdapter {

    @Bean
    public JsonPipetConfig jsonPipetConfig(@Value("${proctor.pipet.config.path}") final String pipetConfigPath) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(pipetConfigPath), JsonPipetConfig.class);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        // this is *required* to get ${...} replacements to work
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @Autowired
    public VariableConfiguration variableConfiguration(final JsonPipetConfig jsonPipetConfig) {
        return VariableConfigurationJsonParser.newParser()
            .registerStandardConverters()
            // Custom types for UserAgent
            .registerValueConverterByCanonicalName(UserAgentValueConverter.userAgentValueConverter())
            .registerValueConverterBySimpleName(UserAgentValueConverter.userAgentValueConverter())
            .buildFrom(jsonPipetConfig);
    }

    @Bean(destroyMethod = "shutdownNow")
    @Autowired
    public ScheduledExecutorService scheduledExecutorService(
            final AbstractProctorLoader loader,
            @Value("${proctor.pipet.reload.seconds}") final int proctorReloadSeconds) {

        final ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder();
        final ScheduledExecutorService executorService =
                Executors.newScheduledThreadPool(4, threadFactoryBuilder.build());

        executorService.scheduleWithFixedDelay(loader, 0, proctorReloadSeconds, TimeUnit.SECONDS);

        return executorService;
    }
}

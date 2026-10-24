package com.seaoftales;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
@Profile("test")
public class TestMailConfiguration {
    @Bean
    JavaMailSender testJavaMailSender() {
        return Mockito.mock(JavaMailSender.class);
    }
}

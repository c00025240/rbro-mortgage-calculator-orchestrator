package ro.raiffeisen.internet.mortgage_calculator.config.rest;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplateBuilder restTemplateBuilder =  new RestTemplateBuilder();
        return restTemplateBuilder.errorHandler(new RestTemplateResponseErrorHandler()).build();
    }
}

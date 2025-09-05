package ro.raiffeisen.internet.mortgage_calculator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BaseWebMvcConfigurationSupport implements WebMvcConfigurer {

  LoggerRequestInterceptorAdapter loggerRequestInterceptorAdapter;

  @Autowired
  public BaseWebMvcConfigurationSupport(
      LoggerRequestInterceptorAdapter loggerRequestInterceptorAdapter) {
    this.loggerRequestInterceptorAdapter = loggerRequestInterceptorAdapter;
  }

  @Override
  public void addInterceptors(final InterceptorRegistry registry) {
    String[] pathPatterns = {
      "/swagger-ui/**",
      "/error",
      "/management/**",
      "/v3/api-docs/**",
      "/swagger-resources/**",
      "/webjars/**",
      "/favicon.ico",
      "/csrf"
    };
    registry.addInterceptor(loggerRequestInterceptorAdapter).excludePathPatterns(pathPatterns);
  }
}

package com.ecom.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Add the module to a service's dependencies and the service gets {@code /health} and has {@code /api} in front of every
 * REST endpoint, without touching a controller. Both are set under {@code ecom.web.*}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(WebProperties.class)
public class WebAutoConfiguration {

   /** First in line, ahead of the security filter, so the probe never needs a token. */
   @Bean
   public FilterRegistrationBean<HealthFilter> healthFilter(WebProperties properties) {
      FilterRegistrationBean<HealthFilter> registration = new FilterRegistrationBean<>(new HealthFilter(properties.healthPath()));
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
      return registration;
   }

   @Bean
   public WebMvcConfigurer apiPrefixConfigurer(WebProperties properties) {
      return new WebMvcConfigurer() {
         @Override
         public void configurePathMatch(PathMatchConfigurer configurer) {
            if (StringUtils.hasText(properties.apiPrefix())) {
               configurer.addPathPrefix(properties.apiPrefix(), HandlerTypePredicate.forAnnotation(RestController.class));
            }
         }
      };
   }
}

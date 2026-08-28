package com.ecom.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityAutoConfiguration {

   /** Only registered when the application provides a {@link PrincipalResolver}; otherwise every request is anonymous. */
   @Bean
   @ConditionalOnBean(PrincipalResolver.class)
   public FilterRegistrationBean<PrincipalFilter> principalFilter(PrincipalResolver resolver) {
      FilterRegistrationBean<PrincipalFilter> registration = new FilterRegistrationBean<>(new PrincipalFilter(resolver));
      registration.setOrder(Integer.MIN_VALUE + 10);
      return registration;
   }

   @Bean
   public SecurityInterceptor securityInterceptor() {
      return new SecurityInterceptor();
   }

   @Bean
   public WebMvcConfigurer securityWebMvcConfigurer(SecurityInterceptor interceptor) {
      return new WebMvcConfigurer() {
         @Override
         public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(interceptor).addPathPatterns("/**");
         }
      };
   }
}

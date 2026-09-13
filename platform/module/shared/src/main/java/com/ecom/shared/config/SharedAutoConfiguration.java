package com.ecom.shared.config;

import com.ecom.shared.validator.EntityVersionValidator;
import com.ecom.shared.web.SharedExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
public class SharedAutoConfiguration {

   @Bean
   @ConditionalOnMissingBean
   public EntityVersionValidator entityVersionValidator(){
      return new EntityVersionValidator();
   }

   /**
    * Kept in a nested class so non-web consumers (which lack spring-webmvc) can still load this auto-configuration.
    */
   @Configuration(proxyBeanMethods = false)
   @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
   @ConditionalOnClass(name = "org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler")
   static class WebConfiguration {

      @Bean
      @ConditionalOnMissingBean
      SharedExceptionHandler sharedExceptionHandler() {
         return new SharedExceptionHandler();
      }
   }
}

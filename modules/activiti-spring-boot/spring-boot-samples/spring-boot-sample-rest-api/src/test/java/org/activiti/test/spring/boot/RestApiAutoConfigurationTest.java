package org.activiti.test.spring.boot;

import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer;
import org.activiti.spring.boot.DataSourceProcessEngineAutoConfiguration;
import org.activiti.spring.boot.RestApiAutoConfiguration;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Josh Long
 */
public class RestApiAutoConfigurationTest {


    @Configuration
    @Import({EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
            MultipartAutoConfiguration.class,
            ServerProperties.class,
            DataSourceAutoConfiguration.class,
            DataSourceProcessEngineAutoConfiguration.DataSourceProcessEngineConfiguration.class,
            SecurityAutoConfiguration.class,
            RestApiAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    protected static class BaseConfiguration {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
        @Bean
        ServletWebServerFactory servletWebServerFactory(){
            return new TomcatServletWebServerFactory();
        }

        @Bean
        public ServerProperties serverProperties() {
            ServerProperties properties = new ServerProperties();
            properties.setPort(0);
            return properties;
        }
    }

    /*   @Configuration
       @Import({EmbeddedServletContainerAutoConfiguration.class,
               DispatcherServletAutoConfiguration.class,
               ServerPropertiesAutoConfiguration.class,
               HttpMessageConvertersAutoConfiguration.class,
               WebMvcAutoConfiguration.class,
               DataSourceAutoConfiguration.class,
               DataSourceProcessEngineAutoConfiguration.DataSourceConfiguration.class,
               RestApiAutoConfiguration.class
       })
       public static class RestApiConfiguration {

           @Bean
           public RestTemplate restTemplate() {
               return new RestTemplate();
           }
       }
   */
    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    private AnnotationConfigServletWebServerApplicationContext context;

    @Test
    public void testRestApiIntegration() throws Throwable {

        this.context = new AnnotationConfigServletWebServerApplicationContext();
        this.context.register(BaseConfiguration.class);
        this.context.refresh();

        RestTemplate restTemplate = this.context.getBean(RestTemplate.class);

        String authenticationChallenge = "http://localhost:" + this.context.getWebServer().getPort() +
                "/repository/process-definitions";

        final AtomicBoolean received401 = new AtomicBoolean();
        received401.set(false);
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                return true;
            }

            @Override
            public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
                if (clientHttpResponse.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
                    received401.set(true);
            }
        });

        ResponseEntity<String> response = restTemplate.getForEntity(authenticationChallenge, String.class);
        org.junit.Assert.assertTrue(received401.get());
    }
}

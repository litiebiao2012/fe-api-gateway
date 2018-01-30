package fe.api.gateway;

import com.google.common.collect.Sets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.util.Set;

/**
 * Created by fe on 2018/1/26.
 */
@SpringBootApplication
@ServletComponentScan("fe.core")
public class ApiGateway {
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(ApiGateway.class);
        app.setWebEnvironment(true);
        Set<Object> set = Sets.newHashSetWithExpectedSize(1);
        set.add("classpath*:spring/spring-*.xml");
        app.setSources(set);
        app.run(args);
    }

    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ApiGateway.class);
    }
}

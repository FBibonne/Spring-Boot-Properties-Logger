package spring.issue;

import fr.insee.boot.PropertiesLogger;
import fr.insee.test.Slf4jStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IssueWithDynamicPropertySourceTest.class ,properties = {
        "spring.config.import=",
        //"spring.config.additional-location=classpath:spring/issue/application.properties"
}
)
@Configuration
class IssueWithDynamicPropertySourceTest {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.config.additional-location", () -> "classpath:/spring/issue/");
    }

    @Test
    @Disabled
    void propertyFromAddtionalLocationShouldBeLoaded(@Autowired Environment environment) {
        assertThat(environment.getProperty("spring.config.additional-location")).hasToString("classpath:/spring/issue/");
        assertThat(environment.getProperty("property.in.addtional.file")).hasToString("ok");
    }

    @AfterAll
    static void clearLogStub(){
        ((Slf4jStub) LoggerFactory.getLogger(PropertiesLogger.class)).getStringBuilder().setLength(0);
    }

}

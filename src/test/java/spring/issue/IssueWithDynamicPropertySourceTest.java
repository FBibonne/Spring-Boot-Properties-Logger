package spring.issue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = IssueWithDynamicPropertySourceTest.class ,properties = {
        //"spring.config.additional-location=classpath:spring/issue/additionalPropsInClasspath.properties"
}
)
@Configuration
class IssueWithDynamicPropertySourceTest {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.config.additional-location", () -> "classpath:spring/issue/additionalPropsInClasspath.properties");
    }

    @Test
    void propertyFromAddtionalLocationShouldBeLoaded(@Autowired Environment environment) {
        assertThat(environment.getProperty("spring.config.additional-location")).hasToString("classpath:spring/issue/additionalPropsInClasspath.properties");
        assertThat(environment.getProperty("property.in.addtional.file")).hasToString("ok");
    }

}

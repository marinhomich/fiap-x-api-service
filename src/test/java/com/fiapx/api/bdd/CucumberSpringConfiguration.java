package com.fiapx.api.bdd;

import com.fiapx.api.FiapXApiApplication;
import com.fiapx.api.config.TestRabbitConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@ActiveProfiles("test")
@Import(TestRabbitConfig.class)
@SpringBootTest(classes = FiapXApiApplication.class)
public class CucumberSpringConfiguration {
}

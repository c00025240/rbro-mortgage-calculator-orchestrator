package ro.raiffeisen.internet.mortgage_calculator.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info =
        @Info(
            title = "rice-loan-calculator-orchestrator API",
            description = "**API Operations performed for loan calculator**\n\n",
            contact =
                @Contact(
                    name = "Internet team",
                    url = "https://confluence.rbro.rbg.cc/display/WWW/www+RBRO",
                    email = "internet.redesign@raiffeisen.ro"),
            version = "1.0.0"),
    servers = @Server(url = "http://localhost:8080"))
@Configuration
public class OpenApiConfig {}

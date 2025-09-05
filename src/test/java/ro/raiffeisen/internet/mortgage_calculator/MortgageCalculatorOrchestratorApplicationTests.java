package ro.raiffeisen.internet.mortgage_calculator;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {MortgageCalculatorOrchestratorApplication.class})
@ExtendWith(MockitoExtension.class)
class MortgageCalculatorOrchestratorApplicationTests {

	public static void main(String[] args) {
		new SpringApplicationBuilder(MortgageCalculatorOrchestratorApplication.class).run(args);
	}

}

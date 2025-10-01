package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.exception.BadRequestException;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MortgageCalculatorFactoryTest {

    @Mock
    private ServiceUtil serviceUtil;

    private MortgageCalculatorFactory factory;
    private List<MortgageCalculator> calculators;

    @BeforeEach
    void setUp() {
        // Create all calculator instances
        calculators = Arrays.asList(
                new CasaTaCalculator(serviceUtil),
                new ConstructieCalculator(serviceUtil),
                new CreditVenitCalculator(serviceUtil),
                new FlexiIntegralCalculator(serviceUtil)
        );
        
        factory = new MortgageCalculatorFactory(calculators);
    }

    @Test
    void getCalculator_withCasaTa_returnsCasaTaCalculator() {
        // When
        MortgageCalculator calculator = factory.getCalculator("CasaTa");

        // Then
        assertThat(calculator).isInstanceOf(CasaTaCalculator.class);
        assertThat(calculator.supports("CasaTa")).isTrue();
    }

    @Test
    void getCalculator_withConstructie_returnsConstructieCalculator() {
        // When
        MortgageCalculator calculator = factory.getCalculator("Constructie");

        // Then
        assertThat(calculator).isInstanceOf(ConstructieCalculator.class);
        assertThat(calculator.supports("Constructie")).isTrue();
    }

    @Test
    void getCalculator_withCreditVenit_returnsCreditVenitCalculator() {
        // When
        MortgageCalculator calculator = factory.getCalculator("CreditVenit");

        // Then
        assertThat(calculator).isInstanceOf(CreditVenitCalculator.class);
        assertThat(calculator.supports("CreditVenit")).isTrue();
    }

    @Test
    void getCalculator_withFlexiIntegral_returnsFlexiIntegralCalculator() {
        // When
        MortgageCalculator calculator = factory.getCalculator("FlexiIntegral");

        // Then
        assertThat(calculator).isInstanceOf(FlexiIntegralCalculator.class);
        assertThat(calculator.supports("FlexiIntegral")).isTrue();
    }

    @Test
    void getCalculator_withUnknownProductCode_throwsBadRequestException() {
        // When/Then
        assertThatThrownBy(() -> factory.getCalculator("UnknownProduct"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported product code: UnknownProduct");
    }

    @Test
    void getCalculator_withNullProductCode_throwsBadRequestException() {
        // When/Then
        assertThatThrownBy(() -> factory.getCalculator(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported product code: null");
    }

    @Test
    void getCalculator_withEmptyProductCode_throwsBadRequestException() {
        // When/Then
        assertThatThrownBy(() -> factory.getCalculator(""))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported product code: ");
    }

    @Test
    void getCalculator_callsSupportsOnEachCalculator() {
        // Given
        String productCode = "CasaTa";

        // When
        MortgageCalculator result = factory.getCalculator(productCode);

        // Then - should find the correct calculator
        assertThat(result).isNotNull();
        assertThat(result.supports(productCode)).isTrue();
    }

    @Test
    void getCalculator_withCaseSensitiveProductCode_returnCorrectCalculator() {
        // CasaTa is case-sensitive
        MortgageCalculator calculator = factory.getCalculator("CasaTa");
        assertThat(calculator).isInstanceOf(CasaTaCalculator.class);

        // casata (lowercase) should not match
        assertThatThrownBy(() -> factory.getCalculator("casata"))
                .isInstanceOf(BadRequestException.class);
    }
}



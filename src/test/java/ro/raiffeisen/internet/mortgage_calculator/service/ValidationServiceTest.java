package ro.raiffeisen.internet.mortgage_calculator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.exception.BadRequestException;
import ro.raiffeisen.internet.mortgage_calculator.model.*;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ValidationServiceTest {

    @InjectMocks
    private ValidationService validationService;

    @Mock
    private MortgageCalculationRequest request;

    @BeforeEach
    void setUp() {
        request = mock(MortgageCalculationRequest.class);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidRequests")
    void testValidateRequestInvalidInput(MortgageCalculationRequest invalidRequest, String expectedMessage) {
        BadRequestException thrown = assertThrows(BadRequestException.class, () -> validationService.validateRequest(invalidRequest));
        assertEquals(expectedMessage, thrown.getMessage());
    }

    private static Stream<Arguments> provideInvalidRequests() {
        return Stream.of(
                Arguments.of(invalidArgumentRequest(
                                "",
                                new Amount("RON", BigDecimal.TEN),
                                new Area("Aiud", "Alba"),
                                new Income(BigDecimal.TEN, BigDecimal.ZERO)),
                        "ProductCode should not be null or empty"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("RON", null),
                                new Area("Aiud", "Alba"),
                                new Income(BigDecimal.TEN, BigDecimal.ZERO)),
                        "Amount should not be null or empty"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("", BigDecimal.TEN),
                                new Area("Aiud", "Alba"),
                                new Income(BigDecimal.TEN, BigDecimal.ZERO)),
                        "Currency should not be null or empty"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("RON", BigDecimal.TEN),
                                null,
                                new Income(BigDecimal.TEN, BigDecimal.ZERO)),
                        "Area should not be null"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("RON", BigDecimal.TEN),
                                new Area("", "Alba"),
                                new Income(BigDecimal.TEN, BigDecimal.ZERO)),
                        "City should not be null or empty"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("RON", BigDecimal.TEN),
                                new Area("Aiud", ""),
                                new Income(BigDecimal.TEN, BigDecimal.ZERO)),
                        "County should not be null or empty"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("RON", BigDecimal.TEN),
                                new Area("Aiud", "Alba"),
                                new Income(null, BigDecimal.ZERO)),
                        "CurrentIncome should not be null"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("RON", BigDecimal.TEN),
                                new Area("Aiud", "Alba"),
                                new Income(BigDecimal.TEN, null)),
                        "OtherInstallments should not be null"),
                Arguments.of(invalidArgumentRequest(
                                "test",
                                new Amount("RON", BigDecimal.TEN),
                                new Area("Aiud", "Alba"),
                                null),
                        "Income should not be null"));
    }

    private static MortgageCalculationRequest invalidArgumentRequest(String productCode, Amount loanAmount, Area area, Income income) {
        return MortgageCalculationRequest.builder()
                .productCode(productCode)
                .installmentType(InstallmentType.EQUAL_INSTALLMENTS)
                .owner(false)
                .hasInsurance(false)
                .downPayment(BigDecimal.TEN)
                .specialOfferRequirements(new SpecialOfferRequirements(false, false))
                .interestRateType(MixedInterestRateType.builder()
                        .fixedPeriod(3)
                        .build())
                .age(43)
                .income(income)
                .loanAmount(loanAmount)
                .area(area)
                .build();
    }

}

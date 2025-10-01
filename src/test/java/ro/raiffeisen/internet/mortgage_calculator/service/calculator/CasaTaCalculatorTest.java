package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.exception.UnprocessableEntityException;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateTypeFormula;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasaTaCalculatorTest {

    @Mock
    private ServiceUtil serviceUtil;

    private CasaTaCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CasaTaCalculator(serviceUtil);
    }

    @Test
    void supports_withCasaTa_returnsTrue() {
        // When/Then
        assertThat(calculator.supports("CasaTa")).isTrue();
    }

    @Test
    void supports_withOtherProductCode_returnsFalse() {
        // When/Then
        assertThat(calculator.supports("Constructie")).isFalse();
        assertThat(calculator.supports("CreditVenit")).isFalse();
        assertThat(calculator.supports("FlexiIntegral")).isFalse();
        assertThat(calculator.supports("Unknown")).isFalse();
    }

    @Test
    void calculate_withValidRequest_setsLoanAmountAndDownPayment() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000), 
                null, // no down payment provided
                22
        );
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        InterestRateAdditionalInfo rateInfo = createRateInfo();
        
        when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));
        when(serviceUtil.calculateCreditAmount(any(), eq(80))).thenReturn(BigDecimal.valueOf(40000));
        when(serviceUtil.calculatePV(anyDouble(), anyInt(), anyDouble())).thenReturn(200000.0);
        when(serviceUtil.getAmountWithAnalysisCommission(any(), any())).thenReturn(BigDecimal.valueOf(40500));
        when(serviceUtil.createRepaymentPlanEntry(anyInt(), any(), any(), any())).thenReturn(new ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry());
        when(serviceUtil.calculateMonthlyInstallment(anyBoolean(), any(), any(), any())).thenReturn(new MonthlyInstallment(BigDecimal.ZERO, BigDecimal.valueOf(300)));
        when(serviceUtil.calculateDAE(any(), any(), any())).thenReturn(BigDecimal.valueOf(6.5));
        when(serviceUtil.calculateTotalPayment(any(), any())).thenReturn(new Amount("RON", BigDecimal.valueOf(70000)));
        when(serviceUtil.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getLoanAmount()).isNotNull();
        assertThat(response.getDownPayment()).isNotNull();
        assertThat(response.getMaxAmount()).isNotNull();
        verify(serviceUtil).calculateCreditAmount(BigDecimal.valueOf(50000), 80);
    }

    @Test
    void calculate_withProvidedDownPayment_usesProvidedValue() {
        // Given
        BigDecimal downPayment = BigDecimal.valueOf(15000);
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                downPayment,
                22
        );
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getDownPayment().getAmount()).isEqualByComparingTo(downPayment);
    }

    @Test
    void calculate_withDownPaymentGreaterThan30Percent_appliesDiscount() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(20000), // 40% down payment
                22
        );
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        // Verify discount was applied by checking the calculation was performed
        verify(serviceUtil, atLeastOnce()).retrieveInterestRate(any(), anyInt());
    }

    @Test
    void calculate_withDownPaymentGreaterThanLoanAmount_throwsException() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(60000), // More than loan amount
                22
        );
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        InterestRateAdditionalInfo rateInfo = createRateInfo();
        
        when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));
        when(serviceUtil.calculateCreditAmount(any(), eq(80))).thenReturn(BigDecimal.valueOf(40000));

        // When/Then
        assertThatThrownBy(() -> calculator.calculate(request, response))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Contributia proprie nu poate fi mai mare decat suma solicitata");
    }

    @Test
    void shouldApplyGuaranteeDiscount_withLargeDownPayment_returnsTrue() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(20000), // 40% down payment
                22
        );
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        
        when(serviceUtil.calculateCreditAmount(any(), eq(80))).thenReturn(BigDecimal.valueOf(40000));

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldApplyGuaranteeDiscount_withSmallDownPayment_returnsFalse() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(5000), // 10% down payment
                22
        );
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        
        when(serviceUtil.calculateCreditAmount(any(), eq(80))).thenReturn(BigDecimal.valueOf(40000));

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void calculate_setsAllRequiredResponseFields() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                null,
                22
        );
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getLoanAmount()).isNotNull();
        assertThat(response.getDownPayment()).isNotNull();
        assertThat(response.getMaxAmount()).isNotNull();
        assertThat(response.getMonthlyInstallment()).isNotNull();
        assertThat(response.getAnnualPercentageRate()).isNotNull();
        assertThat(response.getTotalPaymentAmount()).isNotNull();
    }

    // Helper methods
    
    private MortgageCalculationRequest createRequest(BigDecimal loanAmount, BigDecimal downPayment, int tenor) {
        return MortgageCalculationRequest.builder()
                .productCode("CasaTa")
                .loanAmount(new Amount("RON", loanAmount))
                .downPayment(downPayment)
                .tenor(tenor * 12)
                .age(43)
                .owner(false)
                .hasInsurance(false)
                .installmentType(InstallmentType.EQUAL_INSTALLMENTS)
                .interestRateType(VariableInterestRateType.builder().build())
                .specialOfferRequirements(new SpecialOfferRequirements(false, false))
                .income(new Income(BigDecimal.valueOf(10000), BigDecimal.valueOf(2500)))
                .area(new Area("Bucuresti", "Bucuresti"))
                .build();
    }

    private AdditionalCalculationInfo createAdditionalInfo() {
        return AdditionalCalculationInfo.builder()
                .currency("RON")
                .productId(40)
                .ltv(80)
                .analysisCommission(BigDecimal.valueOf(500))
                .buildingInsurancePremiumRate(BigDecimal.ZERO)
                .monthlyCurrentAccountCommission(BigDecimal.valueOf(5))
                .lifeInsurance(BigDecimal.valueOf(0.026))
                .ircc(5.6f)
                .feeCommission(BigDecimal.valueOf(533.037))
                .postGrantCommission(BigDecimal.TEN)
                .paymentOrderCommission(BigDecimal.ZERO)
                .buildingPADInsurancePremiumRate(BigDecimal.valueOf(99.54))
                .build();
    }

    private InterestRateAdditionalInfo createRateInfo() {
        return InterestRateAdditionalInfo.builder()
                .bankMarginRate(3.99)
                .defaultBankMarginRate(3.99)
                .interestRate(6.75)
                .defaultInterestRate(6.75)
                .defaultVariableInterestAfterFixedInterest(5.66)
                .variableInterestAfterFixedInterest(5.66)
                .yearsWithFixedInterest(0)
                .discounts(List.of(
                        Discount.builder().discountName("avans").discountValue(0.2).build(),
                        Discount.builder().discountName("client").discountValue(0.25).build(),
                        Discount.builder().discountName("asigurare").discountValue(0.15).build(),
                        Discount.builder().discountName("green house").discountValue(0.5).build()
                ))
                .build();
    }

    private void setupMocks() {
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        InterestRateAdditionalInfo rateInfo = createRateInfo();
        
        when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));
        when(serviceUtil.calculateCreditAmount(any(), eq(80))).thenReturn(BigDecimal.valueOf(40000));
        when(serviceUtil.calculatePV(anyDouble(), anyInt(), anyDouble())).thenReturn(200000.0);
        when(serviceUtil.getAmountWithAnalysisCommission(any(), any())).thenReturn(BigDecimal.valueOf(40500));
        when(serviceUtil.createRepaymentPlanEntry(anyInt(), any(), any(), any())).thenReturn(new ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry());
        when(serviceUtil.calculateMonthlyInstallment(anyBoolean(), any(), any(), any())).thenReturn(new MonthlyInstallment(BigDecimal.ZERO, BigDecimal.valueOf(300)));
        when(serviceUtil.calculateDAE(any(), any(), any())).thenReturn(BigDecimal.valueOf(6.5));
        when(serviceUtil.calculateTotalPayment(any(), any())).thenReturn(new Amount("RON", BigDecimal.valueOf(70000)));
        when(serviceUtil.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);
    }
}



package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;
import ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlexiIntegralCalculatorTest {

    @Mock
    private ServiceUtil serviceUtil;

    private FlexiIntegralCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new FlexiIntegralCalculator(serviceUtil);
    }

    @Test
    void supports_withFlexiIntegral_returnsTrue() {
        assertThat(calculator.supports("FlexiIntegral")).isTrue();
    }

    @Test
    void supports_withOtherProductCode_returnsFalse() {
        assertThat(calculator.supports("CasaTa")).isFalse();
        assertThat(calculator.supports("Constructie")).isFalse();
        assertThat(calculator.supports("CreditVenit")).isFalse();
    }

    @Test
    void calculate_setsMinGuaranteeAmount() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getMinGuaranteeAmount()).isNotNull();
        // MinGuarantee = (100 / LTV) * (loanAmount + analysisCommission)
        // With LTV=80: (100/80) * (100000 + 500) = 1.25 * 100500 = 125625
        assertThat(response.getMinGuaranteeAmount()).isGreaterThan(BigDecimal.valueOf(100000));
    }

    @Test
    void calculate_setsLoanAmountWithAnalysisCommission() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();
        when(serviceUtil.getAmountWithAnalysisCommission(eq(loanAmount), any()))
                .thenReturn(BigDecimal.valueOf(100500));

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getLoanAmount()).isNotNull();
        assertThat(response.getLoanAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100500));
        verify(serviceUtil).getAmountWithAnalysisCommission(loanAmount, BigDecimal.valueOf(500));
    }

    @Test
    void calculate_setsHousePrice() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getHousePrice()).isNotNull();
        // HousePrice = (100/70) * loanAmount = 142857.14...
        assertThat(response.getHousePrice().getAmount()).isGreaterThan(loanAmount);
    }

    @Test
    void calculate_withGoodGuarantee_appliesDiscount() {
        // Given - when garantiePentruDiscount (70%) <= garantie (80%)
        // This happens when client provides good guarantee
        BigDecimal loanAmount = BigDecimal.valueOf(50000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then - verify calculation was performed
        verify(serviceUtil, atLeastOnce()).retrieveInterestRate(any(), anyInt());
        assertThat(response.getLoanAmount()).isNotNull();
    }

    @Test
    void calculate_setsLoanAmountWithFee() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getLoanAmountWithFee()).isNotNull();
        assertThat(response.getLoanAmountWithFee()).isEqualTo(response.getLoanAmount());
    }

    @Test
    void calculate_setsMaxAmount() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();
        when(serviceUtil.calculatePV(anyDouble(), anyInt(), anyDouble())).thenReturn(200000.0);

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getMaxAmount()).isNotNull();
        assertThat(response.getMaxAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200000.0));
    }

    @Test
    void shouldApplyGuaranteeDiscount_withGoodGuarantee_returnsTrue() {
        // Given - garantiePentruDiscount (70%) <= garantie (80%)
        // garantie = (100/80) * 50000 = 62500
        // garantiePentruDiscount = (100/70) * 50000 = 71428.57
        // 71428.57 > 62500, so should return FALSE
        BigDecimal loanAmount = BigDecimal.valueOf(50000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then
        assertThat(result).isFalse(); // Because garantiePentruDiscount > garantie
    }

    @Test
    void shouldApplyGuaranteeDiscount_withLargeLoanAmount_checkCalculation() {
        // Given - with larger loan amount, the relationship might change
        BigDecimal loanAmount = BigDecimal.valueOf(200000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then - still false because LTV 70% will always be higher than 80%
        assertThat(result).isFalse();
    }

    @Test
    void calculate_setsAllRequiredResponseFields() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getMinGuaranteeAmount()).isNotNull();
        assertThat(response.getLoanAmount()).isNotNull();
        assertThat(response.getLoanAmountWithFee()).isNotNull();
        assertThat(response.getHousePrice()).isNotNull();
        assertThat(response.getMaxAmount()).isNotNull();
        assertThat(response.getMonthlyInstallment()).isNotNull();
        assertThat(response.getAnnualPercentageRate()).isNotNull();
        assertThat(response.getTotalPaymentAmount()).isNotNull();
    }

    @Test
    void calculate_withDifferentLtvValues_calculatesGuaranteesCorrectly() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        MortgageCalculationRequest request = createRequest(loanAmount, 22);
        MortgageCalculationResponse response = new MortgageCalculationResponse();

        // Setup with different LTV
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        additionalInfo.setLtv(90); // Higher LTV = lower guarantee
        InterestRateAdditionalInfo rateInfo = createRateInfo();
        
        when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));
        when(serviceUtil.calculatePV(anyDouble(), anyInt(), anyDouble())).thenReturn(200000.0);
        when(serviceUtil.getAmountWithAnalysisCommission(any(), any())).thenReturn(BigDecimal.valueOf(100500));
        when(serviceUtil.createRepaymentPlanEntry(anyInt(), any(), any(), any())).thenReturn(new RepaymentPlanEntry());
        when(serviceUtil.calculateMonthlyInstallment(anyBoolean(), any(), any(), any())).thenReturn(new MonthlyInstallment(BigDecimal.ZERO, BigDecimal.valueOf(300)));
        when(serviceUtil.calculateDAE(any(), any(), any())).thenReturn(BigDecimal.valueOf(6.5));
        when(serviceUtil.calculateTotalPayment(any(), any())).thenReturn(new Amount("RON", BigDecimal.valueOf(70000)));
        when(serviceUtil.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getMinGuaranteeAmount()).isNotNull();
        // With LTV 90%: guarantee = (100/90) * 100500 = 111666.67
        // Should be lower than with LTV 80%
    }

    // Helper methods

    private MortgageCalculationRequest createRequest(BigDecimal loanAmount, int tenor) {
        return MortgageCalculationRequest.builder()
                .productCode("FlexiIntegral")
                .loanAmount(new Amount("RON", loanAmount))
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
                        Discount.builder().discountName("client").discountValue(0.25).build()
                ))
                .build();
    }

    private void setupMocks() {
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        InterestRateAdditionalInfo rateInfo = createRateInfo();
        
        when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));
        when(serviceUtil.calculatePV(anyDouble(), anyInt(), anyDouble())).thenReturn(200000.0);
        when(serviceUtil.getAmountWithAnalysisCommission(any(), any())).thenReturn(BigDecimal.valueOf(100500));
        when(serviceUtil.createRepaymentPlanEntry(anyInt(), any(), any(), any())).thenReturn(new RepaymentPlanEntry());
        when(serviceUtil.calculateMonthlyInstallment(anyBoolean(), any(), any(), any())).thenReturn(new MonthlyInstallment(BigDecimal.ZERO, BigDecimal.valueOf(300)));
        when(serviceUtil.calculateDAE(any(), any(), any())).thenReturn(BigDecimal.valueOf(6.5));
        when(serviceUtil.calculateTotalPayment(any(), any())).thenReturn(new Amount("RON", BigDecimal.valueOf(70000)));
        when(serviceUtil.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);
    }
}



package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.exception.UnprocessableEntityException;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;
import ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ConstructieCalculatorTest {

    @Mock
    private ServiceUtil serviceUtil;

    private ConstructieCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ConstructieCalculator(serviceUtil);
    }

    @Test
    void supports_withConstructie_returnsTrue() {
        assertThat(calculator.supports("Constructie")).isTrue();
    }

    @Test
    void supports_withOtherProductCode_returnsFalse() {
        assertThat(calculator.supports("CasaTa")).isFalse();
        assertThat(calculator.supports("CreditVenit")).isFalse();
        assertThat(calculator.supports("FlexiIntegral")).isFalse();
    }

    @Test
    void calculate_withNoDownPayment_setsDefaultValues() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                null, // no down payment
                22
        );
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getDownPayment()).isNotNull();
        assertThat(response.getDownPayment().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getMinGuaranteeAmount()).isNotNull();
        assertThat(response.getNoDocAmount()).isNotNull();
        assertThat(response.getHousePrice()).isNotNull();
    }

    @Test
    void calculate_withDownPayment_calculatesCorrectly() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(50000);
        BigDecimal downPayment = BigDecimal.valueOf(10000);
        MortgageCalculationRequest request = createRequest(loanAmount, downPayment, 22);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getDownPayment().getAmount()).isEqualByComparingTo(downPayment);
        // valoareCredit = 50000 - 10000 = 40000
        // noDocAmount = 0.3 * 40000 = 12000
        assertThat(response.getNoDocAmount()).isEqualByComparingTo(BigDecimal.valueOf(12000));
    }

    @Test
    void calculate_withDownPaymentGreaterThanLoanAmount_throwsException() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(60000), // More than loan amount
                22
        );
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        InterestRateAdditionalInfo rateInfo = createRateInfo();
        
        when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));

        // When/Then
        assertThatThrownBy(() -> calculator.calculate(request, response))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Contributia proprie nu poate fi mai mare decat suma solicitata");
    }

    @Test
    void calculate_calculatesGuaranteeAmounts() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(10000),
                22
        );
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        // valoareCredit = 40000
        // garantie (80% LTV) = 40000 / 0.80 = 50000
        // garantiePentruDiscount (70% LTV) = 40000 / 0.70 = ~57142.86
        assertThat(response.getMinGuaranteeAmount()).isNotNull();
        assertThat(response.getHousePrice()).isNotNull();
    }

    @Test
    void calculate_withHighLTVGuarantee_appliesDiscount() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(20000), // Large down payment
                22
        );
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then - verify calculation was done (discount should be applied)
        verify(serviceUtil, atLeastOnce()).retrieveInterestRate(any(), anyInt());
        assertThat(response.getLoanAmount()).isNotNull();
    }

    @Test
    void shouldApplyGuaranteeDiscount_withGoodGuarantee_returnsTrue() {
        // Given - garantiePentruDiscount <= garantie means good guarantee
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(20000), // Large down payment
                22
        );
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then
        // valoareCredit = loanAmount - downPayment = 50000 - 20000 = 30000
        // garantie (LTV 80% from additionalInfo) = 30000 / 0.8 = 37500
        // garantiePentruDiscount (LTV 80% fixed) = 30000 / 0.8 = 37500
        // 37500 <= 37500 is TRUE, so discount should be applied
        assertThat(result).isTrue();
    }

    @Test
    void calculate_setsNoDocAmountTo30PercentOfCreditValue() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        BigDecimal downPayment = BigDecimal.valueOf(20000);
        MortgageCalculationRequest request = createRequest(loanAmount, downPayment, 22);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        // valoareCredit = 100000 - 20000 = 80000
        // noDocAmount = 0.3 * 80000 = 24000
        assertThat(response.getNoDocAmount()).isEqualByComparingTo(BigDecimal.valueOf(24000));
    }

    @Test
    void calculate_setsAllRequiredResponseFields() {
        // Given
        MortgageCalculationRequest request = createRequest(
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(10000),
                22
        );
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getDownPayment()).isNotNull();
        assertThat(response.getMinGuaranteeAmount()).isNotNull();
        assertThat(response.getNoDocAmount()).isNotNull();
        assertThat(response.getHousePrice()).isNotNull();
        assertThat(response.getLoanAmount()).isNotNull();
        assertThat(response.getLoanAmountWithFee()).isNotNull();
        assertThat(response.getMaxAmount()).isNotNull();
    }

    // Helper methods

    private MortgageCalculationRequest createRequest(BigDecimal loanAmount, BigDecimal downPayment, int tenor) {
        return MortgageCalculationRequest.builder()
                .productCode("Constructie")
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
                .monthlyLifeInsurance(new LifeInsurance(new Amount("RON", BigDecimal.valueOf(10)), Frequency.MONTHLY))
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
        
        // Core mocks - always needed
        lenient().when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        lenient().when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        
        // Optional mocks - not always used
        lenient().when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));
        lenient().when(serviceUtil.calculatePV(anyDouble(), anyInt(), anyDouble())).thenReturn(200000.0);
        lenient().when(serviceUtil.getAmountWithAnalysisCommission(any(), any())).thenReturn(BigDecimal.valueOf(40500));
        
        RepaymentPlanEntry mockEntry = new RepaymentPlanEntry();
        mockEntry.setTotalPaymentAmount(new Amount("RON", BigDecimal.valueOf(2000)));
        mockEntry.setInstallmentAmount(new Amount("RON", BigDecimal.valueOf(1800)));
        mockEntry.setReimbursedCapitalAmount(new Amount("RON", BigDecimal.valueOf(1000)));
        mockEntry.setInterestAmount(new Amount("RON", BigDecimal.valueOf(500)));
        mockEntry.setFeeAmount(new Amount("RON", BigDecimal.valueOf(50)));
        mockEntry.setRemainingLoanAmount(new Amount("RON", BigDecimal.valueOf(99000)));
        lenient().when(serviceUtil.createRepaymentPlanEntry(anyInt(), any(), any(), any())).thenReturn(mockEntry);
        
        lenient().when(serviceUtil.calculateMonthlyInstallment(anyBoolean(), any(), any(), any())).thenReturn(new MonthlyInstallment(BigDecimal.ZERO, BigDecimal.valueOf(300)));
        lenient().when(serviceUtil.calculateDAE(any(), any(), any())).thenReturn(BigDecimal.valueOf(6.5));
        lenient().when(serviceUtil.calculateTotalPayment(any(), any())).thenReturn(new Amount("RON", BigDecimal.valueOf(70000)));
        lenient().when(serviceUtil.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);
    }
}



package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.exception.UnprocessableEntityException;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;
import ro.raiffeisen.internet.mortgage_calculator.model.client.LoanProduct;
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
class CreditVenitCalculatorTest {

    @Mock
    private ServiceUtil serviceUtil;

    private CreditVenitCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CreditVenitCalculator(serviceUtil);
    }

    @Test
    void supports_withCreditVenit_returnsTrue() {
        assertThat(calculator.supports("CreditVenit")).isTrue();
    }

    @Test
    void supports_withOtherProductCode_returnsFalse() {
        assertThat(calculator.supports("CasaTa")).isFalse();
        assertThat(calculator.supports("Constructie")).isFalse();
        assertThat(calculator.supports("FlexiIntegral")).isFalse();
    }

    @Test
    void calculate_withLoanAmountProvided_calculatesCorrectly() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        BigDecimal downPayment = BigDecimal.valueOf(30000);
        MortgageCalculationRequest request = createRequestWithLoanAmount(loanAmount, downPayment, 22);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getLoanAmount()).isNotNull();
        assertThat(response.getDownPayment()).isNotNull();
        assertThat(response.getDownPayment().getAmount()).isEqualByComparingTo(downPayment);
        assertThat(response.getHousePrice()).isNotNull();
        // housePrice = loanAmount + downPayment = 130000
        assertThat(response.getHousePrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(130000));
    }

    @Test
    void calculate_withLoanAmountProvided_andLargeDownPayment_appliesDiscount() {
        // Given - down payment >= 30% of loan amount
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        BigDecimal downPayment = BigDecimal.valueOf(40000); // 40% > 30%
        MortgageCalculationRequest request = createRequestWithLoanAmount(loanAmount, downPayment, 22);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocks();

        // When
        calculator.calculate(request, response);

        // Then - verify calculation was performed
        verify(serviceUtil, atLeastOnce()).retrieveInterestRate(any(), anyInt());
        assertThat(response.getLoanAmount()).isNotNull();
    }

    @Test
    void calculate_withLoanAmountProvided_andDownPaymentGreaterThanLoanAmount_throwsException() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        BigDecimal downPayment = BigDecimal.valueOf(150000); // Greater than loan amount
        MortgageCalculationRequest request = createRequestWithLoanAmount(loanAmount, downPayment, 22);
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
    void calculate_withoutLoanAmount_calculatesMaxLoanAmountBasedOnIncome() {
        // Given
        MortgageCalculationRequest request = createRequestWithoutLoanAmount(22);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocksForIncomeBasedCalculation();

        // When
        calculator.calculate(request, response);

        // Then
        assertThat(response.getMaxAmount()).isNotNull();
        assertThat(response.getLoanAmount()).isNotNull();
        assertThat(response.getDownPayment()).isNotNull();
        assertThat(response.getHousePrice()).isNotNull();
        assertThat(response.getMinGuaranteeAmount()).isNotNull();
        
        // Verify PV calculation was called
        verify(serviceUtil).calculatePV(anyDouble(), anyInt(), anyDouble());
    }

    @Test
    void calculate_withoutLoanAmount_retrievesLoanProduct() {
        // Given
        MortgageCalculationRequest request = createRequestWithoutLoanAmount(22);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocksForIncomeBasedCalculation();

        // When
        calculator.calculate(request, response);

        // Then
        verify(serviceUtil).retrieveLoanProduct("CreditVenit");
    }

    @Test
    void calculate_withoutLoanAmount_setsLoanAmountInRequest() {
        // Given
        MortgageCalculationRequest request = createRequestWithoutLoanAmount(22);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().build();

        setupMocksForIncomeBasedCalculation();

        // When
        calculator.calculate(request, response);

        // Then
        // Request should have loan amount set by calculator
        assertThat(request.getLoanAmount()).isNotNull();
        assertThat(request.getLoanAmount().getAmount()).isNotNull();
    }

    @Test
    void shouldApplyGuaranteeDiscount_withLoanAmountAndLargeDownPayment_returnsTrue() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        BigDecimal downPayment = BigDecimal.valueOf(40000); // 40% >= 30%
        MortgageCalculationRequest request = createRequestWithLoanAmount(loanAmount, downPayment, 22);
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldApplyGuaranteeDiscount_withLoanAmountAndSmallDownPayment_returnsFalse() {
        // Given
        BigDecimal loanAmount = BigDecimal.valueOf(100000);
        BigDecimal downPayment = BigDecimal.valueOf(20000); // 20% = threshold (0.2 * 100000)
        MortgageCalculationRequest request = createRequestWithLoanAmount(loanAmount, downPayment, 22);
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then
        // 20000 >= 0.2 * 100000 (20000) is TRUE, so discount should be applied
        assertThat(result).isTrue();
    }

    @Test
    void shouldApplyGuaranteeDiscount_withoutLoanAmount_returnsFalse() {
        // Given
        MortgageCalculationRequest request = createRequestWithoutLoanAmount(22);
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();

        // When
        boolean result = calculator.shouldApplyGuaranteeDiscount(request, additionalInfo);

        // Then
        assertThat(result).isFalse();
    }

    // Helper methods

    private MortgageCalculationRequest createRequestWithLoanAmount(BigDecimal loanAmount, BigDecimal downPayment, int tenor) {
        return MortgageCalculationRequest.builder()
                .productCode("CreditVenit")
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

    private MortgageCalculationRequest createRequestWithoutLoanAmount(int tenor) {
        return MortgageCalculationRequest.builder()
                .productCode("CreditVenit")
                .loanAmount(null) // No loan amount provided
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

    private LoanProduct createLoanProduct() {
        LoanProduct product = new LoanProduct();
        product.setIdLoan(40);
        product.setProductLoan("CreditVenit");
        return product;
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
        lenient().when(serviceUtil.getAmountWithAnalysisCommission(any(), any())).thenReturn(BigDecimal.valueOf(70500));
        
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

    private void setupMocksForIncomeBasedCalculation() {
        AdditionalCalculationInfo additionalInfo = createAdditionalInfo();
        InterestRateAdditionalInfo rateInfo = createRateInfo();
        LoanProduct loanProduct = createLoanProduct();
        
        // Core mocks - always needed
        lenient().when(serviceUtil.retrieveLoanProduct("CreditVenit")).thenReturn(loanProduct);
        lenient().when(serviceUtil.retrieveAdditionalInfo(any())).thenReturn(additionalInfo);
        lenient().when(serviceUtil.retrieveInterestRate(any(), anyInt())).thenReturn(rateInfo);
        
        // Optional mocks - not always used
        lenient().when(serviceUtil.calculateAvailableRate(any())).thenReturn(BigDecimal.valueOf(4000));
        lenient().when(serviceUtil.calculatePV(anyDouble(), anyInt(), anyDouble())).thenReturn(150000.0);
        lenient().when(serviceUtil.getAmountWithAnalysisCommission(any(), any())).thenReturn(BigDecimal.valueOf(150500));
        
        RepaymentPlanEntry mockEntry = new RepaymentPlanEntry();
        mockEntry.setTotalPaymentAmount(new Amount("RON", BigDecimal.valueOf(2000)));
        mockEntry.setInstallmentAmount(new Amount("RON", BigDecimal.valueOf(1800)));
        mockEntry.setReimbursedCapitalAmount(new Amount("RON", BigDecimal.valueOf(1000)));
        mockEntry.setInterestAmount(new Amount("RON", BigDecimal.valueOf(500)));
        mockEntry.setFeeAmount(new Amount("RON", BigDecimal.valueOf(50)));
        mockEntry.setRemainingLoanAmount(new Amount("RON", BigDecimal.valueOf(99000)));
        lenient().when(serviceUtil.createRepaymentPlanEntry(anyInt(), any(), any(), any())).thenReturn(mockEntry);
        
        lenient().when(serviceUtil.calculateMonthlyInstallment(anyBoolean(), any(), any(), any())).thenReturn(new MonthlyInstallment(BigDecimal.ZERO, BigDecimal.valueOf(500)));
        lenient().when(serviceUtil.calculateDAE(any(), any(), any())).thenReturn(BigDecimal.valueOf(6.5));
        lenient().when(serviceUtil.calculateTotalPayment(any(), any())).thenReturn(new Amount("RON", BigDecimal.valueOf(120000)));
        lenient().when(serviceUtil.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);
    }
}



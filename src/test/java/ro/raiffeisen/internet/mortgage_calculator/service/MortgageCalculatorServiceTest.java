package ro.raiffeisen.internet.mortgage_calculator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.exception.UnprocessableEntityException;
import ro.raiffeisen.internet.mortgage_calculator.helper.MortgageCalculatorMapper;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MortgageCalculatorServiceTest {

    @Mock
    private ServiceUtil initialCalculationService;
    @Mock
    private MortgageCalculatorMapper mapper;
    @Mock
    private ValidationService validationService;

    @InjectMocks
    private MortgageCalculatorService mortgageService;

    @Test
    public void createCalculationTest_forCasaTa_successfullyCase() {
        doNothing().when(validationService).validateRequest(any());
        doCallRealMethod().when(initialCalculationService).calculateMaxPeriod(anyInt(), anyInt());
        when(initialCalculationService.retrieveAdditionalInfo(any())).thenReturn(getAdditionalInfoResponse());
        when(initialCalculationService.retrieveInterestRate(any(MortgageCalculationRequest.class), anyInt())).thenReturn(getInterestRateAdditionalInfoResponse());
        doCallRealMethod().when(initialCalculationService).calculateAvailableRate(any());
        doCallRealMethod().when(initialCalculationService).calculateCreditAmount(any(), anyInt());
        doCallRealMethod().when(initialCalculationService).calculatePV(anyDouble(), anyInt(), anyDouble());
        doCallRealMethod().when(initialCalculationService).getAmountWithAnalysisCommission(any(), any());
        doCallRealMethod().when(initialCalculationService).createRepaymentPlanEntry(anyInt(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculatePrincipal(anyInt(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateBalance(anyInt(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateInterest(anyInt(), any(), any());
        when(initialCalculationService.calculateFeeAmount(anyInt(), any(), any())).thenReturn(BigDecimal.TEN);
        doCallRealMethod().when(initialCalculationService).calculateInstallmentAmount(anyInt(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateTotalPaymentAmount(any(), any(), any(), any(), anyInt());
        when(initialCalculationService.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);
        doCallRealMethod().when(initialCalculationService).calculateMonthlyInstallment(anyBoolean(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateDAE(any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateTotalPayment(any(), any());

        MortgageCalculationResponse expectedResponse = getMortgageCalculationResponse();

        assertThat(mortgageService.createCalculation(getRequest("CasaTa", BigDecimal.valueOf(50000), null)))
                .isEqualTo(expectedResponse);
    }
    @Test
    public void createCalculationTest_forConstructie_successfullyCase() {
        doNothing().when(validationService).validateRequest(any());
        doCallRealMethod().when(initialCalculationService).calculateMaxPeriod(anyInt(), anyInt());
        when(initialCalculationService.retrieveAdditionalInfo(any())).thenReturn(getAdditionalInfoResponse());
        when(initialCalculationService.retrieveInterestRate(any(MortgageCalculationRequest.class), anyInt())).thenReturn(getInterestRateAdditionalInfoResponse());
        doCallRealMethod().when(initialCalculationService).calculateAvailableRate(any());
        doCallRealMethod().when(initialCalculationService).calculatePV(anyDouble(), anyInt(), anyDouble());
        doCallRealMethod().when(initialCalculationService).getAmountWithAnalysisCommission(any(), any());
        doCallRealMethod().when(initialCalculationService).createRepaymentPlanEntry(anyInt(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculatePrincipal(anyInt(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateBalance(anyInt(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateInterest(anyInt(), any(), any());
        when(initialCalculationService.calculateFeeAmount(anyInt(), any(), any())).thenReturn(BigDecimal.TEN);
        doCallRealMethod().when(initialCalculationService).calculateInstallmentAmount(anyInt(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateTotalPaymentAmount(any(), any(), any(), any(), anyInt());
        when(initialCalculationService.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);
        doCallRealMethod().when(initialCalculationService).calculateMonthlyInstallment(anyBoolean(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateDAE(any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateTotalPayment(any(), any());

        MortgageCalculationResponse expectedResponse = getMortgageCalculationResponseForConstructie();

        assertThat(mortgageService.createCalculation(getRequest("Constructie", BigDecimal.valueOf(50000), null)))
                .isEqualTo(expectedResponse);
    }

    @Test
    public void createCalculationTest_forCasaTa_successfullyCase_Avans30() {
        doNothing().when(validationService).validateRequest(any());
        doCallRealMethod().when(initialCalculationService).calculateMaxPeriod(anyInt(), anyInt());
        when(initialCalculationService.retrieveAdditionalInfo(any())).thenReturn(getAdditionalInfoResponse());
        when(initialCalculationService.retrieveInterestRate(any(MortgageCalculationRequest.class), anyInt())).thenReturn(getInterestRateAdditionalInfoResponse());
        doCallRealMethod().when(initialCalculationService).calculateAvailableRate(any());
        doCallRealMethod().when(initialCalculationService).calculateCreditAmount(any(), anyInt());
        doCallRealMethod().when(initialCalculationService).calculatePV(anyDouble(), anyInt(), anyDouble());
        doCallRealMethod().when(initialCalculationService).getAmountWithAnalysisCommission(any(), any());
        doCallRealMethod().when(initialCalculationService).createRepaymentPlanEntry(anyInt(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculatePrincipal(anyInt(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateBalance(anyInt(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateInterest(anyInt(), any(), any());
        when(initialCalculationService.calculateFeeAmount(anyInt(), any(), any())).thenReturn(BigDecimal.TEN);
        doCallRealMethod().when(initialCalculationService).calculateInstallmentAmount(anyInt(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateTotalPaymentAmount(any(), any(), any(), any(), anyInt());
        when(initialCalculationService.calculateBuildingInsurancePremiumRate(any(), any(), any(), anyInt(), any())).thenReturn(BigDecimal.TEN);
        doCallRealMethod().when(initialCalculationService).calculateMonthlyInstallment(anyBoolean(), any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateDAE(any(), any(), any());
        doCallRealMethod().when(initialCalculationService).calculateTotalPayment(any(), any());

        MortgageCalculationResponse expectedResponse = getMortgageCalculationResponseWhenAvans30();

        assertThat(mortgageService.createCalculation(getRequest("CasaTa", BigDecimal.valueOf(50000), BigDecimal.valueOf(15000))))
                .isEqualTo(expectedResponse);
    }

    @Test
    public void createCalculationTest_forCasaTa_DownPaymentBiggerThanAmount() {
        doNothing().when(validationService).validateRequest(any());
        doCallRealMethod().when(initialCalculationService).calculateMaxPeriod(anyInt(), anyInt());
        when(initialCalculationService.retrieveAdditionalInfo(any())).thenReturn(getAdditionalInfoResponse());
        when(initialCalculationService.retrieveInterestRate(any(MortgageCalculationRequest.class), anyInt())).thenReturn(getInterestRateAdditionalInfoResponse());
        doCallRealMethod().when(initialCalculationService).calculateAvailableRate(any());
        doCallRealMethod().when(initialCalculationService).calculateCreditAmount(any(), anyInt());

        assertThatThrownBy(() -> mortgageService.createCalculation(getRequest("CasaTa", BigDecimal.valueOf(50000), BigDecimal.valueOf(150000))))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Ne pare rau! Contributia proprie nu poate fi mai mare decat suma solicitata");
    }

    @Test
    public void createCalculationTest_forCasaTa_LoanAmountBiggerThanAffordableAmount() {

        doNothing().when(validationService).validateRequest(any());
        doCallRealMethod().when(initialCalculationService).calculateMaxPeriod(anyInt(), anyInt());
        when(initialCalculationService.retrieveAdditionalInfo(any())).thenReturn(getAdditionalInfoResponse());
        when(initialCalculationService.retrieveInterestRate(any(MortgageCalculationRequest.class), anyInt())).thenReturn(getInterestRateAdditionalInfoResponse());
        doCallRealMethod().when(initialCalculationService).calculateAvailableRate(any());
        doCallRealMethod().when(initialCalculationService).calculateCreditAmount(any(), anyInt());
        doCallRealMethod().when(initialCalculationService).calculatePV(anyDouble(), anyInt(), anyDouble());

        assertThatThrownBy(() -> mortgageService.createCalculation(getRequest("CasaTa", BigDecimal.valueOf(5000000), null)))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Ne pare rau! ☹️\n" +
                        "Valoarea creditului este prea mare pentru venitul si cheltuielile tale! Te rugam sa incerci o suma mai mica decat 206014.190000 Lei");
    }

    private MortgageCalculationResponse getMortgageCalculationResponse() {
        return MortgageCalculationResponse.builder()
                .interestRateType(MixedInterestRateType.builder().interestRate(0).fixedPeriod(3).build())
                .nominalInterestRate(BigDecimal.valueOf(6.75))
                .interestRateFormula(new InterestRateFormula(3.99, 5.6))
                .loanAmount(new Amount("RON", BigDecimal.valueOf(40500).setScale(2, RoundingMode.HALF_DOWN)))
                .maxAmount(new Amount("RON", BigDecimal.valueOf(206014.19)))
                .downPayment(new Amount("RON", BigDecimal.valueOf(10000).setScale(2, RoundingMode.HALF_DOWN)))
                .loanAmountWithFee(new Amount("RON", BigDecimal.valueOf(40500).setScale(2, RoundingMode.HALF_DOWN)))
                .totalPaymentAmount(new Amount("RON", BigDecimal.valueOf(74328)))
                .monthlyInstallment(new MonthlyInstallment(BigDecimal.valueOf(304.88), BigDecimal.valueOf(278.78)))
                .loanCosts(new LoanCosts(null,
                        List.of(new LifeInsurance(new Amount("RON", BigDecimal.valueOf(10.53000000)), Frequency.MONTHLY)),
                        new DiscountsValues(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.valueOf(4.92)),
                        new TotalDiscountsValues(BigDecimal.ZERO, BigDecimal.ZERO)))
                .annualPercentageRate(BigDecimal.valueOf(6.64))
                .tenor(22)
                .build();
    }

    private MortgageCalculationResponse getMortgageCalculationResponseForConstructie() {
        return MortgageCalculationResponse.builder()
                .interestRateType(MixedInterestRateType.builder().interestRate(0).fixedPeriod(3).build())
                .nominalInterestRate(BigDecimal.valueOf(6.75))
                .interestRateFormula(new InterestRateFormula(3.99, 5.6))
                .loanAmount(new Amount("RON", BigDecimal.valueOf(50500).setScale(2, RoundingMode.HALF_DOWN)))
                .maxAmount(new Amount("RON", BigDecimal.valueOf(206014.19)))
                .downPayment(new Amount("RON", BigDecimal.ZERO))
                .loanAmountWithFee(new Amount("RON", BigDecimal.valueOf(50500).setScale(2, RoundingMode.HALF_DOWN)))
                .housePrice(new Amount("RON", BigDecimal.valueOf(71428.55).setScale(6, RoundingMode.HALF_DOWN)))
                .totalPaymentAmount(new Amount("RON", BigDecimal.valueOf(91867)))
                .monthlyInstallment(new MonthlyInstallment(BigDecimal.valueOf(377.69), BigDecimal.valueOf(345.14)))
                .loanCosts(new LoanCosts(null,
                        List.of(new LifeInsurance(new Amount("RON", BigDecimal.valueOf(13.13)), Frequency.MONTHLY)),
                        new DiscountsValues(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.valueOf(6.13)),
                        new TotalDiscountsValues(BigDecimal.valueOf(0), BigDecimal.valueOf(0))))
                .annualPercentageRate(BigDecimal.valueOf(6.48))
                .noDocAmount(BigDecimal.valueOf(15000).setScale(1, RoundingMode.HALF_DOWN))
                .minGuaranteeAmount(BigDecimal.valueOf(62500).setScale(6, RoundingMode.HALF_DOWN))
                .tenor(22)
                .build();
    }

    private MortgageCalculationResponse getMortgageCalculationResponseWhenAvans30() {
        return MortgageCalculationResponse.builder()
                .interestRateType(MixedInterestRateType.builder().interestRate(0).fixedPeriod(3).build())
                .nominalInterestRate(BigDecimal.valueOf(6.55))
                .interestRateFormula(new InterestRateFormula(3.79, 5.6))
                .loanAmount(new Amount("RON", BigDecimal.valueOf(35500).setScale(2, RoundingMode.HALF_DOWN)))
                .maxAmount(new Amount("RON", BigDecimal.valueOf(209508.94)))
                .downPayment(new Amount("RON", BigDecimal.valueOf(15000).setScale(2, RoundingMode.HALF_DOWN)))
                .loanAmountWithFee(new Amount("RON", BigDecimal.valueOf(35500).setScale(2, RoundingMode.HALF_DOWN)))
                .totalPaymentAmount(new Amount("RON", BigDecimal.valueOf(64471)))
                .monthlyInstallment(new MonthlyInstallment(BigDecimal.valueOf(264.17), BigDecimal.valueOf(241.48)))
                .loanCosts(new LoanCosts(null,
                        List.of(new LifeInsurance(new Amount("RON", BigDecimal.valueOf(9.23)), Frequency.MONTHLY)),
                        new DiscountsValues(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN), BigDecimal.valueOf(4.31)),
                        new TotalDiscountsValues(BigDecimal.valueOf(4.31), BigDecimal.valueOf(1092.24))))
                .annualPercentageRate(BigDecimal.valueOf(6.55))
                .tenor(22)
                .build();
    }

    private InterestRateAdditionalInfo getInterestRateAdditionalInfoResponse() {
        return InterestRateAdditionalInfo.builder()
                .bankMarginRate(3.99)
                .defaultInterestRate(6.75)
                .interestRate(6.75)
                .defaultBankMarginRate(3.99)
                .defaultVariableInterestAfterFixedInterest(5.66)
                .variableInterestAfterFixedInterest(5.66)
                .yearsWithFixedInterest(3)
                .discounts(List.of(Discount.builder()
                                .discountName("avans 30")
                                .discountValue(0.2)
                        .build()))
                .build();
    }

    private AdditionalCalculationInfo getAdditionalInfoResponse() {
        return AdditionalCalculationInfo.builder()
                .currency("RON")
                .productId(40)
                .ircc(5.6f)
                .analysisCommission(BigDecimal.valueOf(500))
                .buildingInsurancePremiumRate(BigDecimal.ZERO)
                .ltv(80)
                .paymentOrderCommission(BigDecimal.ZERO)
                .lifeInsurance(BigDecimal.valueOf(0.026))
                .buildingPADInsurancePremiumRate(BigDecimal.valueOf(99.54))
                .feeCommission(BigDecimal.valueOf(533.037))
                .postGrantCommission(BigDecimal.TEN)
                .fee(BigDecimal.TEN)
                .monthlyCurrentAccountCommission(BigDecimal.valueOf(5))
                .build();
    }

    private MortgageCalculationRequest getRequest(String productCode, BigDecimal amount, BigDecimal downPayment) {
        return MortgageCalculationRequest.builder()
                .productCode(productCode)
                .installmentType(InstallmentType.EQUAL_INSTALLMENTS)
                .owner(false)
                .hasInsurance(false)
                .downPayment(downPayment)
                .specialOfferRequirements(new SpecialOfferRequirements(false, false))
                .interestRateType(MixedInterestRateType.builder()
                        .fixedPeriod(3)
                        .build())
                .age(43)
                .income(new Income(BigDecimal.valueOf(10000), BigDecimal.valueOf(2500)))
                .loanAmount(new Amount("RON", amount))
                .area(new Area("Bucuresti", "Bucuresti"))
                .build();
    }
}

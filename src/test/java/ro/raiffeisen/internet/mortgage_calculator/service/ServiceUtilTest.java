package ro.raiffeisen.internet.mortgage_calculator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.raiffeisen.internet.mortgage_calculator.exception.InternalServerException;
import ro.raiffeisen.internet.mortgage_calculator.helper.MortgageCalculatorMapper;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.*;
import ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class ServiceUtilTest {

    @Mock
    private RetrieveService retrieveClient;
    @Mock
    private FxClientRetrieve fxClientRetrieve;
    @Mock
    private MortgageCalculatorMapper calculatorMapper;
    @InjectMocks
    private ServiceUtil serviceUtil;

    @Test
    public void calculateAvailableRateTest() {
        BigDecimal actualResponse = serviceUtil.calculateAvailableRate(new Income(BigDecimal.valueOf(10000), BigDecimal.TEN));
        assertEquals(BigDecimal.valueOf(3990.0), actualResponse);
    }

    @Test
    public void calculateMaxPeriodTest() {
        Integer actualResponse = serviceUtil.calculateMaxPeriod(44, 30);
        assertEquals(21, actualResponse);
    }

    @Test
    public void calculatePVTest() {
        double actualResponse = serviceUtil.calculatePV(6.5, 30, 3500);
        assertEquals(96671.77337784052, actualResponse);
    }

    @Test
    public void getBackendValueTest() {
        String actualResponse = serviceUtil.getBackEndValue(new MixedInterestRateType(MixedInterestRateType.Type.MIXED, 6.5, 3));
        assertEquals("Dobanda mixta", actualResponse);
    }

    @Test
    public void getAmountWithAnalysisCommissionTest() {
        BigDecimal actualResponse = serviceUtil.getAmountWithAnalysisCommission(BigDecimal.valueOf(1000), BigDecimal.valueOf(500));
        assertEquals(BigDecimal.valueOf(1500), actualResponse.setScale(0, RoundingMode.HALF_DOWN));
    }

    @Test
    void testSetInterestRateDetailsMixedInterestRateType() {
        LoanInterestRate fixedInterestRate = LoanInterestRate.builder().interestRateType("Dobanda fixa")
                .margin(5)
                .interestRate(5f)
                .year(5)
                .build();
        LoanInterestRate variableInterestRate = LoanInterestRate.builder().interestRateType("Dobanda variabila")
                .margin(5)
                .year(5)
                .interestRate(9f)
                .build();
        List<LoanInterestRate> loanInterestRates = Arrays.asList(fixedInterestRate, variableInterestRate);
        MixedInterestRateType mixedInterestRateType = mock(MixedInterestRateType.class);
        when(mixedInterestRateType.getFixedPeriod()).thenReturn(5);
        InterestRateAdditionalInfo additionalInfo = InterestRateAdditionalInfo.builder().build();
        serviceUtil.setInterestRateDetails(loanInterestRates, mixedInterestRateType, additionalInfo);

        assertEquals(5, additionalInfo.getBankMarginRate());
        assertEquals(5, additionalInfo.getDefaultBankMarginRate());
        assertEquals(5, additionalInfo.getInterestRate());
        assertEquals(5, additionalInfo.getDefaultInterestRate());
        assertEquals(9, additionalInfo.getDefaultVariableInterestAfterFixedInterest());
        assertEquals(9, additionalInfo.getVariableInterestAfterFixedInterest());
    }

    @Test
    void testSetInterestRateDetailsVariableInterestRateType() {
        LoanInterestRate variableInterestRate = LoanInterestRate.builder()
                .interestRateType("Dobanda variabila")
                .margin(5)
                .interestRate(9f)
                .build();
        List<LoanInterestRate> loanInterestRates = Collections.singletonList(variableInterestRate);
        InterestRateType interestRateType = mock(InterestRateType.class);

        InterestRateAdditionalInfo additionalInfo = InterestRateAdditionalInfo.builder().build();
        serviceUtil.setInterestRateDetails(loanInterestRates, interestRateType, additionalInfo);

        assertEquals(5, additionalInfo.getBankMarginRate());
        assertEquals(5, additionalInfo.getDefaultBankMarginRate());
        assertEquals(9, additionalInfo.getInterestRate());
        assertEquals(9, additionalInfo.getDefaultInterestRate());
    }

    @Test
    void testCalculateBuildingInsurancePremiumRate_InRONCurrency() {
        Amount loanAmount = new Amount("RON", BigDecimal.valueOf(100000));
        BigDecimal analysisCommission = BigDecimal.valueOf(1000);
        Integer ltv = 80;
        when(fxClientRetrieve.getExchangeRates(anyString())).thenReturn(List.of(ExchangeRate.builder().currencyPair("EURRON").referenceRate("4.97").build()));
        BigDecimal result = serviceUtil.calculateBuildingInsurancePremiumRate("RON", loanAmount, analysisCommission, ltv, new BigDecimal("0.11"));

        assertEquals(BigDecimal.valueOf(138.87).setScale(2, RoundingMode.HALF_UP), result.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testCalculateBuildingInsurancePremiumRate_NoExchangeRateFound() {
        Amount loanAmount = new Amount("RON", BigDecimal.valueOf(100000));
        BigDecimal analysisCommission = BigDecimal.valueOf(1000);
        Integer ltv = 80;

        when(fxClientRetrieve.getExchangeRates("EUR")).thenReturn(Collections.emptyList());

        InternalServerException exception = assertThrows(InternalServerException.class, () ->
                serviceUtil.calculateBuildingInsurancePremiumRate("RON", loanAmount, analysisCommission, ltv, any()));

        assertEquals("No exchange rate found", exception.getMessage());
    }

    @Test
    void testRetrieveAdditionalInfo_WithValidRequest() {
        MortgageCalculationRequest request = MortgageCalculationRequest.builder()
                .installmentType(InstallmentType.EQUAL_INSTALLMENTS)
                .productCode("FlexiCredit")
                .owner(false)
                .hasInsurance(false)
                .specialOfferRequirements(new SpecialOfferRequirements(false, false))
                .interestRateType(MixedInterestRateType.builder()
                        .fixedPeriod(3)
                        .build())
                .age(43)
                .income(new Income(BigDecimal.valueOf(10000), BigDecimal.valueOf(2500)))
                .loanAmount(new Amount("RON", BigDecimal.valueOf(50000)))
                .area(new Area("Bucuresti", "Bucuresti"))
                .build();

        LoanProduct loanProduct = LoanProduct.builder().idLoan(1).productLoan("FlexiCredit").build();
        LoanAllParameters loanAllParameters = new LoanAllParameters();
        NomenclatureDistrict district = NomenclatureDistrict.builder().city("Bucuresti").county("Bucuresti").zone(1).build();
        AdditionalCalculationInfo additionalCalculationInfo = new AdditionalCalculationInfo();

        when(serviceUtil.retrieveLoanProduct(anyString())).thenReturn(loanProduct);
        when(retrieveClient.getLoanAllParametersByMultipleArguments(anyInt(), anyBoolean(), anyString(), anyString(), anyBoolean())).thenReturn(loanAllParameters);
        when(retrieveClient.getDistricts()).thenReturn(List.of(district));
        when(retrieveClient.getLtvByAreaOwnerAndSum(anyDouble(), anyBoolean(), anyInt(), anyInt())).thenReturn(80);
        when(calculatorMapper.buildAllAdditionalInfo(any(), anyInt())).thenReturn(additionalCalculationInfo);

        AdditionalCalculationInfo result = serviceUtil.retrieveAdditionalInfo(request);

        assertEquals("RON", result.getCurrency());
        assertEquals(1, result.getProductId());
    }

    @Test
    void testRetrieveInterestRate_MixedInterestRateType() {
        MortgageCalculationRequest request = MortgageCalculationRequest.builder()
                .specialOfferRequirements(new SpecialOfferRequirements(false, false))
                .interestRateType(MixedInterestRateType.builder()
                        .fixedPeriod(3)
                        .build())
                .build();

        List<LoanInterestRate> loanInterestRates = List.of(
                LoanInterestRate.builder().interestRateType("Dobanda fixa").interestRate(5).margin(2).year(3).build(),
                LoanInterestRate.builder().interestRateType("Dobanda variabila").interestRate(9).margin(2).year(3).build());

        when(retrieveClient.getAllLoanInterestRatesByLoanProduct(anyInt(), anyBoolean(), anyBoolean())).thenReturn(loanInterestRates);
        when(retrieveClient.getDiscounts(anyInt())).thenReturn(List.of(Discount.builder().discountName("client").discountValue(0.2).build()));

        InterestRateAdditionalInfo additionalInfo = serviceUtil.retrieveInterestRate(request, 1);

        assertEquals(36, additionalInfo.getYearsWithFixedInterest());
        assertEquals(List.of(Discount.builder().discountName("client").discountValue(0.2).build()), additionalInfo.getDiscounts());
    }

    @Test
    void testCreateRepaymentPlanEntry_EqualInstallments() {
        int currentMonth = 0;
        MortgageCalculationRequest loanCalculationRequest = getMortgageCalculationRequest(InstallmentType.EQUAL_INSTALLMENTS);

        AdditionalCalculationInfo additionalInfo = getAdditionalCalculationInfo();
        InterestRateAdditionalInfo rateAdditionalInfo = getInterestRateAdditionalInfo();

        BigDecimal expectedPrincipal = BigDecimal.valueOf(0.00);
        BigDecimal expectedBalance = BigDecimal.valueOf(50500.00);
        BigDecimal expectedInterest = BigDecimal.valueOf(0.00);
        BigDecimal expectedFeeAmount = BigDecimal.valueOf(500.00);
        BigDecimal expectedInstallmentAmount = BigDecimal.valueOf(0.00);

        RepaymentPlanEntry result = serviceUtil.createRepaymentPlanEntry(currentMonth, loanCalculationRequest, additionalInfo, rateAdditionalInfo);

        assertEquals(currentMonth, result.getMonth());
        assertEquals(new Amount("RON", expectedPrincipal.setScale(2, RoundingMode.HALF_DOWN)), result.getReimbursedCapitalAmount());
        assertEquals(new Amount("RON", expectedBalance.setScale(2, RoundingMode.HALF_DOWN)), result.getRemainingLoanAmount());
        assertEquals(new Amount("RON", expectedInterest.setScale(2, RoundingMode.HALF_UP)), result.getInterestAmount());
        assertEquals(new Amount("RON", expectedFeeAmount.setScale(2, RoundingMode.HALF_DOWN)), result.getFeeAmount());
        assertEquals(new Amount("RON", expectedInstallmentAmount.setScale(2, RoundingMode.HALF_UP)), result.getInstallmentAmount());
    }

    @Test
    void testCreateRepaymentPlanEntry_DecreasingInstallments() {
        int currentMonth = 0;
        MortgageCalculationRequest loanCalculationRequest = getMortgageCalculationRequest(InstallmentType.DECREASING_INSTALLMENTS);

        AdditionalCalculationInfo additionalInfo = getAdditionalCalculationInfo();
        InterestRateAdditionalInfo rateAdditionalInfo = getInterestRateAdditionalInfo();

        BigDecimal expectedPrincipal = BigDecimal.valueOf(0.00);
        BigDecimal expectedBalance = BigDecimal.valueOf(50500.00);
        BigDecimal expectedInterest = BigDecimal.valueOf(0.00);
        BigDecimal expectedFeeAmount = BigDecimal.valueOf(500.00);
        BigDecimal expectedInstallmentAmount = BigDecimal.valueOf(0.00);

        RepaymentPlanEntry result = serviceUtil.createRepaymentPlanEntry(currentMonth, loanCalculationRequest, additionalInfo, rateAdditionalInfo);

        assertEquals(currentMonth, result.getMonth());
        assertEquals(new Amount("RON", expectedPrincipal.setScale(2, RoundingMode.HALF_DOWN)), result.getReimbursedCapitalAmount());
        assertEquals(new Amount("RON", expectedBalance.setScale(2, RoundingMode.HALF_DOWN)), result.getRemainingLoanAmount());
        assertEquals(new Amount("RON", expectedInterest.setScale(2, RoundingMode.HALF_UP)), result.getInterestAmount());
        assertEquals(new Amount("RON", expectedFeeAmount.setScale(2, RoundingMode.HALF_DOWN)), result.getFeeAmount());
        assertEquals(new Amount("RON", expectedInstallmentAmount.setScale(2, RoundingMode.HALF_UP)), result.getInstallmentAmount());
    }

    @Test
    void testCalculateFeeAmount_WithinTenor_ShouldAddCommission() {
        int currentMonth = 13; // After first year, should add commission
        MortgageCalculationRequest request = getMortgageCalculationRequest(InstallmentType.EQUAL_INSTALLMENTS);
        request.setTenor(22);
        BigDecimal feeAmount = serviceUtil.calculateFeeAmount(currentMonth, getAdditionalCalculationInfo(), request);

        assertEquals(BigDecimal.valueOf(104.54).setScale(2), feeAmount.setScale(2));
    }

    private InterestRateAdditionalInfo getInterestRateAdditionalInfo() {
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

    private AdditionalCalculationInfo getAdditionalCalculationInfo() {
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

    private MortgageCalculationRequest getMortgageCalculationRequest(InstallmentType type) {
        return MortgageCalculationRequest.builder()
                .installmentType(type)
                .productCode("constructie")
                .owner(false)
                .hasInsurance(false)
                .specialOfferRequirements(new SpecialOfferRequirements(false, false))
                .interestRateType(MixedInterestRateType.builder()
                        .fixedPeriod(3)
                        .build())
                .age(43)
                .income(new Income(BigDecimal.valueOf(10000), BigDecimal.valueOf(2500)))
                .loanAmount(new Amount("RON", BigDecimal.valueOf(50000)))
                .area(new Area("Bucuresti", "Bucuresti"))
                .build();
    }
}

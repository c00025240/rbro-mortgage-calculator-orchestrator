package ro.raiffeisen.internet.mortgage_calculator.service.calculator;

import org.springframework.stereotype.Component;
import ro.raiffeisen.internet.mortgage_calculator.model.Amount;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationResponse;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateTypeFormula;
import ro.raiffeisen.internet.mortgage_calculator.service.ServiceUtil;

import java.math.BigDecimal;

/**
 * Calculator implementation for "Flexi Integral" credit product.
 */
@Component
public class FlexiIntegralCalculator extends AbstractMortgageCalculator {

    public FlexiIntegralCalculator(ServiceUtil serviceUtil) {
        super(serviceUtil);
    }

    @Override
    public boolean supports(String productCode) {
        return "FlexiIntegral".equals(productCode);
    }

    @Override
    protected void calculateProductSpecificDetails(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate) {

        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal analysisCommission = additionalInfo.getAnalysisCommission();

        BigDecimal garantie = calculateGuaranteeAmount(additionalInfo, loanAmount.doubleValue());
        BigDecimal garantiePentruDiscount = calculateGuaranteeAmountBigDecimal(80, loanAmount);

        boolean shouldApplyGuaranteeDiscount = garantiePentruDiscount.compareTo(garantie) <= 0;
        if (shouldApplyGuaranteeDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }

        calculateMaxAmount(request, response, additionalInfo.getCurrency(), rateTypeFormula, availableRate, loanAmount);
        
        response.setMinGuaranteeAmount(garantie);
        response.setLoanAmount(new Amount(additionalInfo.getCurrency(), 
                serviceUtil.getAmountWithAnalysisCommission(loanAmount, analysisCommission)));
        response.setLoanAmountWithFee(response.getLoanAmount());
        response.setHousePrice(new Amount(additionalInfo.getCurrency(), garantiePentruDiscount));
    }

    @Override
    protected boolean shouldApplyGuaranteeDiscount(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo) {
        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal garantie = calculateGuaranteeAmount(additionalInfo, loanAmount.doubleValue());
        BigDecimal garantiePentruDiscount = calculateGuaranteeAmountBigDecimal(80, loanAmount);
        return garantiePentruDiscount.compareTo(garantie) <= 0;
    }
}


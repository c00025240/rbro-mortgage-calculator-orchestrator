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
import java.math.RoundingMode;

/**
 * Calculator implementation for "Casa Ta" credit product.
 */
@Component
public class CasaTaCalculator extends AbstractMortgageCalculator {

    public CasaTaCalculator(ServiceUtil serviceUtil) {
        super(serviceUtil);
    }

    @Override
    public boolean supports(String productCode) {
        return "CasaTa".equals(productCode);
    }

    @Override
    protected void calculateProductSpecificDetails(
            MortgageCalculationRequest request,
            MortgageCalculationResponse response,
            AdditionalCalculationInfo additionalInfo,
            InterestRateAdditionalInfo interestRateAdditionalInfo,
            InterestRateTypeFormula rateTypeFormula,
            BigDecimal availableRate) {

        String currency = additionalInfo.getCurrency();
        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal valoareCredit = serviceUtil.calculateCreditAmount(loanAmount, additionalInfo.getLtv());
        BigDecimal downPaymentLtv = loanAmount.subtract(valoareCredit);

        BigDecimal downPayment = request.getDownPayment();
        if (downPayment != null) {
            validateDownPaymentNotGreaterThanAmount(downPayment, loanAmount);
        }

        response.setDownPayment(new Amount(currency, downPayment == null ?
                downPaymentLtv.setScale(2, RoundingMode.HALF_DOWN) :
                downPayment.setScale(2, RoundingMode.HALF_DOWN)));

        BigDecimal requestedAmount = loanAmount.subtract(response.getDownPayment().getAmount().setScale(2, RoundingMode.HALF_DOWN));
        response.setLoanAmount(new Amount(currency, serviceUtil.getAmountWithAnalysisCommission(requestedAmount, additionalInfo.getAnalysisCommission())));
        response.setLoanAmountWithFee(response.getLoanAmount());

        boolean shouldApplyDownPaymentDiscount = shouldApplyDownPaymentDiscount(response.getDownPayment().getAmount(), loanAmount);
        if (shouldApplyDownPaymentDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }

        calculateMaxAmount(request, response, currency, rateTypeFormula, availableRate, valoareCredit);

        request.setLoanAmount(new Amount(currency, loanAmount.subtract(downPayment != null ? downPayment : downPaymentLtv)));
    }

    @Override
    protected boolean shouldApplyGuaranteeDiscount(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo) {
        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal downPayment = request.getDownPayment();
        if (downPayment != null) {
            return shouldApplyDownPaymentDiscount(downPayment, loanAmount);
        }
        // If no down payment specified, calculate based on LTV
        BigDecimal valoareCredit = serviceUtil.calculateCreditAmount(loanAmount, additionalInfo.getLtv());
        BigDecimal downPaymentLtv = loanAmount.subtract(valoareCredit);
        return shouldApplyDownPaymentDiscount(downPaymentLtv, loanAmount);
    }
}


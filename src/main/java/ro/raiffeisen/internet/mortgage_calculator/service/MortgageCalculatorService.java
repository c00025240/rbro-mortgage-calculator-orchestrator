package ro.raiffeisen.internet.mortgage_calculator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ro.raiffeisen.internet.mortgage_calculator.exception.UnprocessableEntityException;
import ro.raiffeisen.internet.mortgage_calculator.helper.MortgageCalculatorMapper;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.Discount;
import ro.raiffeisen.internet.mortgage_calculator.model.client.LoanProduct;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.CalculatedValues;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateTypeFormula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MortgageCalculatorService {

    private final ServiceUtil initialCalculationService;
    private final MortgageCalculatorMapper mapper;
    private final ValidationService validationService;

    @Cacheable(value = "mortgageCalculatorCache", keyGenerator = "mortgageRequestKeyGenerator")
    public MortgageCalculationResponse createCalculation(MortgageCalculationRequest request) {
        log.info("/calculator/mortgage-calculator request body:" + mapper.mapToJson(request));
        validationService.validateRequest(request);

        int maxTenor = initialCalculationService.calculateMaxPeriod(request.getAge(), request.getTenor());
        request.setTenor(maxTenor * 12);
        MortgageCalculationResponse response = MortgageCalculationResponse.builder().tenor(maxTenor).build();

        switch (request.getProductCode()) {
            case "CasaTa" -> calculateDetailsForCasaTa(request, response);
            case "Constructie" -> calculateDetailsForConstructie(request, response);
            case "CreditVenit" -> calculateDetailsForCreditVenit(request, response);
            case "FlexiIntegral" -> calculateDetailsForFlexiIntegral(request, response);
        }

        return response;
    }

    private void calculateDetailsForFlexiIntegral(MortgageCalculationRequest request, MortgageCalculationResponse response) {
        AdditionalCalculationInfo additionalInfo = retrieveAdditionalInfo(request);
        InterestRateAdditionalInfo interestRateAdditionalInfo = retrieveInterestRate(request, additionalInfo.getProductId());
        InterestRateTypeFormula rateTypeFormula = calculateInterestRateBasedOnChosenDiscounts(interestRateAdditionalInfo, request);

        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal analysisCommission = additionalInfo.getAnalysisCommission();

        BigDecimal availableRate = initialCalculationService.calculateAvailableRate(request.getIncome());

        BigDecimal garantie = calculateGuaranteeAmount(additionalInfo, loanAmount.doubleValue());
        BigDecimal garantiePentruDiscount = calculateGuaranteeAmountBigDecimal(80, loanAmount);

        boolean shouldApplyGuaranteeDiscount = garantiePentruDiscount.compareTo(garantie) <= 0;
        if (shouldApplyGuaranteeDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }

        calculateMaxAmount(request, response, additionalInfo.getCurrency(), rateTypeFormula, availableRate, loanAmount);
        response.setMinGuaranteeAmount(garantie);
        response.setLoanAmount(new Amount(additionalInfo.getCurrency(), initialCalculationService.getAmountWithAnalysisCommission(loanAmount, analysisCommission)));
        response.setLoanAmountWithFee(response.getLoanAmount());
        response.setHousePrice(new Amount(additionalInfo.getCurrency(), garantiePentruDiscount));

        calculateCommonDetails(request, additionalInfo, response, interestRateAdditionalInfo, rateTypeFormula, shouldApplyGuaranteeDiscount);

    }

    private void calculateDetailsForCreditVenit(MortgageCalculationRequest request, MortgageCalculationResponse response) {
        if (request.getLoanAmount() != null) {
            processLoanAmountCreditVenit(request, response);
        } else {
            processDefaultLoanAmountCreditVenit(request, response);
        }
    }

    private void processLoanAmountCreditVenit(MortgageCalculationRequest request, MortgageCalculationResponse response) {
        AdditionalCalculationInfo additionalInfo = retrieveAdditionalInfo(request);
        InterestRateAdditionalInfo interestRateAdditionalInfo = retrieveInterestRate(request, additionalInfo.getProductId());
        InterestRateTypeFormula rateTypeFormula = calculateInterestRateBasedOnChosenDiscounts(interestRateAdditionalInfo, request);

        BigDecimal availableRate = initialCalculationService.calculateAvailableRate(request.getIncome());
        BigDecimal loanAmount = request.getLoanAmount().getAmount();

        BigDecimal downPayment = request.getDownPayment();
        if (downPayment.compareTo(loanAmount) > 0)
            throw new UnprocessableEntityException("Ne pare rau! Contributia proprie nu poate fi mai mare decat suma solicitata", null);

        boolean shouldApplyDownPaymentDiscount = shouldApplyDownPaymentDiscount(downPayment, request.getLoanAmount().getAmount());
        if (shouldApplyDownPaymentDiscount)
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);

        calculateMaxAmount(request, response, additionalInfo.getCurrency(), rateTypeFormula, availableRate, loanAmount);
        response.setLoanAmount(new Amount(additionalInfo.getCurrency(), initialCalculationService.getAmountWithAnalysisCommission(loanAmount.subtract(downPayment), additionalInfo.getAnalysisCommission())));
        response.setLoanAmountWithFee(response.getLoanAmount());

        response.setDownPayment(new Amount(additionalInfo.getCurrency(), downPayment));
        response.setHousePrice(new Amount(additionalInfo.getCurrency(), loanAmount.add(downPayment)));

        request.setLoanAmount(new Amount(additionalInfo.getCurrency(), loanAmount.subtract(downPayment)));

        calculateCommonDetails(request, additionalInfo, response, interestRateAdditionalInfo, rateTypeFormula, shouldApplyDownPaymentDiscount);
    }

    private void processDefaultLoanAmountCreditVenit(MortgageCalculationRequest request, MortgageCalculationResponse response) {
        LoanProduct loanProduct = initialCalculationService.retrieveLoanProduct(request.getProductCode());
        InterestRateAdditionalInfo interestRateAdditionalInfo = retrieveInterestRate(request, loanProduct.getIdLoan());

        InterestRateTypeFormula rateTypeFormula = calculateInterestRateBasedOnChosenDiscounts(interestRateAdditionalInfo, request);
        BigDecimal availableRate = initialCalculationService.calculateAvailableRate(request.getIncome());

        double maxLoanAmount = initialCalculationService.calculatePV(rateTypeFormula.getInterestRate(), request.getTenor(), availableRate.doubleValue());
        request.setLoanAmount(new Amount("RON", BigDecimal.valueOf(maxLoanAmount)));

        AdditionalCalculationInfo additionalInfo = retrieveAdditionalInfo(request);

        request.setLoanAmount(new Amount(additionalInfo.getCurrency(), BigDecimal.valueOf(maxLoanAmount).subtract(additionalInfo.getAnalysisCommission())));
        response.setMaxAmount(
                new Amount(
                        additionalInfo.getCurrency(),
                        BigDecimal.valueOf(maxLoanAmount).setScale(2, RoundingMode.HALF_DOWN)));

        BigDecimal garantie = calculateGuaranteeAmount(additionalInfo, maxLoanAmount);
        BigDecimal downPayment = garantie.subtract(BigDecimal.valueOf(maxLoanAmount));

        boolean shouldApplyDownPaymentDiscount = shouldApplyDownPaymentDiscount(downPayment, request.getLoanAmount().getAmount());
        if (shouldApplyDownPaymentDiscount)
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);

        response.setHousePrice(new Amount(additionalInfo.getCurrency(), BigDecimal.valueOf(maxLoanAmount).add(downPayment)));
        response.setDownPayment(new Amount(additionalInfo.getCurrency(), downPayment));
        response.setLoanAmount(new Amount(additionalInfo.getCurrency(), BigDecimal.valueOf(maxLoanAmount)));
        response.setLoanAmountWithFee(response.getLoanAmount());
        response.setMinGuaranteeAmount(garantie);

        calculateCommonDetails(request, additionalInfo, response, interestRateAdditionalInfo, rateTypeFormula, shouldApplyDownPaymentDiscount);
    }

    private void calculateDetailsForConstructie(MortgageCalculationRequest request, MortgageCalculationResponse response) {
        AdditionalCalculationInfo additionalInfo = retrieveAdditionalInfo(request);
        InterestRateAdditionalInfo interestRateAdditionalInfo = retrieveInterestRate(request, additionalInfo.getProductId());
        BigDecimal loanAmount = request.getLoanAmount().getAmount();

        String currency = additionalInfo.getCurrency();
        BigDecimal contributieProprie = request.getDownPayment() == null ? BigDecimal.ZERO : request.getDownPayment();

        if (contributieProprie.compareTo(loanAmount) > 0)
            throw new UnprocessableEntityException("Ne pare rau! Contributia proprie nu poate fi mai mare decat suma solicitata", null);
        response.setDownPayment(new Amount(currency, contributieProprie));

        BigDecimal valoareCredit = request.getLoanAmount().getAmount().subtract(contributieProprie);
        BigDecimal garantie = calculateGuaranteeAmountBigDecimal(additionalInfo.getLtv(), valoareCredit);
        BigDecimal garantiePentruDiscount = calculateGuaranteeAmountBigDecimal(80, valoareCredit);
        BigDecimal sumeFaraJustificare = calculateNoDocAmount(valoareCredit);

        response.setMinGuaranteeAmount(garantie);
        response.setNoDocAmount(sumeFaraJustificare);
        response.setHousePrice(new Amount(additionalInfo.getCurrency(), garantiePentruDiscount));

        InterestRateTypeFormula rateTypeFormula = calculateInterestRateBasedOnChosenDiscounts(interestRateAdditionalInfo, request);
        boolean shouldApplyGuaranteeDiscount = garantiePentruDiscount.compareTo(garantie) <= 0;
        if (shouldApplyGuaranteeDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }

        BigDecimal availableRate = initialCalculationService.calculateAvailableRate(request.getIncome());

        calculateMaxAmount(request, response, currency, rateTypeFormula, availableRate, valoareCredit);

        response.setLoanAmount(new Amount(currency, initialCalculationService.getAmountWithAnalysisCommission(loanAmount.subtract(contributieProprie), additionalInfo.getAnalysisCommission())));
        response.setLoanAmountWithFee(response.getLoanAmount());

        request.setLoanAmount(new Amount(currency, loanAmount.subtract(contributieProprie)));

        calculateCommonDetails(request, additionalInfo, response, interestRateAdditionalInfo, rateTypeFormula, shouldApplyGuaranteeDiscount);
    }

    private BigDecimal calculateGuaranteeAmountBigDecimal(Integer ltv, BigDecimal valoareCredit) {
        return BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(ltv), 6, RoundingMode.HALF_UP)
                .multiply(valoareCredit);
    }

    private BigDecimal calculateGuaranteeAmount(AdditionalCalculationInfo additionalInfo, double maxLoanAmount) {
        return BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(additionalInfo.getLtv()), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(maxLoanAmount).add(additionalInfo.getAnalysisCommission()));
    }

    private BigDecimal calculateNoDocAmount(BigDecimal valoareCredit) {
        return BigDecimal.valueOf(0.3).multiply(valoareCredit);
    }

    private void calculateCommonDetails(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo, MortgageCalculationResponse response, InterestRateAdditionalInfo interestRateAdditionalInfo, InterestRateTypeFormula rateTypeFormula, boolean shouldApplyDownPaymentDiscount) {
        additionalInfo.setBuildingInsurancePremiumRate(initialCalculationService.calculateBuildingInsurancePremiumRate(additionalInfo.getCurrency(),
                request.getLoanAmount(), additionalInfo.getAnalysisCommission(), additionalInfo.getLtv(), additionalInfo.getBuildingInsurancePremiumRate()));

        LoanCosts loanCosts = new LoanCosts();
        CalculatedValues calculatedValues = calculateBasedOnDiscounts(additionalInfo, request, loanCosts, interestRateAdditionalInfo);
        calculateTotalDiscounts(calculatedValues, loanCosts, request, shouldApplyDownPaymentDiscount);

        interestRateAdditionalInfo.setInterestRate(rateTypeFormula.getInterestRate());
        interestRateAdditionalInfo.setVariableInterestAfterFixedInterest(rateTypeFormula.getVariableInterestAfterFixedInterest());
        interestRateAdditionalInfo.setBankMarginRate(rateTypeFormula.getBankMarginRate());

        List<RepaymentPlanEntry> repaymentPlanEntries = createRepaymentPlanEntries(request, additionalInfo, interestRateAdditionalInfo);

        response.setMonthlyInstallment(initialCalculationService.calculateMonthlyInstallment(request.isHasInsurance(), repaymentPlanEntries,
                request.getInterestRateType(), additionalInfo.getMonthlyLifeInsurance().getValue().getAmount()));
        response.setInterestRateType(request.getInterestRateType());
        response.setInterestRateFormula(getInterestRateFormatedFormula(interestRateAdditionalInfo.getBankMarginRate(), additionalInfo.getIrcc()));
        response.setNominalInterestRate(BigDecimal.valueOf(interestRateAdditionalInfo.getInterestRate()).setScale(2, RoundingMode.HALF_DOWN));

        List<BigDecimal> cashFlows = repaymentPlanEntries.stream()
                .map(repaymentPlanEntry -> repaymentPlanEntry.getTotalPaymentAmount().getAmount())
                .collect(Collectors.toList());

        BigDecimal amountWithAnalysisCommission = initialCalculationService.getAmountWithAnalysisCommission(request.getLoanAmount().getAmount(), additionalInfo.getAnalysisCommission());
        BigDecimal dae = initialCalculationService.calculateDAE(cashFlows, amountWithAnalysisCommission, additionalInfo);

        loanCosts.setLifeInsurance(List.of(additionalInfo.getMonthlyLifeInsurance()));

        response.setLoanCosts(loanCosts);
        response.setAnnualPercentageRate(dae);
        response.setTotalPaymentAmount(initialCalculationService.calculateTotalPayment(cashFlows, additionalInfo));
        response.setCommissionDescription(additionalInfo.getCommissionDescription());
    }

    private InterestRateFormula getInterestRateFormatedFormula(double bankMarginRate, Float ircc) {
        DecimalFormat df = new DecimalFormat("#.00");
        double formattedBankMarginRate = Double.parseDouble(df.format(bankMarginRate));
        double formattedIrcc = Double.parseDouble(df.format(ircc));

        return InterestRateFormula.builder()
                .bankMarginRate(formattedBankMarginRate)
                .irccRate(formattedIrcc)
                .build();
    }

    private List<RepaymentPlanEntry> createRepaymentPlanEntries(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo, InterestRateAdditionalInfo interestRateAdditionalInfo) {
        List<RepaymentPlanEntry> repaymentPlanEntries = new ArrayList<>();
        for (int i = 0; i <= request.getTenor(); i++) {
            repaymentPlanEntries.add(initialCalculationService.createRepaymentPlanEntry(i, request, additionalInfo, interestRateAdditionalInfo));
        }
        return repaymentPlanEntries;
    }

    private AdditionalCalculationInfo retrieveAdditionalInfo(MortgageCalculationRequest request) {
        return initialCalculationService.retrieveAdditionalInfo(request);
    }

    private InterestRateAdditionalInfo retrieveInterestRate(MortgageCalculationRequest request, Integer productId) {
        return initialCalculationService.retrieveInterestRate(request, productId);
    }

    private void calculateDetailsForCasaTa(MortgageCalculationRequest request, MortgageCalculationResponse response) {
        AdditionalCalculationInfo additionalInfo = retrieveAdditionalInfo(request);
        InterestRateAdditionalInfo interestRateAdditionalInfo = retrieveInterestRate(request, additionalInfo.getProductId());

        String currency = additionalInfo.getCurrency();
        InterestRateTypeFormula rateTypeFormula = calculateInterestRateBasedOnChosenDiscounts(interestRateAdditionalInfo, request);
        BigDecimal availableRate = initialCalculationService.calculateAvailableRate(request.getIncome());

        BigDecimal loanAmount = request.getLoanAmount().getAmount();
        BigDecimal valoareCredit = initialCalculationService.calculateCreditAmount(loanAmount, additionalInfo.getLtv());
        BigDecimal downPaymentLtv = loanAmount.subtract(valoareCredit);

        BigDecimal downPayment = request.getDownPayment();
        if (downPayment != null && downPayment.compareTo(loanAmount) > 0)
            throw new UnprocessableEntityException("Ne pare rau! Contributia proprie nu poate fi mai mare decat suma solicitata", null);

        response.setDownPayment(new Amount(currency, request.getDownPayment() == null ?
                downPaymentLtv.setScale(2, RoundingMode.HALF_DOWN) :
                request.getDownPayment().setScale(2, RoundingMode.HALF_DOWN)));

        BigDecimal requestedAmount = loanAmount.subtract(response.getDownPayment().getAmount().setScale(2, RoundingMode.HALF_DOWN));
        response.setLoanAmount(new Amount(currency,initialCalculationService.getAmountWithAnalysisCommission(requestedAmount, additionalInfo.getAnalysisCommission())));
        response.setLoanAmountWithFee(response.getLoanAmount());

        boolean shouldApplyDownPaymentDiscount = shouldApplyDownPaymentDiscount(response.getDownPayment().getAmount(), loanAmount);
        if (shouldApplyDownPaymentDiscount) {
            applyDiscount(interestRateAdditionalInfo, "avans", rateTypeFormula);
        }
        calculateMaxAmount(request, response, currency, rateTypeFormula, availableRate, valoareCredit);

        request.setLoanAmount(new Amount(currency, loanAmount.subtract(request.getDownPayment() != null ?
                request.getDownPayment() : downPaymentLtv)));
        calculateCommonDetails(request, additionalInfo, response, interestRateAdditionalInfo, rateTypeFormula, shouldApplyDownPaymentDiscount);
    }

    private void calculateMaxAmount(MortgageCalculationRequest request, MortgageCalculationResponse response, String currency, InterestRateTypeFormula rateTypeFormula, BigDecimal availableRate, BigDecimal valoareCredit) {
        double maxLoanAmount = initialCalculationService.calculatePV(rateTypeFormula.getInterestRate(), request.getTenor(), availableRate.doubleValue());
        BigDecimal maxLoanAmountBd = BigDecimal.valueOf(maxLoanAmount).setScale(2, RoundingMode.HALF_DOWN);
        response.setMaxAmount(new Amount(currency, maxLoanAmountBd));

        if (maxLoanAmountBd.compareTo(valoareCredit) < 0) {
            throw new UnprocessableEntityException(String.format("Ne pare rau! ☹️\n" +
                    "Valoarea creditului este prea mare pentru venitul si cheltuielile tale! Te rugam sa incerci o suma mai mica decat %f Lei", maxLoanAmountBd), maxLoanAmountBd);
        }
    }

    private boolean shouldApplyDownPaymentDiscount(BigDecimal downPayment, BigDecimal amount) {
        return downPayment.compareTo(BigDecimal.valueOf(0.2).multiply(amount)) >= 0;
    }


    private void calculateTotalDiscounts(CalculatedValues calculatedValues, LoanCosts loanCosts, MortgageCalculationRequest request, boolean shouldApplyDownPaymentDiscount) {
        BigDecimal totalDiscountInstallment = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        if (request.getSpecialOfferRequirements().isHasSalaryInTheBank()) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountHasSalaryInTheBank());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountHasSalaryInTheBank(), request, loanCosts.getDiscounts().getDiscountAmountHasSalaryInTheBank()));
        }

        if (request.getSpecialOfferRequirements().isCasaVerde()) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountCasaVerde());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountCasaVerde(), request, loanCosts.getDiscounts().getDiscountAmountCasaVerde()));
        }

        if (request.isHasInsurance()) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountInsurance());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountInsurance(), request, loanCosts.getDiscounts().getDiscountAmountInsurance()));
        }

        if (shouldApplyDownPaymentDiscount) {
            totalDiscountInstallment = totalDiscountInstallment.add(loanCosts.getDiscounts().getDiscountAmountDownPayment());
            totalDiscountAmount = totalDiscountAmount.add(calculateTotalDiscountAmount(
                    calculatedValues.getVariableDiscountAmountDownPayment(), request, loanCosts.getDiscounts().getDiscountAmountDownPayment()));
        }

        loanCosts.setTotalDiscountsValues(new TotalDiscountsValues(totalDiscountInstallment, totalDiscountAmount));
    }

    private BigDecimal calculateTotalDiscountAmount(BigDecimal calculatedVariableValue, MortgageCalculationRequest request, BigDecimal discountAmount) {
        if (request.getInterestRateType() instanceof MixedInterestRateType interestRateType) {
            return discountAmount.multiply(BigDecimal.valueOf(interestRateType.getFixedPeriod())).multiply(new BigDecimal(12))
                    .add(calculatedVariableValue.multiply(BigDecimal.valueOf(request.getTenor() - interestRateType.getFixedPeriod() * 12L)));
        }
        return discountAmount.multiply(BigDecimal.valueOf(request.getTenor()));
    }

    private InterestRateTypeFormula calculateInterestRateBasedOnChosenDiscounts(InterestRateAdditionalInfo additionalInfo, MortgageCalculationRequest request) {
        double interestRate = additionalInfo.getDefaultInterestRate();
        double variableInterestRate = additionalInfo.getDefaultVariableInterestAfterFixedInterest();
        double bankMargin = additionalInfo.getDefaultBankMarginRate();
        InterestRateTypeFormula interestRateTypeFormula = new InterestRateTypeFormula(interestRate, bankMargin, variableInterestRate);

        if (request.getSpecialOfferRequirements().isHasSalaryInTheBank())
            applyDiscount(additionalInfo, "client", interestRateTypeFormula);

        if (request.getSpecialOfferRequirements().isCasaVerde())
            applyDiscount(additionalInfo, "green house", interestRateTypeFormula);

        if (request.isHasInsurance())
            applyDiscount(additionalInfo, "asigurare", interestRateTypeFormula);

        return interestRateTypeFormula;
    }

    private void applyDiscount(InterestRateAdditionalInfo additionalInfo, String discountName, InterestRateTypeFormula formula) {
        double discountValue = getDiscountValue(additionalInfo, discountName);

        formula.setInterestRate(formula.getInterestRate() - discountValue);
        formula.setVariableInterestAfterFixedInterest(formula.getVariableInterestAfterFixedInterest() - discountValue);
        formula.setBankMarginRate(formula.getBankMarginRate() - discountValue);
    }

    private double getDiscountValue(InterestRateAdditionalInfo additionalInfo, String discountName) {
        return additionalInfo.getDiscounts().stream()
                .filter(discount -> discount.getDiscountName().equals(discountName))
                .map(Discount::getDiscountValue)
                .findFirst()
                .orElse(0d);
    }

    private CalculatedValues calculateBasedOnDiscounts(AdditionalCalculationInfo additionalInfo, MortgageCalculationRequest request, LoanCosts loanCosts, InterestRateAdditionalInfo interestRateAdditionalInfo) {
        double defaultInterestRate = interestRateAdditionalInfo.getDefaultInterestRate();
        BigDecimal totalPaymentDefaultValue = calculateTotalPaymentBasedOnDiscount(additionalInfo, request, "No discount", defaultInterestRate, interestRateAdditionalInfo);

        DiscountsValues discountsValues = new DiscountsValues();
        discountsValues.setDiscountAmountDownPayment(calculateDiscountDifference(additionalInfo, request, "avans", defaultInterestRate, totalPaymentDefaultValue, interestRateAdditionalInfo));
        discountsValues.setDiscountAmountCasaVerde(calculateDiscountDifference(additionalInfo, request, "green house", defaultInterestRate, totalPaymentDefaultValue, interestRateAdditionalInfo));
        discountsValues.setDiscountAmountInsurance(calculateDiscountDifference(additionalInfo, request, "asigurare", defaultInterestRate, totalPaymentDefaultValue, interestRateAdditionalInfo));
        discountsValues.setDiscountAmountHasSalaryInTheBank(calculateDiscountDifference(additionalInfo, request, "client", defaultInterestRate, totalPaymentDefaultValue, interestRateAdditionalInfo));

        loanCosts.setDiscounts(discountsValues);

        CalculatedValues calculatedValues = new CalculatedValues();
        if (request.getInterestRateType() instanceof MixedInterestRateType) {
            defaultInterestRate = interestRateAdditionalInfo.getDefaultVariableInterestAfterFixedInterest();
            BigDecimal totalVariablePaymentDefaultValue = calculateTotalPaymentBasedOnDiscount(additionalInfo, request, "No discount", defaultInterestRate, interestRateAdditionalInfo);
            calculatedValues.setVariableDiscountAmountDownPayment(calculateDiscountDifference(additionalInfo, request, "avans", defaultInterestRate, totalVariablePaymentDefaultValue, interestRateAdditionalInfo));
            calculatedValues.setVariableDiscountAmountCasaVerde(calculateDiscountDifference(additionalInfo, request, "green house", defaultInterestRate, totalVariablePaymentDefaultValue, interestRateAdditionalInfo));
            calculatedValues.setVariableDiscountAmountInsurance(calculateDiscountDifference(additionalInfo, request, "asigurare", defaultInterestRate, totalVariablePaymentDefaultValue, interestRateAdditionalInfo));
            calculatedValues.setVariableDiscountAmountHasSalaryInTheBank(calculateDiscountDifference(additionalInfo, request, "client", defaultInterestRate, totalVariablePaymentDefaultValue, interestRateAdditionalInfo));
        }

        return calculatedValues;
    }

    private BigDecimal calculateDiscountDifference(AdditionalCalculationInfo additionalInfo, MortgageCalculationRequest request, String discountName, double defaultInterestRate, BigDecimal totalPaymentDefaultValue, InterestRateAdditionalInfo interestRateAdditionalInfo) {
        BigDecimal totalPaymentWithDiscount = calculateTotalPaymentBasedOnDiscount(additionalInfo, request, discountName, defaultInterestRate, interestRateAdditionalInfo);
        return totalPaymentDefaultValue.subtract(totalPaymentWithDiscount);
    }

    public BigDecimal calculateTotalPaymentBasedOnDiscount(AdditionalCalculationInfo additionalInfo, MortgageCalculationRequest request, String discountName, double defaultInterestRate, InterestRateAdditionalInfo interestRateAdditionalInfo) {
        double discountValue = interestRateAdditionalInfo.getDiscounts().stream()
                .filter(discount -> discount.getDiscountName().equals(discountName))
                .findFirst()
                .orElse(Discount.builder().discountValue(0d).build())
                .getDiscountValue();

        interestRateAdditionalInfo.setInterestRate(defaultInterestRate - discountValue);

        List<RepaymentPlanEntry> repaymentPlanEntries = createPartialRepaymentPlanEntries(request, additionalInfo, interestRateAdditionalInfo);
        return repaymentPlanEntries.get(1).getTotalPaymentAmount().getAmount();
    }

    private List<RepaymentPlanEntry> createPartialRepaymentPlanEntries(MortgageCalculationRequest request, AdditionalCalculationInfo additionalInfo, InterestRateAdditionalInfo interestRateAdditionalInfo) {
        List<RepaymentPlanEntry> repaymentPlanEntries = new ArrayList<>();
        for (int i = 0; i <= 1; i++) {
            repaymentPlanEntries.add(initialCalculationService.createRepaymentPlanEntry(i, request, additionalInfo, interestRateAdditionalInfo));
        }
        return repaymentPlanEntries;
    }

}

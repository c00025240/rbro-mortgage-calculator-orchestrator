package ro.raiffeisen.internet.mortgage_calculator.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.formula.functions.Finance;
import org.apache.poi.ss.formula.functions.Irr;
import org.springframework.stereotype.Service;
import ro.raiffeisen.internet.mortgage_calculator.exception.InternalServerException;
import ro.raiffeisen.internet.mortgage_calculator.helper.MortgageCalculatorMapper;
import ro.raiffeisen.internet.mortgage_calculator.model.*;
import ro.raiffeisen.internet.mortgage_calculator.model.client.*;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.AdditionalCalculationInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.utils.InterestRateAdditionalInfo;
import ro.raiffeisen.internet.mortgage_calculator.model.repayment.RepaymentPlanEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ServiceUtil {
    private static final String CURRENCY_RON = "RON";
    private static final String CURRENCY_EUR = "EUR";
    private static final String CURRENCY_PAIR = "EURRON";
    private final RetrieveService retrieveClient;
    private final FxClientRetrieve fxClientRetrieve;
    private final MortgageCalculatorMapper calculatorMapper;


    public BigDecimal calculateAvailableRate(Income income) {
        return (income.getCurrentIncome().multiply(new BigDecimal("0.4"))).subtract(income.getOtherInstallments());
    }

    public int calculateMaxPeriod(int age, int tenor) {
        int maxPeriodAllowed = 65 - age;

        if (tenor == 0) {
            return Math.min(maxPeriodAllowed, 30);
        } else if (tenor > maxPeriodAllowed) {
            return Math.min(maxPeriodAllowed, 30);
        } else {
            return Math.min(tenor, 30);
        }
    }

    public double calculatePV(double annualInterestRate, int maxPeriod, double availableRate) {
        double monthlyInterestRate = annualInterestRate / (100 * 12);

        return availableRate * (1 - Math.pow(1 + monthlyInterestRate, -maxPeriod)) / monthlyInterestRate;
    }

    public String getBackEndValue(InterestRateType interestRateType) {
        if (interestRateType instanceof MixedInterestRateType) {
            return "Dobanda mixta";
        } else if (interestRateType instanceof VariableInterestRateType) {
            return "Dobanda variabila";
        }

        throw new InternalServerException("No interestRateType found");
    }

    public void setInterestRateDetails(List<LoanInterestRate> loanInterestRates, InterestRateType interestRateType, InterestRateAdditionalInfo additionalInfo) {
        if (interestRateType instanceof MixedInterestRateType mixedInterestRateType) {
            LoanInterestRate fixedInterest = loanInterestRates.stream()
                    .filter(loanInterestRate -> mixedInterestRateType.getFixedPeriod() == loanInterestRate.getYear()
                            && "Dobanda fixa".equals(loanInterestRate.getInterestRateType()))
                    .findFirst().orElseThrow(() -> new InternalServerException("No rate found"));

            LoanInterestRate variableInterest = loanInterestRates.stream()
                    .filter(loanInterestRate -> mixedInterestRateType.getFixedPeriod() == loanInterestRate.getYear()
                            && "Dobanda variabila".equals(loanInterestRate.getInterestRateType()))
                    .findFirst().orElseThrow(() -> new InternalServerException("No rate found"));

            additionalInfo.setBankMarginRate(fixedInterest.getMargin());
            additionalInfo.setDefaultBankMarginRate(fixedInterest.getMargin());
            additionalInfo.setInterestRate(fixedInterest.getInterestRate());
            additionalInfo.setDefaultInterestRate(fixedInterest.getInterestRate());
            additionalInfo.setDefaultVariableInterestAfterFixedInterest(variableInterest.getInterestRate());
            additionalInfo.setVariableInterestAfterFixedInterest(variableInterest.getInterestRate());
        } else {
            LoanInterestRate interestRate = loanInterestRates.stream()
                    .filter(loanInterestRate -> "Dobanda variabila".equals(loanInterestRate.getInterestRateType()))
                    .findFirst().orElseThrow(() -> new InternalServerException("No rate found"));

            additionalInfo.setBankMarginRate(interestRate.getMargin());
            additionalInfo.setDefaultBankMarginRate(interestRate.getMargin());
            additionalInfo.setInterestRate(interestRate.getInterestRate());
            additionalInfo.setDefaultInterestRate(interestRate.getInterestRate());
        }
    }

    public BigDecimal calculateBuildingInsurancePremiumRate(String currency, Amount loanAmount, BigDecimal analysisCommission, Integer ltv, BigDecimal buildingInsurancePremiumRate) {
        ExchangeRate exchangeRate = fxClientRetrieve.getExchangeRates(CURRENCY_EUR).stream()
                .filter(rate -> CURRENCY_PAIR.equals(rate.getCurrencyPair()))
                .findFirst()
                .orElseThrow(() -> new InternalServerException("No exchange rate found"));
        BigDecimal exchangeRateValue = new BigDecimal(exchangeRate.getReferenceRate());
        BigDecimal totalAmount = getAmountWithAnalysisCommission(loanAmount.getAmount(), analysisCommission);

        BigDecimal estimatedBuildingValueLtv = totalAmount.divide(BigDecimal.valueOf(ltv * 0.01), RoundingMode.HALF_UP);
        BigDecimal estimatedBuildingValue = CURRENCY_RON.equals(currency) ?
                estimatedBuildingValueLtv
                :
                estimatedBuildingValueLtv.multiply(exchangeRateValue);
        return estimatedBuildingValue.multiply(buildingInsurancePremiumRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_DOWN))
                .setScale(2, RoundingMode.HALF_DOWN);
    }

    public BigDecimal getAmountWithAnalysisCommission(BigDecimal loanAmount, BigDecimal analysisCommission) {
        return loanAmount.add(analysisCommission).setScale(2, RoundingMode.HALF_DOWN);
    }

    public AdditionalCalculationInfo retrieveAdditionalInfo(MortgageCalculationRequest request) {
        String currency = request.getLoanAmount() != null ? request.getLoanAmount().getCurrency() : "RON";

        LoanProduct loanProduct = retrieveLoanProduct(request.getProductCode());
        LoanAllParameters loanAllParameters = retrieveClient.getLoanAllParametersByMultipleArguments(loanProduct.getIdLoan(),
                request.getSpecialOfferRequirements().isHasSalaryInTheBank(),
                currency,
                getBackEndValue(request.getInterestRateType()), false);
        List<NomenclatureDistrict> districts = retrieveClient.getDistricts();
        Integer zone = districts.stream()
                .filter(district -> district.getCity().equals(request.getArea().getCity()) && district.getCounty().equals(request.getArea().getCounty()))
                .findFirst().orElseThrow().getZone();

        Integer ltv = retrieveClient.getLtvByAreaOwnerAndSum(request.getLoanAmount().getAmount().doubleValue(), request.isOwner(), zone, loanProduct.getIdLoan());

        AdditionalCalculationInfo additionalInfo = calculatorMapper.buildAllAdditionalInfo(loanAllParameters, ltv);

        additionalInfo.setCurrency(currency);
        additionalInfo.setProductId(loanProduct.getIdLoan());

        return additionalInfo;
    }

    public LoanProduct retrieveLoanProduct(String productCode) {
        return retrieveClient.getLoanProduct(productCode);
    }


    public InterestRateAdditionalInfo retrieveInterestRate(MortgageCalculationRequest request, Integer productId) {
        InterestRateAdditionalInfo additionalInfo = InterestRateAdditionalInfo.builder()
                .build();
        List<LoanInterestRate> loanInterestRates = retrieveClient
                .getAllLoanInterestRatesByLoanProduct(productId, false, false); // merg cu false pentru a prelua standardul
        setInterestRateDetails(loanInterestRates, request.getInterestRateType(), additionalInfo);

        additionalInfo.setYearsWithFixedInterest(request.getInterestRateType() instanceof MixedInterestRateType interestRateType ?
                interestRateType.getFixedPeriod() * 12 : 0);
        additionalInfo.setDiscounts(retrieveClient.getDiscounts(productId));

        return additionalInfo;
    }

    public BigDecimal calculateCreditAmount(BigDecimal amount, Integer ltv) {
        return amount.multiply(new BigDecimal(ltv * 0.01));
    }

    public RepaymentPlanEntry createRepaymentPlanEntry(int currentMonth, MortgageCalculationRequest loanCalculationRequest,
                                                       AdditionalCalculationInfo additionalInfo, InterestRateAdditionalInfo rateAdditionalInfo) {
        boolean isDecreasing = Objects.equals(loanCalculationRequest.getInstallmentType().getValue(), InstallmentType.DECREASING_INSTALLMENTS.toString());
        RepaymentPlanEntry repaymentPlanEntry = new RepaymentPlanEntry();

        repaymentPlanEntry.setMonth(currentMonth);

        repaymentPlanEntry.setReimbursedCapitalAmount(
                new Amount(additionalInfo.getCurrency(),
                        isDecreasing ?
                                calculatePrincipalForDecreasingRates(currentMonth, loanCalculationRequest.getTenor(), loanCalculationRequest.getLoanAmount().getAmount(), additionalInfo)
                                        .setScale(2, RoundingMode.HALF_DOWN)
                                :
                                calculatePrincipal(currentMonth, loanCalculationRequest, additionalInfo, rateAdditionalInfo)
                                        .setScale(2, RoundingMode.HALF_DOWN)));

        repaymentPlanEntry.setRemainingLoanAmount(
                new Amount(additionalInfo.getCurrency(),
                        calculateBalance(currentMonth, loanCalculationRequest.getLoanAmount().getAmount(), additionalInfo)
                                .setScale(2, RoundingMode.HALF_DOWN)));

        repaymentPlanEntry.setInterestAmount(
                new Amount(additionalInfo.getCurrency(),
                        calculateInterest(currentMonth, additionalInfo, rateAdditionalInfo)
                                .setScale(2, RoundingMode.HALF_UP)));

        repaymentPlanEntry.setFeeAmount(
                new Amount(additionalInfo.getCurrency(),
                        calculateFeeAmount(currentMonth, additionalInfo, loanCalculationRequest)
                                .setScale(2, RoundingMode.HALF_DOWN)));

        BigDecimal installmentAmount = calculateInstallmentAmount(currentMonth,
                additionalInfo.getInterest(),
                additionalInfo.getPrincipal());

        repaymentPlanEntry.setInstallmentAmount(
                new Amount(additionalInfo.getCurrency(),
                        installmentAmount.setScale(2, RoundingMode.HALF_UP)));

        repaymentPlanEntry.setTotalPaymentAmount(
                new Amount(additionalInfo.getCurrency(),
                        currentMonth == 0 ?
                                additionalInfo.getAnalysisCommission()
                                :
                                calculateTotalPaymentAmount(loanCalculationRequest,
                                        additionalInfo,
                                        additionalInfo.getFee(),
                                        installmentAmount,
                                        currentMonth)
                                        .setScale(2, RoundingMode.HALF_DOWN)));

        return repaymentPlanEntry;
    }

    public BigDecimal calculateTotalPaymentAmount(MortgageCalculationRequest request,
                                                  AdditionalCalculationInfo additionalInfo,
                                                  BigDecimal feeAmount,
                                                  BigDecimal monthlyRateWithoutCommission,
                                                  int currentMonth) {
        BigDecimal totalPayment = monthlyRateWithoutCommission.add(feeAmount);

        BigDecimal insurance = calculateLifeInsurance(request, currentMonth, additionalInfo.getLifeInsurance(), additionalInfo.getAnalysisCommission());
        additionalInfo.setMonthlyLifeInsurance(
                new LifeInsurance(
                        new Amount(additionalInfo.getCurrency(), insurance.setScale(2, RoundingMode.HALF_DOWN)), Frequency.MONTHLY));

        if (request.isHasInsurance()) {
            totalPayment = totalPayment.add(insurance);
        }

        return totalPayment;
    }

    private BigDecimal calculateLifeInsurance(MortgageCalculationRequest request, int currentMonth, BigDecimal lifeInsurance, BigDecimal analysisCommission) {
        return currentMonth > request.getTenor() || lifeInsurance == null ?
                BigDecimal.ZERO
                :
                lifeInsurance
                        .divide(new BigDecimal(100), 6, RoundingMode.HALF_DOWN)
                        .multiply(getAmountWithAnalysisCommission(request.getLoanAmount().getAmount(), analysisCommission));
    }

    public BigDecimal calculateInstallmentAmount(int currentMonth, BigDecimal interest, BigDecimal principal) {
        return currentMonth == 0 ? BigDecimal.ZERO : interest.add(principal);
    }

    public BigDecimal calculateFeeAmount(int currentMonth,
                                         AdditionalCalculationInfo additionalInfo,
                                         MortgageCalculationRequest loanCalculationRequest) {
        BigDecimal fee;
        boolean shouldAddCommission = Math.floor((currentMonth - 1.0) / 12) == ((currentMonth - 1.0) / 12);

        if (currentMonth == 0) {
            fee = additionalInfo.getAnalysisCommission();
        } else {
            fee = currentMonth > loanCalculationRequest.getTenor() ?
                    BigDecimal.ZERO
                    :
                    additionalInfo.getMonthlyCurrentAccountCommission()
                            .add(shouldAddCommission && currentMonth != 1 ?
                                    getBuildingInsurance(additionalInfo.getBuildingPADInsurancePremiumRate(), additionalInfo.getBuildingInsurancePremiumRate())
                                    :
                                    BigDecimal.ZERO);
        }
        additionalInfo.setFee(fee);

        return fee;
    }

    private BigDecimal getBuildingInsurance(BigDecimal propertyPADInsurancePremiumRate, BigDecimal propertyInsurancePremiumRate) {
        return propertyPADInsurancePremiumRate.add(propertyInsurancePremiumRate);
    }

    public BigDecimal calculateInterest(int currentMonth, AdditionalCalculationInfo additionalInfo, InterestRateAdditionalInfo rateAdditionalInfo) {
        BigDecimal interest = currentMonth == 0 ?
                BigDecimal.ZERO
                :
                currentMonth <= rateAdditionalInfo.getYearsWithFixedInterest() ?
                        additionalInfo.getPreviousBalance()
                                .multiply(BigDecimal.valueOf(rateAdditionalInfo.getInterestRate()))
                                .divide(BigDecimal.valueOf(12 * 100), RoundingMode.HALF_DOWN)
                        :
                        additionalInfo.getPreviousBalance()
                                .multiply(rateAdditionalInfo.getYearsWithFixedInterest() > 0 ?
                                        BigDecimal.valueOf(rateAdditionalInfo.getVariableInterestAfterFixedInterest()) : BigDecimal.valueOf(rateAdditionalInfo.getInterestRate()))
                                .divide(BigDecimal.valueOf(12 * 100), RoundingMode.HALF_DOWN);

        additionalInfo.setInterest(interest);

        return interest;
    }

    public BigDecimal calculateBalance(int currentMonth, BigDecimal amount, AdditionalCalculationInfo additionalInfo) {
        BigDecimal totalAmount = getAmountWithAnalysisCommission(amount, additionalInfo.getAnalysisCommission());
        additionalInfo.setPreviousBalance(currentMonth == 1 ? totalAmount : additionalInfo.getBalance());

        BigDecimal balance = currentMonth == 0 ?
                totalAmount
                :
                additionalInfo.getBalance().subtract(additionalInfo.getPrincipal());
        additionalInfo.setBalance(balance);

        return balance;
    }


    public BigDecimal calculatePrincipalForDecreasingRates(int currentMonth, int tenor, BigDecimal amount, AdditionalCalculationInfo additionalInfo) {
        BigDecimal principal = currentMonth == 0 || currentMonth > tenor ?
                BigDecimal.ZERO
                :
                getAmountWithAnalysisCommission(amount, additionalInfo.getAnalysisCommission()).divide(new BigDecimal(tenor), 10, RoundingMode.UP);
        additionalInfo.setPrincipal(principal);

        return principal;
    }

    public BigDecimal calculatePrincipal(int currentMonth, MortgageCalculationRequest request,
                                         AdditionalCalculationInfo additionalInfo, InterestRateAdditionalInfo rateAdditionalInfo) {
        BigDecimal amount = getAmountWithAnalysisCommission(request.getLoanAmount().getAmount(), additionalInfo.getAnalysisCommission());
        BigDecimal principal = currentMonth <= rateAdditionalInfo.getYearsWithFixedInterest() ?
                calculatePrincipal(currentMonth, request.getTenor(),
                        amount,
                        BigDecimal.valueOf(rateAdditionalInfo.getInterestRate()))
                        .negate()
                :
                currentMonth > request.getTenor() ?
                        BigDecimal.ZERO
                        :
                        calculatePrincipal(currentMonth - rateAdditionalInfo.getYearsWithFixedInterest(),
                                request.getTenor() - rateAdditionalInfo.getYearsWithFixedInterest(),
                                calculateReferenceBalance(currentMonth, amount, additionalInfo, rateAdditionalInfo.getYearsWithFixedInterest()),
                                rateAdditionalInfo.getYearsWithFixedInterest() > 0 ?
                                        BigDecimal.valueOf(rateAdditionalInfo.getVariableInterestAfterFixedInterest()) : BigDecimal.valueOf(rateAdditionalInfo.getInterestRate()))
                                .negate();

        additionalInfo.setPrincipal(principal);

        return principal;
    }

    private BigDecimal calculateReferenceBalance(int currentMonth, BigDecimal amount, AdditionalCalculationInfo additionalInfo, Integer yearsWithFixedInterest) {
        if (currentMonth == yearsWithFixedInterest + 1) {
            additionalInfo.setReferenceBalance(additionalInfo.getBalance());
        }

        return additionalInfo.getReferenceBalance() != null ? additionalInfo.getReferenceBalance() : amount;
    }

    private BigDecimal calculatePrincipal(int currentMonth, int period, BigDecimal amount, BigDecimal interestRate) {
        return currentMonth == 0 || currentMonth > period ? BigDecimal.ZERO : calculatePPMT(currentMonth, period, interestRate, amount);
    }

    public static BigDecimal calculatePPMT(int currentMonth, int period, BigDecimal rate, BigDecimal amount) {
        double interestRate = rate.divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.UP).doubleValue();
        double amountDouble = amount.doubleValue();

        return BigDecimal.valueOf(Finance.ppmt(interestRate, currentMonth, period, amountDouble));
    }

    public MonthlyInstallment calculateMonthlyInstallment(boolean hasInsurance, List<RepaymentPlanEntry> repaymentPlanEntries, InterestRateType rateType, BigDecimal lifeInsurance) {
        BigDecimal installment = repaymentPlanEntries
                .get(1)
                .getTotalPaymentAmount()
                .getAmount();

        if (rateType instanceof MixedInterestRateType mixedInterestRateType && repaymentPlanEntries.size() > 37) {
            BigDecimal variableInstallment = repaymentPlanEntries
                    .get(mixedInterestRateType.getFixedPeriod() * 12 + 2)
                    .getTotalPaymentAmount()
                    .getAmount();

            return hasInsurance ?
                    new MonthlyInstallment(
                            installment.subtract(lifeInsurance).setScale(2, RoundingMode.DOWN),
                            variableInstallment.subtract(lifeInsurance).setScale(2, RoundingMode.DOWN))
                    :
                    new MonthlyInstallment(
                            installment.setScale(2, RoundingMode.DOWN),
                            variableInstallment.setScale(2, RoundingMode.DOWN));
        }
        return hasInsurance ?
                new MonthlyInstallment(BigDecimal.ZERO, installment.subtract(lifeInsurance).setScale(2, RoundingMode.DOWN))
                :
                new MonthlyInstallment(BigDecimal.ZERO, installment.setScale(2, RoundingMode.DOWN));
    }

    public BigDecimal calculateDAE(List<BigDecimal> cashFlows, BigDecimal amount, AdditionalCalculationInfo additionalInfo) {
        double[] totalPaymentForIrr = new double[cashFlows.size()];

        for (int i = 0; i < cashFlows.size(); i++) {
            if (i == 0) {
                totalPaymentForIrr[i] = getFirstCashFlow(cashFlows.get(i), amount, additionalInfo);
            } else {
                totalPaymentForIrr[i] = cashFlows.get(i).doubleValue();
            }
        }

        BigDecimal dae = BigDecimal.ONE
                .add(BigDecimal.valueOf(Irr.irr(totalPaymentForIrr, 0)))
                .pow(12).subtract(BigDecimal.ONE);
        dae = dae.multiply(BigDecimal.valueOf(100D)).setScale(2, RoundingMode.HALF_DOWN);

        return dae;
    }

    private double getFirstCashFlow(BigDecimal totalPaymentAmount, BigDecimal amount,
                                    AdditionalCalculationInfo additionalInfo) {

        return totalPaymentAmount
                .subtract(amount)
                .add(additionalInfo.getFeeCommission())
                .add(additionalInfo.getPaymentOrderCommission())
                .add(additionalInfo.getBuildingInsurancePremiumRate())
                .add(additionalInfo.getBuildingPADInsurancePremiumRate())
                .add(additionalInfo.getPostGrantCommission()).doubleValue();
    }

    public Amount calculateTotalPayment(List<BigDecimal> cashFlows, AdditionalCalculationInfo additionalInfo) {
        BigDecimal totalPayment = cashFlows.stream().skip(1).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Amount(additionalInfo.getCurrency(),
                totalPayment.add(additionalInfo.getFeeCommission())
                        .add(additionalInfo.getPaymentOrderCommission())
                        .add(additionalInfo.getBuildingInsurancePremiumRate())
                        .add(additionalInfo.getBuildingPADInsurancePremiumRate())
                        .add(additionalInfo.getPostGrantCommission())
                        .setScale(0, RoundingMode.DOWN));
    }
}

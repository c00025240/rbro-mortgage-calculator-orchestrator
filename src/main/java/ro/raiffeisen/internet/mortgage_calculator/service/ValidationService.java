package ro.raiffeisen.internet.mortgage_calculator.service;

import org.springframework.stereotype.Service;
import ro.raiffeisen.internet.mortgage_calculator.exception.BadRequestException;
import ro.raiffeisen.internet.mortgage_calculator.model.MixedInterestRateType;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;

@Service
public class ValidationService {
    public void validateRequest(MortgageCalculationRequest request) {
        if (request.getProductCode() == null || request.getProductCode().isBlank())
            throw new BadRequestException("ProductCode should not be null or empty");

        if (request.getLoanAmount() != null && request.getLoanAmount().getAmount() == null)
            throw new BadRequestException("Amount should not be null or empty");

        if (request.getLoanAmount() != null && (request.getLoanAmount().getCurrency() == null || request.getLoanAmount().getCurrency().isBlank()))
            throw new BadRequestException("Currency should not be null or empty");

        if (request.getArea() == null)
            throw new BadRequestException("Area should not be null");

        if (request.getArea().getCity() == null || request.getArea().getCity().isBlank())
            throw new BadRequestException("City should not be null or empty");

        if (request.getArea().getCounty() == null || request.getArea().getCounty().isBlank())
            throw new BadRequestException("County should not be null or empty");

        if (request.getIncome() == null)
            throw new BadRequestException("Income should not be null");

        if (request.getIncome().getCurrentIncome() == null)
            throw new BadRequestException("CurrentIncome should not be null");

        if (request.getIncome().getOtherInstallments() == null)
            throw new BadRequestException("OtherInstallments should not be null");

        if (request.getAge() == 0)
            throw new BadRequestException("Age is required to proceed");

        if (request.getInterestRateType() == null)
            throw new BadRequestException("InterestRateType should not be null or empty");

        if (request.getInterestRateType() instanceof MixedInterestRateType mixedInterestRateType) {
            if (mixedInterestRateType.getFixedPeriod() == 0)
                throw new BadRequestException("Fixed period is required to proceed");
        }

        if (request.getInstallmentType() == null)
            throw new BadRequestException("InstallmentType should not be null or empty");

        if (request.getSpecialOfferRequirements() == null) {
            throw new BadRequestException("SpecialOfferRequirements should not be null or empty");
        }
    }
}

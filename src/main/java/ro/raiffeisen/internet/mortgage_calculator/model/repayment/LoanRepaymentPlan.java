package ro.raiffeisen.internet.mortgage_calculator.model.repayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class LoanRepaymentPlan {

    @JsonProperty
    @Schema(description = "An array containing repayment plan entries.", required = true)
    private List<RepaymentPlanEntry> repaymentPlan;
}

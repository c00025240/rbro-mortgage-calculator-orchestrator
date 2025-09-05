package ro.raiffeisen.internet.mortgage_calculator.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoanInterestRate {

  @JsonProperty private Integer idInterestRate;

  @JsonProperty private float interestRate;

  @JsonProperty private String legalForm;

  @JsonProperty private Boolean ourClient;

  @JsonProperty private float minimumSum;

  @JsonProperty private float maximumSum;

  @JsonProperty private String interestRateType;

  @JsonProperty private float margin;
  
  @JsonProperty private Integer year;

  @JsonProperty private Integer fkLoanProduct;
}

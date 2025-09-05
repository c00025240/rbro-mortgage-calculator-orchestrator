package ro.raiffeisen.internet.mortgage_calculator.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExchangeRate {

    @JsonProperty private String currencyPair;
    @JsonProperty private String currency;
    @JsonProperty private String type;
    @JsonProperty private BigDecimal buyRate;
    @JsonProperty private BigDecimal sellRate;
    @JsonProperty private BigDecimal middleRate;
    @JsonProperty private BigDecimal parity;
    @JsonProperty private String validityDate;
    @JsonProperty private String referenceRate;
}

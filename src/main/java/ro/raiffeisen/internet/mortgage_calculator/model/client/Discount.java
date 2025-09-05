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
public class Discount {

    @JsonProperty
    private Integer id;

    @JsonProperty
    private String discountName;

    @JsonProperty
    private String discountLabel;

    @JsonProperty
    private String discountComment;

    @JsonProperty
    private Double discountValue;

    @JsonProperty
    private Integer idLoan;
}

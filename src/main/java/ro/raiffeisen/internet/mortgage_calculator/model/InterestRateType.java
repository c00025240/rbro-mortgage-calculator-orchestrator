package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = VariableInterestRateType.class, name = "VARIABLE"),
        @JsonSubTypes.Type(value = MixedInterestRateType.class, name = "MIXED"),
})
@Schema(
        name = "InterestRateType",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(
                        value = "VARIABLE",
                        schema = VariableInterestRateType.class),
                @DiscriminatorMapping(
                        value = "MIXED",
                        schema = MixedInterestRateType.class),
        }
)
public interface InterestRateType {

}

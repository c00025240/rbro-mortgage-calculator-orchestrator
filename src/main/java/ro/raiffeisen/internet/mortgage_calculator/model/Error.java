package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {
  @Schema(
      example = "c052421c-17ab-457f-9be6-5cbce2724094",
      description = "A unique UUID to identify a specific error.")
  private String errorId;

  @NotNull
  @Schema(
      example = "06f31981-c15d-48fb-86c6-53bfae940802",
      required = true,
      description =
          "A unique UUID of a specific request. A value shoud be obtained from a X-Request-Id header.")
  private String requestId;

  @NotNull
  @Schema(
      example = "2725ab36-4608-4abc-ba1c-6e929ef539b1",
      required = true,
      description =
          "A unique UUID for batch requests. A value shoud be obtained from a X-Correlation-Id header.")
  private String correlationId;

  @NotNull
  @Schema(
      example = "400",
      required = true,
      description =
          """
          A HTTP status code. If there is in use different protocol than HTTP, we should map an error to this protocol.
         """)
  private Integer status;

  @NotNull
  @Schema(
      example = "400",
      required = true,
      description =
          """
          A value that must be displayed to the user.
         """)
  private BigDecimal displayedValue;

  private List<ErrorReason> reasons;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Error error = (Error) o;
    return Objects.equals(this.errorId, error.errorId)
        && Objects.equals(this.requestId, error.requestId)
        && Objects.equals(this.correlationId, error.correlationId)
        && Objects.equals(this.status, error.status)
        && Objects.equals(this.reasons, error.reasons);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorId, requestId, correlationId, status, reasons);
  }
}

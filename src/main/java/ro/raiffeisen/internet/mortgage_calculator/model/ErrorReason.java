package ro.raiffeisen.internet.mortgage_calculator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Schema(description = "An additional information about caused error.")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorReason {
  @NotNull
  @Schema(
      example = "COMMON_BAD_REQUEST",
      required = true,
      description =
          """
            This is the standardized error entry that can be translated by the frontends to a custom error message or used
             by support to track the origination of a bug. List of common error codes:
            - **COMMON_INVALID_PARAMETER** An invalid parameter or parameter value was supplied in the request.
            - **COMMON_MISSING_PARAMETER** The API request is missing a required parameter.
            - **COMMON_BAD_REQUEST** The request query was invalid. Check the documentation to ensure that the supplied
             parameters are supported, and check if the request contains an invalid combination of parameters or an invalid
              value.
            - **COMMON_MISSING_CREDENTIALS** The user is not authorized to make the request.
            - **COMMON_INVALID_CREDENTIALS** The supplied authorization credentials for the request are invalid.
            - **COMMON_EXPIRED_ACCESS_TOKEN** The supplied access token has expired.
            - **COMMON_ACCESS_DENIED** The requested operation is forbidden.
            - **COMMON_NOT_FOUND** The requested resource could not be found.
            - **COMMON_METHOD_NOT_ALLOWED** The HTTP method for the request is not supported.
            - **COMMON_NOT_ACCEPTABLE** The server cannot produce a response matching the list of acceptable values defined
             in the header.
            - **COMMON_RESOURCE_ALREADY_EXISTS** The resource already exists.
            - **COMMON_UNSUPPORTED_MEDIA_TYPE** The server refuses to service the request because the payload is in an
             unsupported format.
            - **COMMON_RATE_LIMIT_EXCEEDED** Too many requests have been sent recently.
            - **COMMON_INTERNAL_ERROR** The request failed due to an internal error.
            - **COMMON_SERVICE_UNAVAILABLE** The requested service is temporarily unavailable.
            - **COMMON_GATEWAY_TIMEOUT** Request timed out during the execution.
          """)
  private String code;

  @Schema(
      example = "ERROR",
      description =
          "Shows if the reason for an unexpected situation is critical or just informative.")
  private SeverityType severity;

  @NotNull
  @Schema(
      example = "Payment rejected. Missing creditor iban.",
      required = true,
      description =
          """
          A human-readable message in a user-requested language defined by a Accept-Language header. Default native
          " NWU language used when a header is not specified. " +
          """)
  private String message;

  @Schema(
      example = "creditorAccount.iban",
      description = "The path of the problematic field which causes the error.")
  private String path;

  public enum SeverityType {
    ERROR("ERROR"),

    WARNING("WARNING"),

    INFO("INFO");

    private final String value;

    SeverityType(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static SeverityType fromValue(String value) {
      for (SeverityType b : SeverityType.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }
}

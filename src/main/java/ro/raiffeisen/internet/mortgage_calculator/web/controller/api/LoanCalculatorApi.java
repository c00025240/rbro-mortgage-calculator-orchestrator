package ro.raiffeisen.internet.mortgage_calculator.web.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationRequest;
import ro.raiffeisen.internet.mortgage_calculator.model.MortgageCalculationResponse;

import java.util.Optional;

import static ro.raiffeisen.internet.mortgage_calculator.model.StandardHttpHeaders.*;


@RequestMapping("/")
public interface LoanCalculatorApi {

    Logger log = LoggerFactory.getLogger(LoanCalculatorApi.class);

    default Optional<ObjectMapper> getObjectMapper() {
        return Optional.empty();
    }

    default Optional<String> getAcceptHeader() {
        return getRequest().map(r -> r.getHeader("Accept"));
    }

    default Optional<HttpServletRequest> getRequest() {
        return Optional.empty();
    }

    @Operation(
            summary = "This method is used to calculate loan's parameters for a customer.",
            operationId = "createCalculation",
            tags = {"post"}
    )
    @PostMapping(value = "/calculator/mortgage-calculator")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful operation",
                            content =
                            @Content(
                                    schema =
                                    @Schema(
                                            implementation =
                                                    MortgageCalculationResponse.class),
                                    mediaType = MediaType.APPLICATION_JSON_VALUE)),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request",
                            content =
                            @Content(
                                    schema = @Schema(implementation = Error.class),
                                    mediaType = MediaType.APPLICATION_JSON_VALUE)),
                    @ApiResponse(
                            responseCode = "500",
                            description =
                                    "Unexpected condition encountered which prevented fulfilling the request.",
                            content =
                            @Content(
                                    schema = @Schema(implementation = Error.class),
                                    mediaType = MediaType.APPLICATION_JSON_VALUE))
            }
    )
    default ResponseEntity<MortgageCalculationResponse> createCalculation(
            @Parameter(
                    name = X_REQUEST_ID,
                    description =
                            """
                                                                                     ID of the request, unique to the call, as determined by the initiating party used to track requests in logs. 
                                                                                     The server includes that ID in every log statement that it creates. If a client receives an error it can 
                                                                                     include the ID in a bug report, allowing the server operator to look up the corresponding log statements 
                                                                                     (without having to rely on timestamps, IPs, etc)
                                                                                     """,
                    example = "99391c7e-ad88-49ec-a2ad-99ddcb1f7721",
                    required = true)
            @NotNull
            @RequestHeader(name = X_REQUEST_ID)
            String requestId,
            @Parameter(
                    name = X_CORRELATION_ID,
                    description =
                            """
                                                                                     Will be used to track requests through all involved services to be able to graph request flows, 
                                                                                     to track all upstream calls from the business perspective, from the client to the upstream. 
                                                                                     (e.g. a customer fills out a form on 4 different pages, every GET/POST operation related to 
                                                                                     this process would have the same X-Correlation-ID header value). The X-Correlation-ID must 
                                                                                     be propagated unchanged in any and all upstream service calls (e.g. NWU services). 
                                                                                     RICE doesn't recommend sending a traceId (or spanId or any other B3 header 
                                                                                     https://github.com/openzipkin/b3-propagation) into X-Correlation-ID field, as this is not the intention.""",
                    example = "99391c7e-ad88-49ec-a2ad-99ddcb1f7721")
            @RequestHeader(name = X_CORRELATION_ID, required = false)
            String correlationId,
            @Parameter(
                    name = X_IDEMPOTENCY_KEY,
                    description =
                            """
                                                                                     A unique request identifier to support idempotency.
                                                                                     """,
                    example = "e457b5a2e4d86bd1198ee56343ba864fe8b2")
            @RequestHeader(name = X_IDEMPOTENCY_KEY, required = false)
            @Size(max = 40)
            String xIdempotencyKey,
            @Parameter(
                    name = ACCEPT_VERSION,
                    description =
                            """
                                                                                     Versioning is supported in API requests via the recommended, but optional.
                                                                                     Accept-Version: header. It allows API consumers to call the intended version of the API.
                                                                                     If no Accept-Version header is present, it implies that the newest version of the entity
                                                                                     should be retrieved.
                                                                                     More details about API versioning could be found on the RICE confluence page.
                                                                                     """,
                    example = "1.2")
            @NotBlank
            @RequestHeader(name = RICE_NWU_ID)
            @Size(max = 10)
            String riceNwuId,
            @Parameter(
                    name = DEVICE_SESSION_ID,
                    description =
                            """
                                Device session identifier assigned by implemented in RBI group solution for device security and
                                user behavior analytics.
                                """,
                    example = "HP3WR49WZsP8T5eboyHQ==_8I0gbpi3Mw/A1hAdYRZqRwAAcR0DCY2B")
            @RequestHeader(name = DEVICE_SESSION_ID, required = false)
            @Size(max = 128)
            String deviceSessionId,
            @Parameter(
                    name = DEVICE_SESSION_PROVIDER,
                    description =
                            """
                                The header paired with Device-Session-ID holding identifier of device session provider.
                                The identifier is used  when additional information connected with the session needs to
                                be retrieved from the provider.
                                """,
                    example = "Precognitive")
            @RequestHeader(name = DEVICE_SESSION_PROVIDER, required = false)
            @Size(max = 128)
            String deviceSessionProvider,
            @Validated @RequestBody
            MortgageCalculationRequest loanCalculationRequest) {
        if (!(getObjectMapper().isPresent() && getAcceptHeader().isPresent())) {
            log.warn("ObjectMapper or HttpServletRequest not configured in default Account Api interface so no example is generated");
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

//    @Operation(
//            summary = "Calculates a loan's repayment plan for a given use case.",
//            operationId = "/retail-loan-calculations/repayment-plans",
//            tags = {"post"}
//    )
//    @PostMapping(value = "/retail-loan-calculations/repayment-plans")
//    @ApiResponses(
//            value = {
//                    @ApiResponse(
//                            responseCode = "201",
//                            description = "Successful operation",
//                            content =
//                            @Content(
//                                    schema =
//                                    @Schema(
//                                            implementation =
//                                                    LoanRepaymentPlan.class),
//                                    mediaType = MediaType.APPLICATION_JSON_VALUE)),
//                    @ApiResponse(
//                            responseCode = "400",
//                            description = "Bad request",
//                            content =
//                            @Content(
//                                    schema = @Schema(implementation = Error.class),
//                                    mediaType = MediaType.APPLICATION_JSON_VALUE)),
//                    @ApiResponse(
//                            responseCode = "500",
//                            description =
//                                    "Unexpected condition encountered which prevented fulfilling the request.",
//                            content =
//                            @Content(
//                                    schema = @Schema(implementation = Error.class),
//                                    mediaType = MediaType.APPLICATION_JSON_VALUE))
//            }
//    )
//    default ResponseEntity<LoanRepaymentPlan> createRepaymentPlan(
//            @Parameter(
//                    name = X_REQUEST_ID,
//                    description =
//                            """
//                                                                                     ID of the request, unique to the call, as determined by the initiating party used to track requests in logs.
//                                                                                     The server includes that ID in every log statement that it creates. If a client receives an error it can
//                                                                                     include the ID in a bug report, allowing the server operator to look up the corresponding log statements
//                                                                                     (without having to rely on timestamps, IPs, etc)
//                                                                                     """,
//                    example = "99391c7e-ad88-49ec-a2ad-99ddcb1f7721",
//                    required = true)
//            @NotNull
//            @RequestHeader(name = X_REQUEST_ID)
//            String requestId,
//            @Parameter(
//                    name = X_CORRELATION_ID,
//                    description =
//                            """
//                                                                                     Will be used to track requests through all involved services to be able to graph request flows,
//                                                                                     to track all upstream calls from the business perspective, from the client to the upstream.
//                                                                                     (e.g. a customer fills out a form on 4 different pages, every GET/POST operation related to
//                                                                                     this process would have the same X-Correlation-ID header value). The X-Correlation-ID must
//                                                                                     be propagated unchanged in any and all upstream service calls (e.g. NWU services).
//                                                                                     RICE doesn't recommend sending a traceId (or spanId or any other B3 header
//                                                                                     https://github.com/openzipkin/b3-propagation) into X-Correlation-ID field, as this is not the intention.""",
//                    example = "99391c7e-ad88-49ec-a2ad-99ddcb1f7721")
//            @RequestHeader(name = X_CORRELATION_ID, required = false)
//            String correlationId,
//            @Parameter(
//                    name = X_IDEMPOTENCY_KEY,
//                    description =
//                            """
//                                                                                     A unique request identifier to support idempotency.
//                                                                                     """,
//                    example = "e457b5a2e4d86bd1198ee56343ba864fe8b2")
//            @RequestHeader(name = X_IDEMPOTENCY_KEY, required = false)
//            @Size(max = 40)
//            String xIdempotencyKey,
//            @Parameter(
//                    name = ACCEPT_VERSION,
//                    description =
//                            """
//                                                                                     Versioning is supported in API requests via the recommended, but optional.
//                                                                                     Accept-Version: header. It allows API consumers to call the intended version of the API.
//                                                                                     If no Accept-Version header is present, it implies that the newest version of the entity
//                                                                                     should be retrieved.
//                                                                                     More details about API versioning could be found on the RICE confluence page.
//                                                                                     """,
//                    example = "1.2")
//            @NotBlank
//            @RequestHeader(name = RICE_NWU_ID)
//            @Size(max = 10)
//            String riceNwuId,
//            @Parameter(
//                    name = DEVICE_SESSION_ID,
//                    description =
//                            """
//                                Device session identifier assigned by implemented in RBI group solution for device security and
//                                user behavior analytics.
//                                """,
//                    example = "HP3WR49WZsP8T5eboyHQ==_8I0gbpi3Mw/A1hAdYRZqRwAAcR0DCY2B")
//            @RequestHeader(name = DEVICE_SESSION_ID, required = false)
//            @Size(max = 128)
//            String deviceSessionId,
//            @Parameter(
//                    name = DEVICE_SESSION_PROVIDER,
//                    description =
//                            """
//                                The header paired with Device-Session-ID holding identifier of device session provider.
//                                The identifier is used  when additional information connected with the session needs to
//                                be retrieved from the provider.
//                                """,
//                    example = "Precognitive")
//            @RequestHeader(name = DEVICE_SESSION_PROVIDER, required = false)
//            @Size(max = 128)
//            String deviceSessionProvider,
//            @Validated @RequestBody
//            LoanCalculationRequest loanCalculationRequest) {
//        if (!(getObjectMapper().isPresent() && getAcceptHeader().isPresent())) {
//            log.warn("ObjectMapper or HttpServletRequest not configured in default Account Api interface so no example is generated");
//        }
//
//        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
//    }
}

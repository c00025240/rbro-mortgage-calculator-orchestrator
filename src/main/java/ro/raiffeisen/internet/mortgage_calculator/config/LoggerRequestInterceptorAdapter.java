package ro.raiffeisen.internet.mortgage_calculator.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static ro.raiffeisen.internet.mortgage_calculator.helper.MarkerFields.*;
import static ro.raiffeisen.internet.mortgage_calculator.model.StandardHttpHeaders.*;


@Component
public class LoggerRequestInterceptorAdapter implements HandlerInterceptor {

  private final Logger log = LoggerFactory.getLogger("AUDIT_REQUESTS");

  @Override
  public boolean preHandle(
      final HttpServletRequest request,
      @NonNull final HttpServletResponse response,
      @NonNull final Object handler) {

    LocalDateTime startTime = LocalDateTime.now();
    request.setAttribute("startTime", startTime);
    final String urlPath = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
    final String queryParams = request.getQueryString();

    MDC.put(CONTEXT_REQUEST_ID, request.getHeader(X_REQUEST_ID));

    if (null == request.getHeader(X_CORRELATION_ID)) {
      MDC.put(CONTEXT_TRACE_ID, UUID.randomUUID().toString());
    } else {
      MDC.put(CONTEXT_TRACE_ID, request.getHeader(X_CORRELATION_ID));
    }

    MDC.put(CONTEXT_CORRELATION_ID, request.getHeader(X_CORRELATION_ID));
    MDC.put(CONTEXT_SPAN_ID, UUID.randomUUID().toString());
    MDC.put(CONTEXT_PARENT_SPAN_ID, request.getHeader(X_CORRELATION_ID));
    MDC.put(CONTEXT_NWU_ID, request.getHeader(RICE_NWU_ID));
    MDC.put(CONTEXT_API_VERSION, request.getHeader(ACCEPT_VERSION));

    MDC.put(CONTEXT_REQUEST_URL, urlPath);

    if (request.getMethod().equals(HttpMethod.POST.name())) {
      MDC.put(CONTEXT_OPERATION, "create");
    } else if (request.getMethod().equals(HttpMethod.PUT.name())) {
      MDC.put(CONTEXT_OPERATION, "replace");
    } else if (request.getMethod().equals(HttpMethod.PATCH.name())) {
      MDC.put(CONTEXT_OPERATION, "update");
    } else if (request.getMethod().equals(HttpMethod.DELETE.name())) {
      MDC.put(CONTEXT_OPERATION, "delete");
    }

    MDC.put(CONTEXT_HTTP_METHOD, request.getMethod());
    if (queryParams != null) {
      MDC.put(CONTEXT_DATA, queryParams);
    }
    log.info("Request processing started...");
    MDC.remove("data");

    return true;
  }

  @Override
  public void afterCompletion(
      final HttpServletRequest request,
      final HttpServletResponse response,
      @NonNull final Object handler,
      @Nullable final Exception ex) {

    try {
      final LocalDateTime startTime = (LocalDateTime) request.getAttribute("startTime");
      MDC.put(
          CONTEXT_EXECUTION_TIME, ChronoUnit.MILLIS.between(startTime, LocalDateTime.now()) + "");
      MDC.put(CONTEXT_HTTP_STATUS, String.valueOf(response.getStatus()));
      log.info("Request processing ended.");
    } finally {
      MDC.clear();
    }
  }
  
}

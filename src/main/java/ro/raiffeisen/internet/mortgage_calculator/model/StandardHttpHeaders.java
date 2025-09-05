package ro.raiffeisen.internet.mortgage_calculator.model;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class StandardHttpHeaders {
  public static final String X_REQUEST_ID = "X-Request-ID";
  public static final String X_CORRELATION_ID = "X-Correlation-ID";
  public static final String X_IDEMPOTENCY_KEY = "X-Idempotency-Key";
  public static final String RICE_NWU_ID = "RICE-NWU-ID";
  public static final String ACCEPT_VERSION = "Accept-Version";
  public static final String DEVICE_SESSION_PROVIDER = "Device-Session-Provider";
  public static final String DEVICE_SESSION_ID = "Device-Session-ID";

  private StandardHttpHeaders() {
    throw new IllegalStateException("Utility class");
  }

  public static HttpHeaders getStandardResponseHeaders(String correlationId) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);

    if (null != correlationId) {
//      httpHeaders.add(X_CORRELATION_ID,  ESAPI.encoder().encodeForHTML(correlationId));
    }

    return httpHeaders;
  }
}

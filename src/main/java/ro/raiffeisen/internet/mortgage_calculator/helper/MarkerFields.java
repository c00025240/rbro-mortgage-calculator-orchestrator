package ro.raiffeisen.internet.mortgage_calculator.helper;

public final class MarkerFields {

    public static final String CONTEXT_EXECUTION_TIME = "execution_time";
    public static final String CONTEXT_REQUEST_ID = "request_id";
    public static final String CONTEXT_DATA = "data";
    public static final String CONTEXT_SPAN_ID = "span_id";
    public static final String CONTEXT_PARENT_SPAN_ID = "parent_span_id";
    public static final String CONTEXT_OPERATION = "operation";
    public static final String CONTEXT_CORRELATION_ID = "correlation_id";
    public static final String CONTEXT_NWU_ID = "nwu_id";
    public static final String CONTEXT_API_VERSION = "api_version";
    public static final String CONTEXT_TRACE_ID = "trace_id";

    public static final String CONTEXT_HTTP_STATUS = "http_status";
    public static final String CONTEXT_CLIENT_IP = "client_ip";
    public static final String CONTEXT_HTTP_METHOD = "http_method";
    public static final String CONTEXT_REQUEST_URL = "req_url";

    public static final String EXCEPTION = "exception";
    public static final String EXCEPTION_MESSAGE = "exception_message";

    private MarkerFields() {
        throw new IllegalStateException("Utility class");
    }
}

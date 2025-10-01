package ro.raiffeisen.internet.mortgage_calculator.helper;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component("mortgageRequestKeyGenerator")
public class MortgageRequestKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
        try {
            return method.getName() + "_" + String.join("_",
                    java.util.Arrays.stream(params)
                    .map(param -> param == null ? "null" : param.toString())
                    .toArray(String[]::new));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to generate cache key", e);
        }
    }
}

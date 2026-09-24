package com.mirae.elibrary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed binding for the {@code app.*} configuration in application.yml.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Cors cors;
    private final Jwt jwt;
    private final Loan loan;

    public AppProperties(Cors cors, Jwt jwt, Loan loan) {
        this.cors = cors;
        this.jwt = jwt;
        this.loan = loan;
    }

    public Cors getCors() {
        return cors;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Loan getLoan() {
        return loan;
    }

    public static class Cors {
        private final List<String> allowedOrigins;

        public Cors(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }
    }

    public static class Jwt {
        private final String secret;
        private final long expirationMs;

        public Jwt(String secret, long expirationMs) {
            this.secret = secret;
            this.expirationMs = expirationMs;
        }

        public String getSecret() {
            return secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }
    }

    public static class Loan {
        private final int maxActiveLoans;
        private final int periodDays;

        public Loan(int maxActiveLoans, int periodDays) {
            this.maxActiveLoans = maxActiveLoans;
            this.periodDays = periodDays;
        }

        public int getMaxActiveLoans() {
            return maxActiveLoans;
        }

        public int getPeriodDays() {
            return periodDays;
        }
    }
}

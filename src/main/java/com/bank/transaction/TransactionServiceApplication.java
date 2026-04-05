package com.bank.transaction;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class TransactionServiceApplication {

    public static void main(String[] args) {

        // Load .env file
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Set environment variables for Spring Boot
        setEnv("DB_URL", dotenv);
        setEnv("DB_USERNAME", dotenv);
        setEnv("DB_PASSWORD", dotenv);
        setEnv("MAIL_USERNAME", dotenv);
        setEnv("MAIL_PASSWORD", dotenv);
        setEnv("APP_BASE_URL", dotenv);

        SpringApplication.run(TransactionServiceApplication.class, args);
    }

    private static void setEnv(String key, Dotenv dotenv) {
        String value = dotenv.get(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}
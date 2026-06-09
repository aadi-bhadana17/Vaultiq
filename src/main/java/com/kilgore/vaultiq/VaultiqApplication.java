package com.kilgore.vaultiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VaultiqApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaultiqApplication.class, args);
    }
}

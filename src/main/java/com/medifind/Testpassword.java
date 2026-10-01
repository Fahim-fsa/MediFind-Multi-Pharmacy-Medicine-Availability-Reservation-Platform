package com.medifind;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Testpassword {

    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println(
                encoder.encode("Admin@12345")
        );

    }
}

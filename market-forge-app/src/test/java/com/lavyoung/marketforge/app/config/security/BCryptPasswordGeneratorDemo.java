package com.lavyoung.marketforge.app.config.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
public class BCryptPasswordGeneratorDemo {

    public static void main(String[] args) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
        System.out.println(passwordEncoder.encode("1111"));
    }
}

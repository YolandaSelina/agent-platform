package com.rk.agent.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptGen {
    public static void main(String[] args) {
        String pwd = args.length > 0 ? args[0] : "admin123";
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode(pwd);
        System.out.println(hash);
        System.out.println("verify: " + enc.matches(pwd, hash));
    }
}

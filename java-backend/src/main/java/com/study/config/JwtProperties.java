package com.study.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置,绑定 jwt.* 。
 *
 * @ConfigurationProperties 把 application.yml / 环境变量里的 jwt 前缀配置绑定到这个对象。
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expiresIn;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}

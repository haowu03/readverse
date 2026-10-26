package com.readverse.framework.security;

public interface TokenStore {

    void save(String jti, LoginUser user, long ttlSeconds);

    LoginUser get(String jti);

    void remove(String jti);
}

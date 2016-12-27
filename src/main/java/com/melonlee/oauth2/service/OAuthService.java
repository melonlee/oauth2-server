package com.melonlee.oauth2.service;

/**
 * Created by Melon on 16/12/22.
 */
public interface OAuthService {


    public void addAuthCode(String authCode, String username);

    public void addAccessToken(String accessToken, String username);

    boolean checkAuthCode(String authCode);

    boolean checkAccessToken(String accessToken);

    String getUsernameByAuthCode(String authCode);

    String getUsernameByAccessToken(String accessToken);

    //auth code .access token 过期时间
    long getExpireIn();

    public boolean checkClientId(String clientId);

    public boolean checkClientSecret(String clientSecret);

}

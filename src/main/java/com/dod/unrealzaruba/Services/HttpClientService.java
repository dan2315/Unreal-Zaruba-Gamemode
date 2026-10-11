package com.dod.unrealzaruba.Services;

import dod.unrealfrontiers.ufessential.Auth.AuthService;

import java.net.http.HttpClient;

public class HttpClientService {
    public static String baseUrl() {
        return AuthService.getBaseUrl();
    }
    private final HttpClient httpClient;

    public HttpClientService() {
        httpClient = HttpClient.newHttpClient();
    }

    public HttpClient get() {
        return httpClient;
    }
}

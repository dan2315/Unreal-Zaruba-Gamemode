package com.dod.unrealzaruba.Services;

import java.net.http.HttpClient;

public class HttpClientService {
    public static final String BASEURL = "https://unrealfrontiers.xyz";
    private final HttpClient httpClient;

    public HttpClientService() {
        httpClient = HttpClient.newHttpClient();
    }

    public HttpClient get() {
        return httpClient;
    }
}

package org.example.jobflow.worker.service;

import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;

@Service
public class UrlCheckService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public int check(String url) throws Exception {
        URI uri = URI.create(url);

        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTP and HTTPS URLs are allowed");
        }

        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Invalid URL");
        }

        InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());

        if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(this::isPrivateAddress)) {
            throw new IllegalArgumentException("URL points to a private or reserved address");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    private boolean isPrivateAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }
}
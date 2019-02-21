package com.github.jstumpp.uups;

import com.github.jstumpp.uups.server.Server;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.profiles.active=test"}, classes = {Server.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
public class IntegrationTestUsingRealServer {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    private RestTemplate getRestTemplate() throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplateBuilder().rootUri("http://localhost:" + port).requestFactory(requestFactory).build();
    }

    @Before
    public void setup() throws Exception {
        restTemplate = getRestTemplate();
    }

    @Test
    public void happyCase() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        Assert.assertThat(response.getStatusCode(), Matchers.is(HttpStatus.OK));
    }

    @Test
    public void exceptiontest() throws Exception {

        thrown.expect(HttpServerErrorException.class);
        thrown.expectMessage("500");

        ResponseEntity<String> response = restTemplate.getForEntity("/parseexception", String.class);

        System.out.println(response.getBody().toString());
        Assert.assertThat(response.getStatusCode(), Matchers.is(HttpStatus.OK));
    }
}

package com.onceforall.ecm.gateway;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.onceforall.ecm.config.SecurityBeanOverrideConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EcmApp.class, SecurityBeanOverrideConfiguration.class})
public class ${NAME}GatewayIntTest {


    @Autowired
    private ${NAME}Gateway gateway;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(9093));

    @Test
    public void shouldTestWhenStatusIsOk() {
        // given
        stubFor(get(urlEqualTo("/api/v1/path"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", String.valueOf(MediaType.APPLICATION_JSON))
                .withBody("")
            )
        );

        // when
        gateway.methodName();

        // then

        verify(getRequestedFor(urlPathEqualTo("/api/v1/path")));
    }
}

package com.github.jstumpp.uups;

import com.github.jstumpp.uups.server.Server;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.util.NestedServletException;

import static org.hamcrest.Matchers.containsString;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.profiles.active=test"}, classes = {Server.class})
@AutoConfigureMockMvc
public class IntegrationTestUsingMockMvc {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Autowired
    private MockMvc mvc;

    @Test
    public void noErrorEndpoint() throws Exception {
        mvc.perform(MockMvcRequestBuilders
                .get("/"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void exceptionEndpoint() throws Exception {
        exception.expect(NestedServletException.class);
        mvc.perform(MockMvcRequestBuilders
                .get("/parseexception")
                .contentType(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.content().string(containsString("Uups")));
    }
}

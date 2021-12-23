package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import service.controller.TikaServiceConfig;
import service.model.ServiceInformation;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.processor.CompositeTikaProcessorConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Implements general tests for the Service Controller
 * (no documents processing)
 */
@SpringBootTest(classes = TikaServiceApplication.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TikaServiceConfig.class, LegacyPdfProcessorConfig.class, CompositeTikaProcessorConfig.class})
public class ServiceControllerTests  {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceInformation serviceInformation;

    @Test
    public void testGetApplicationInfo() throws Exception {
        String INFO_ENDPOINT_URL = "/api/info";
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                .get(INFO_ENDPOINT_URL)
                .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        // check response status
        int status = result.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value(), status);

        // parse content
        ObjectMapper mapper = new ObjectMapper();
        ServiceInformation response = mapper.readValue(result.getResponse().getContentAsString(),
                ServiceInformation.class);

        // check example content
        assertEquals(response.getServiceConfig().getAppVersion(), serviceInformation.getServiceConfig().getAppVersion());
    }
}

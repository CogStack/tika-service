package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import service.controller.TikaServiceConfig;
import service.model.ServiceResponseContent;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.model.TikaProcessingResult;
import tika.processor.CompositeTikaProcessorConfig;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Implements document processing tests for the Service Controller
 * A document is passed as an ocet stream
 */
@SpringBootTest(classes = TikaServiceApplication.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TikaServiceConfig.class, LegacyPdfProcessorConfig.class, CompositeTikaProcessorConfig.class})
public class ServiceControllerDocumentStreamTests extends ServiceControllerDocumentTests  {

    @Autowired
    private MockMvc mockMvc;

    @Override
    protected TikaProcessingResult sendProcessingRequest(final String docPath, HttpStatus expectedStatus) throws Exception  {
        return sendFileProcessingRequest(docPath, expectedStatus);
    }

    private TikaProcessingResult sendFileProcessingRequest(final String docPath, HttpStatus expectedStatus) throws Exception  {
        InputStream stream = utils.getDocumentStream(docPath);

        byte[] content = stream.readAllBytes();

        String PROCESS_ENDPOINT_URL = "/api/process";
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(PROCESS_ENDPOINT_URL)
                .content(content))
                //.param("some-random", "4"))
                .andExpect(status().is(expectedStatus.value()))
                .andReturn();

        assertEquals(expectedStatus.value(), result.getResponse().getStatus());
        assertNotNull(result.getResponse().getContentAsString());

        // parse content
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        return mapper.readValue(result.getResponse().getContentAsString(),
                ServiceResponseContent.class).getResult();
    }
}

package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import service.controller.TikaServiceConfig;
import service.model.ServiceInformation;
import service.model.ServiceResponseContent;
import tika.DocumentTestUtils;
import tika.legacy.LegacyPdfProcessorConfig;
import tika.model.TikaProcessingResult;
import tika.processor.CompositeTikaProcessorConfig;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(classes = TikaServiceApplication.class)
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TikaServiceConfig.class, LegacyPdfProcessorConfig.class, CompositeTikaProcessorConfig.class})
@TestPropertySource(properties = {"spring.config.location = classpath:tika/config/tika-processor-config.yaml,classpath:application.properties"})
public class ServiceControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
    private ServiceInformation serviceinfo;

    final private String INFO_ENDPOINT_URL = "/api/info";
    final private String PROCESS_ENDPOINT_URL = "/api/process";
	final private String PROCESS_FILE_ENDPOINT_URL = "/api/process_file";

    DocumentTestUtils utils = new DocumentTestUtils();

	@Test
	public void testGetApplicationInfo() throws Exception {
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders
				.get(INFO_ENDPOINT_URL)
				.accept(MediaType.APPLICATION_JSON_UTF8))
				.andReturn();

		// check response status
		int status = result.getResponse().getStatus();
		assertEquals(HttpStatus.OK.value(), status);

		// parse content
		ObjectMapper mapper = new ObjectMapper();
        ServiceInformation response = mapper.readValue(result.getResponse().getContentAsString(),
                ServiceInformation.class);

        // check example content
        assertEquals(response.getServiceConfig().getAppVersion(), serviceinfo.getServiceConfig().getAppVersion());
    }

    @Test
    public void testProcessFileEx1Doc() throws Exception {
        //InputStream is = controller.getClass().getClassLoader().getResourceAsStream("excel.xlsx");

        final String docPathPrefix = "generic/pat_id_1";
        final String fullDocPath = docPathPrefix + ".doc";

        InputStream stream = utils.getDocumentStream(fullDocPath);

        MockMultipartFile multipartFile = new MockMultipartFile("file", fullDocPath, "multipart/form-data", stream);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart(PROCESS_FILE_ENDPOINT_URL)
                .file(multipartFile))
                //.param("some-random", "4"))
                .andExpect(status().is(200))
                .andReturn();
                //.andExpect(content().string("success"));

        assertEquals(200, result.getResponse().getStatus());
        assertNotNull(result.getResponse().getContentAsString());

        // parse content
        ObjectMapper mapper = new ObjectMapper();
        TikaProcessingResult tikaResult = mapper.readValue(result.getResponse().getContentAsString(),
                ServiceResponseContent.class).getResult();

        // check content
        assertTrue(tikaResult.getText().length() > 0);
    }


    @Test
    public void testProcessContentEx1Doc() throws Exception {
        //InputStream is = controller.getClass().getClassLoader().getResourceAsStream("excel.xlsx");

        final String docPathPrefix = "generic/pat_id_1";
        final String fullDocPath = docPathPrefix + ".doc";

        InputStream stream = utils.getDocumentStream(fullDocPath);

        byte[] content = stream.readAllBytes();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(PROCESS_ENDPOINT_URL)
                .content(content))
                //.param("some-random", "4"))
                .andExpect(status().is(200))
                .andReturn();
        //.andExpect(content().string("success"));

        assertEquals(200, result.getResponse().getStatus());
        assertNotNull(result.getResponse().getContentAsString());

        // parse content
        ObjectMapper mapper = new ObjectMapper();
        TikaProcessingResult tikaResult = mapper.readValue(result.getResponse().getContentAsString(),
                ServiceResponseContent.class).getResult();

        // check content
        assertTrue(tikaResult.getText().length() > 0);
    }

}

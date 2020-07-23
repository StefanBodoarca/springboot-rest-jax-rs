package apache.rs_service;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.ws.validation.rest.RestDocumentValidationServiceImpl;
import eu.europa.esig.dss.ws.validation.rest.client.RestDocumentValidationService;

import java.util.Arrays;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;

@Configuration
public class CXFConfig {
	public static final String REST_VALIDATION = "/rest/validation";
	
	@Autowired
	private RemoteDocumentValidationService remoteValidationService;
	
	 @Autowired
	 private Bus bus;
	
	@Bean
	public Server createServerValidationRestService() {
		JAXRSServerFactoryBean sfb = new JAXRSServerFactoryBean();
		sfb.setBus(bus);
		//sfb.setServiceBean(restValidationService());
		sfb.setServiceBeans(Arrays.<Object>asList(new CheckService(), restValidationService()));
		sfb.setAddress(REST_VALIDATION);
		sfb.setProvider(jacksonJsonProvider());
		sfb.setProvider(exceptionRestMapper());
		sfb.setFeatures(Arrays.asList(createOpenApiFeature()));
		return sfb.create();
	}
	
	@Bean
	public JacksonJsonProvider jacksonJsonProvider() {
		JacksonJsonProvider jsonProvider = new JacksonJsonProvider();
		jsonProvider.setMapper(objectMapper());
		return jsonProvider;
	}
	
	@Bean
    public OpenApiFeature createOpenApiFeature() {
        final OpenApiFeature openApiFeature = new OpenApiFeature();
		openApiFeature.setCustomizer(openApiCustomizer());
        openApiFeature.setPrettyPrint(true);
        openApiFeature.setScan(true);
		openApiFeature.setUseContextBasedConfig(true);
        openApiFeature.setTitle("DSS WebServices");
        return openApiFeature;
    }
	
	@Bean
	public OpenApiCustomizer openApiCustomizer() {
		OpenApiCustomizer customizer = new OpenApiCustomizer();
		customizer.setDynamicBasePath(true);
		return customizer;
	}
	
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		// true value allows to process {@code @IDREF}s cycles
		JaxbAnnotationIntrospector jai = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
		objectMapper.setAnnotationIntrospector(jai);
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		return objectMapper;
	}
	
	@Bean
	public RestDocumentValidationService restValidationService() {
		RestDocumentValidationServiceImpl service = new RestDocumentValidationServiceImpl();
		service.setValidationService(remoteValidationService);
		return service;
	}
	
	@Bean
	public ExceptionRestMapper exceptionRestMapper() {
		return new ExceptionRestMapper();
	}
}

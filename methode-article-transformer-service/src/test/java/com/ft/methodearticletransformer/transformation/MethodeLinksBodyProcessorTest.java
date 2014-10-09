package com.ft.methodearticletransformer.transformation;

import static com.ft.methodetesting.xml.XmlMatcher.identicalXmlTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.ft.jerseyhttpwrapper.ResilientClient;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.MessageBodyWorkers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ft.methodeapi.model.EomAssetType;
import com.ft.methodearticletransformer.methode.MethodeFileService;

import javax.ws.rs.core.MediaType;

@RunWith(MockitoJUnitRunner.class)
public class MethodeLinksBodyProcessorTest {
	
	@Mock
	private MethodeFileService methodeFileService;
	@Mock
	private ResilientClient semanticStoreContentReaderClient;

	@Mock
	private InBoundHeaders headers;
	@Mock
	private MessageBodyWorkers workers;
	@Mock
	private WebResource.Builder builder;

	private InputStream entity;

	@Before
	public void setup() {
		entity = new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8));
		WebResource webResource = mock(WebResource.class);
		when(semanticStoreContentReaderClient.resource(any(URI.class))).thenReturn(webResource);
		when(webResource.accept(any(MediaType[].class))).thenReturn(builder);
		when(builder.header(anyString(), anyObject())).thenReturn(builder);
		when(builder.get(ClientResponse.class)).thenReturn(clientResponseWithCode(404));
	}

	private MethodeLinksBodyProcessor bodyProcessor;
	
	private String uuid = UUID.randomUUID().toString();
	private static final String TRANSACTION_ID = "tid_test";

	@Test
	public void shouldReplaceNodeWhenItsALinkThatWillBeInTheContentStoreWhenAvailableInMethode(){
		Map<String, EomAssetType> assetTypes = new HashMap<>();
		assetTypes.put(uuid, new EomAssetType.Builder().type("EOM::CompoundStory").uuid(uuid).build());
		when(methodeFileService.assetTypes(anySet(), anyString())).thenReturn(assetTypes);
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		
		String body = "<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
        assertThat(processedBody, is(identicalXmlTo("<body><content id=\"" + uuid + "\" title=\"Some absurd text here\" type=\"" + MethodeLinksBodyProcessor.ARTICLE_TYPE + "\"> Link Text</content></body>")));
	}

	@Test
	public void shouldReplaceNodeWhenItsALinkThatWillBeInTheContentStoreWhenAvailableInContentStore(){
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		when(builder.get(ClientResponse.class)).thenReturn(clientResponseWithCode(200));

		String body = "<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
		assertThat(processedBody, is(identicalXmlTo("<body><content id=\"" + uuid + "\" title=\"Some absurd text here\" type=\"" + MethodeLinksBodyProcessor.ARTICLE_TYPE + "\"> Link Text</content></body>")));
	}

	@Test
	public void shouldNotReplaceNodeWhenItsALinkThatWillNotBeInTheContentStore(){
		Map<String, EomAssetType> assetTypes = new HashMap<>();
		assetTypes.put(uuid, new EomAssetType.Builder().type("Slideshow").uuid(uuid).build());
		when(methodeFileService.assetTypes(anySet(), anyString())).thenReturn(assetTypes);
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		
		String body = "<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
        assertThat(processedBody, is(identicalXmlTo("<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>")));
	}
	
	@Test
	public void shouldStripIntlFromHrefValueWhenItsNotAValidInternalLink(){
		Map<String, EomAssetType> assetTypes = new HashMap<>();
		assetTypes.put(uuid, new EomAssetType.Builder().type("Slideshow").uuid(uuid).build());
		when(methodeFileService.assetTypes(anySet(), anyString())).thenReturn(assetTypes);
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		
		String body = "<body><a href=\"http://www.ft.com/intl/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
        assertThat(processedBody, is(identicalXmlTo("<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>")));
	}
	
	@Test
	public void shouldStripParamFromHrefValueWhenItsNotAValidInternalLink(){
		Map<String, EomAssetType> assetTypes = new HashMap<>();
		assetTypes.put(uuid, new EomAssetType.Builder().type("Slideshow").uuid(uuid).build());
		when(methodeFileService.assetTypes(anySet(), anyString())).thenReturn(assetTypes);
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		
		String body = "<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html?param=5\" title=\"Some absurd text here\"> Link Text</a></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
        assertThat(processedBody, is(identicalXmlTo("<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>")));
	}
	
	@Test
	public void shouldRemoveNodeIfATagHasNoHrefAttributeForNonInternalLinks() {
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		
		String body = "<body><a title=\"Some absurd text here\">Link Text</a></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
        assertThat(processedBody, is(identicalXmlTo("<body>Link Text</body>")));
	}
	
	@Test
	public void shouldRemoveNodeIfATagHasNoHrefAttributeForNonInternalLinksEvenIfNodeEmpty() {
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		
		String body = "<body><a title=\"Some absurd text here\"/></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
        assertThat(processedBody, is(identicalXmlTo("<body></body>")));
	}
	
	@Test
	public void shouldConvertNonArticlePathBasedInternalLinksToFullFledgedWebsiteLinks(){
		Map<String, EomAssetType> assetTypes = new HashMap<>();
		assetTypes.put(uuid, new EomAssetType.Builder().type("EOM::MediaGallery").uuid(uuid).build());
		when(methodeFileService.assetTypes(anySet(), anyString())).thenReturn(assetTypes);
		bodyProcessor = new MethodeLinksBodyProcessor(methodeFileService, semanticStoreContentReaderClient);
		
		String body = "<body><a href=\"/FT Production/Slideshows/gallery.xml;uuid=" + uuid + "\" title=\"Some absurd text here\"> Link Text</a></body>";
		String processedBody = bodyProcessor.process(body, new DefaultTransactionIdBodyProcessingContext(TRANSACTION_ID));
        assertThat(processedBody, is(identicalXmlTo("<body><a href=\"http://www.ft.com/cms/s/" + uuid + ".html\" title=\"Some absurd text here\"> Link Text</a></body>")));
	}

	private ClientResponse clientResponseWithCode(int status) {
		return new ClientResponse(status, headers, entity, workers);
	}
}
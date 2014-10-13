package com.ft.methodearticletransformer.resources;

import com.ft.api.jaxrs.errors.ErrorEntity;
import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.methode.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodeTransformerResourceTest {

	private static final String TRANSACTION_ID = "tid_test";

	private MethodeTransformerResource methodeTransformerResource;
	private MethodeFileService methodeFileService;
	private HttpHeaders httpHeaders;
	private EomFileProcessorForContentStore eomFileProcessorForContentStore;

	@Before
	public void setup() {
		methodeFileService = mock(MethodeFileService.class);
		eomFileProcessorForContentStore = mock(EomFileProcessorForContentStore.class);

		methodeTransformerResource = new MethodeTransformerResource(methodeFileService, eomFileProcessorForContentStore);

		httpHeaders = mock(HttpHeaders.class);
		when(httpHeaders.getRequestHeader(TransactionIdUtils.TRANSACTION_ID_HEADER)).thenReturn(Arrays.asList(TRANSACTION_ID));
	}

	@Test
	public void shouldReturn200WhenProcessedOk() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		Content content = Content.builder().build();
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)). thenReturn(content);

		assertThat(methodeTransformerResource.getByUuid(randomUuid.toString(), httpHeaders), equalTo(content));
	}

	@Test
	public void shouldThrow400ExceptionWhenNoUuidPassed() {
		try {
			methodeTransformerResource.getByUuid(null, httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(MethodeTransformerResource.ErrorMessage.UUID_REQUIRED.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_BAD_REQUEST));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow404ExceptionWhenContentNotFoundInMethode() {
		UUID randomUuid = UUID.randomUUID();
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenThrow(new MethodeFileNotFoundException(randomUuid));
		try {
			methodeTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(MethodeTransformerResource.ErrorMessage.METHODE_FILE_NOT_FOUND.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_NOT_FOUND));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

	@Test
	public void shouldThrow422ExceptionWhenContentNotEligibleForPublishing() {
		UUID randomUuid = UUID.randomUUID();
		EomFile eomFile = mock(EomFile.class);
		when(methodeFileService.fileByUuid(randomUuid, TRANSACTION_ID)).thenReturn(eomFile);
		when(eomFileProcessorForContentStore.process(eomFile, TRANSACTION_ID)).
				thenThrow(new MethodeContentNotEligibleForPublishException(randomUuid, "oh no!"));
		try {
			methodeTransformerResource.getByUuid(randomUuid.toString(), httpHeaders);
			fail("No exception was thrown, but expected one.");
		} catch (WebApplicationClientException wace) {
			assertThat(((ErrorEntity)wace.getResponse().getEntity()).getMessage(),
					equalTo(MethodeTransformerResource.ErrorMessage.METHODE_CONTENT_TYPE_NOT_SUPPORTED.toString()));
			assertThat(wace.getResponse().getStatus(), equalTo(HttpStatus.SC_UNPROCESSABLE_ENTITY));
		} catch (Throwable throwable) {
			fail(String.format("The thrown exception was not of expected type. It was [%s] instead.",
					throwable.getClass().getCanonicalName()));
		}
	}

}

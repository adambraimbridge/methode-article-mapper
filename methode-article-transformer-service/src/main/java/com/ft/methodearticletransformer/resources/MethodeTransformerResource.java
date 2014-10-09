package com.ft.methodearticletransformer.resources;


import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.ft.api.jaxrs.errors.ClientError;
import com.ft.api.util.transactionid.TransactionIdUtils;
import com.ft.content.model.Content;
import com.ft.methodeapi.model.EomFile;
import com.ft.methodearticletransformer.methode.MethodeContentNotEligibleForPublishException;
import com.ft.methodearticletransformer.methode.MethodeFileNotFoundException;
import com.ft.methodearticletransformer.methode.MethodeFileService;
import com.ft.methodearticletransformer.transformation.EomFileProcessorForContentStore;

@Path("/content")
public class MethodeTransformerResource {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";
    
    private final MethodeFileService methodeFileService;
    private final EomFileProcessorForContentStore eomFileProcessorForContentStore;

    public MethodeTransformerResource(MethodeFileService methodeFileService, EomFileProcessorForContentStore eomFileProcessorForContentStore) {
		this.methodeFileService = methodeFileService;
		this.eomFileProcessorForContentStore = eomFileProcessorForContentStore;
	}

	@GET
    @Timed
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON + CHARSET_UTF_8)
    public final Content getByUuid(@PathParam("uuid") String uuid, @Context HttpHeaders httpHeaders) {
		
		String transactionId = TransactionIdUtils.getTransactionIdOrDie(httpHeaders, uuid, "Publish request");
        if (uuid == null) {
            throw ClientError.status(400).context(uuid).error("uuid is required").exception();
        }
        
        try {
        	EomFile eomFile = methodeFileService.fileByUuid(UUID.fromString(uuid), transactionId);
    		return eomFileProcessorForContentStore.process(eomFile, transactionId);
        } catch (MethodeFileNotFoundException e) {
			throw ClientError.status(404)
			.reason(ErrorMessage.METHODE_FILE_NOT_FOUND)
			.exception(e);
        } catch (MethodeContentNotEligibleForPublishException e) {
        	throw ClientError.status(422)
			.reason(ErrorMessage.METHODE_CONTENT_TYPE_NOT_SUPPORTED)
			.exception(e);
        }
		
    }
	private enum ErrorMessage {
		METHODE_FILE_NOT_FOUND("Article cannot be found in Methode"),
		METHODE_CONTENT_TYPE_NOT_SUPPORTED("Unsupported article type - not a compound story");


	    private final String text;

	    ErrorMessage(String text) {
	        this.text = text;
	    }

	    @Override
	    public String toString() {
	        return text;
	    }
	}
}
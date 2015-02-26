package com.ft.methodearticletransformer.transformation;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import com.ft.bodyprocessing.BodyProcessingContext;
import com.ft.bodyprocessing.xml.StAXTransformingBodyProcessor;
import com.ft.bodyprocessing.xml.eventhandlers.BaseXMLParser;
import com.ft.bodyprocessing.xml.eventhandlers.XmlParser;
import org.apache.commons.lang.StringUtils;

public class PullQuoteXMLParser extends BaseXMLParser<PullQuoteData> implements XmlParser<PullQuoteData> {

	private static final String QUOTE_SOURCE = "web-pull-quote-source";
	private static final String QUOTE_TEXT = "web-pull-quote-text";
	private static final String PULL_QUOTE = "web-pull-quote";
    private static final String DUMMY_SOURCE_TEXT = "EM-dummyText";
	private StAXTransformingBodyProcessor stAXTransformingBodyProcessor;

	public PullQuoteXMLParser(StAXTransformingBodyProcessor stAXTransformingBodyProcessor) {
		super(PULL_QUOTE);
		checkNotNull(stAXTransformingBodyProcessor, "The StAXTransformingBodyProcessor cannot be null.");
		this.stAXTransformingBodyProcessor = stAXTransformingBodyProcessor;
	}

	@Override
	public void transformFieldContentToStructuredFormat(PullQuoteData pullQuoteData, BodyProcessingContext bodyProcessingContext) {
		// TODO Remove this method.
		throw new IllegalStateException("This method should no longer be called.");
	}

	@Override
	public PullQuoteData createDataBeanInstance() {
		return new PullQuoteData();
	}

	private String transformRawContentToStructuredFormat(String unprocessedContent, BodyProcessingContext bodyProcessingContext) {
		if (!StringUtils.isBlank(unprocessedContent)) {
			return stAXTransformingBodyProcessor.process(unprocessedContent, bodyProcessingContext);
		}
		return "";
	}

	@Override
	protected void populateBean(PullQuoteData pullQuoteData, StartElement nextStartElement,
								XMLEventReader xmlEventReader, BodyProcessingContext bodyProcessingContext) {
		// look for either web-pull-quote-text or web-pull-quote-source
		if (isElementNamed(nextStartElement.getName(), QUOTE_TEXT)) {
			pullQuoteData.setQuoteText(transformRawContentToStructuredFormat(parseRawContent(QUOTE_TEXT, xmlEventReader), bodyProcessingContext));
		}
		if (isElementNamed(nextStartElement.getName(), QUOTE_SOURCE)) {
			pullQuoteData.setQuoteSource(transformRawContentToStructuredFormat(parseRawContent(QUOTE_SOURCE, xmlEventReader), bodyProcessingContext));
		}
	}

	@Override
	public boolean doesTriggerElementContainAllDataNeeded() {
		return false;
	}

}

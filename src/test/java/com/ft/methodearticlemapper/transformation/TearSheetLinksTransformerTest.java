package com.ft.methodearticlemapper.transformation;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.ft.bodyprocessing.BodyProcessingException;
import com.ft.methodearticlemapper.model.concordance.ConceptView;
import com.ft.methodearticlemapper.model.concordance.Concordance;
import com.ft.methodearticlemapper.model.concordance.Concordances;
import com.ft.methodearticlemapper.model.concordance.Identifier;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndTextQualifier;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TearSheetLinksTransformerTest {
  private static final URI CONCORDANCE_API = URI.create("http://localhost/concordances");
  private static final URI CONCORDANCE_URL =
      URI.create(
          "http://localhost/concordances?authority=http%3A%2F%2Fapi.ft.com%2Fsystem%2FFT-TME&identifierValue=");
  private static final String TME_AUTHORITY = "http://api.ft.com/system/FT-TME";
  private static final String TME_ID_1 = "tmeid1";
  private static final String TME_ID_2 = "tmeid2";
  private static final String ORG_ID = UUID.randomUUID().toString();
  private static final String API_URL2 = "http://api.ft.com/organisations/" + ORG_ID;

  private static final String BODY_TEMPLATE = "<body><p>Some text</p>%s</body>";

  private static final String COMPANY_NOT_CONCORDED =
      "<company  DICoSEDOL=\"2297907\" CompositeId=\""
          + TME_ID_1
          + "\" >not concorded company name</company>";
  private static final String COMPANY_CONCORDED =
      "<company  DICoSEDOL=\"2297907\" CompositeId=\""
          + TME_ID_2
          + "\" >concorded company name</company>";
  private static final String COMPANY_NO_COMPOSITE_ID =
      "<company  DICoSEDOL=\"2297907\" >company without compositeId</company>";

  private Document doc;
  private NodeList nodes;
  private DocumentBuilder db;
  private TearSheetLinksTransformer toTest;
  private Client client = mock(Client.class);
  private Identifier identifier = new Identifier(TME_AUTHORITY, TME_ID_2);
  private ConceptView concept = new ConceptView(API_URL2, API_URL2);
  private Concordances concordances;

  @Before
  public void setUp() throws Exception {
    toTest = new TearSheetLinksTransformer(client, CONCORDANCE_API);
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    db = dbf.newDocumentBuilder();

    Concordance concordance = new Concordance(concept, identifier);
    concordances = new Concordances(Arrays.asList(concordance));
  }

  @Test
  public void shouldNotChangeBodyIfNoCompanyTags() throws Exception {
    final String body = String.format(BODY_TEMPLATE, "");
    doc = db.parse(new InputSource(new StringReader(body)));
    nodes = getNodeList(doc);
    toTest.handle(doc, nodes);
    String actual = serializeDocument(doc);
    assertThat(actual, equalTo(body));

    verifyZeroInteractions(client);
  }

  @Test
  public void shouldNotChangeBodyIfCompanyTMEIdNotConcorded() throws Exception {
    final String body = String.format(BODY_TEMPLATE, COMPANY_NOT_CONCORDED);
    doc = db.parse(new InputSource(new StringReader(body)));
    nodes = getNodeList(doc);
    String queryUrl = CONCORDANCE_URL + TME_ID_1;
    mockConcordanceQueryResponse(queryUrl, null);
    toTest.handle(doc, nodes);
    String actual = serializeDocument(doc);
    Diff diff = new Diff(body, actual);
    diff.overrideElementQualifier(new ElementNameAndTextQualifier());
    XMLAssert.assertXMLEqual(diff, true);
  }

  @Test
  public void shouldTransformCompanyIfCompanyTMEIdIsConcorded() throws Exception {
    final String body = String.format(BODY_TEMPLATE, "<p>" + COMPANY_CONCORDED + "</p>");
    doc = db.parse(new InputSource(new StringReader(body)));
    nodes = getNodeList(doc);
    String queryUrl = CONCORDANCE_URL + TME_ID_2;
    mockConcordanceQueryResponse(queryUrl, concordances);
    toTest.handle(doc, nodes);
    String actual = serializeDocument(doc);

    String expectedBody =
        "<body>"
            + "<p>Some text</p>"
            + "<p><concept type=\"http://www.ft.com/ontology/company/PublicCompany\" id=\""
            + ORG_ID
            + "\">concorded company name</concept></p>"
            + "</body>";

    Diff diff = new Diff(expectedBody, actual);
    diff.overrideElementQualifier(new ElementNameAndTextQualifier());
    XMLAssert.assertXMLEqual(diff, true);
  }

  @Test
  public void shouldTransformMultipleCompanies() throws Exception {
    final String body =
        String.format(
            BODY_TEMPLATE,
            "<p>" + COMPANY_NOT_CONCORDED + "</p>" + "<p>" + COMPANY_CONCORDED + "</p>");
    doc = db.parse(new InputSource(new StringReader(body)));
    nodes = getNodeList(doc);
    String queryUrl1 = CONCORDANCE_URL + TME_ID_2 + "&identifierValue=" + TME_ID_1;
    mockConcordanceQueryResponse(queryUrl1, concordances);
    toTest.handle(doc, nodes);
    String actual = serializeDocument(doc);

    String expectedBody =
        "<body>"
            + "<p>Some text</p>"
            + "<p>"
            + COMPANY_NOT_CONCORDED
            + "</p>"
            + "<p><concept type=\"http://www.ft.com/ontology/company/PublicCompany\" id=\""
            + ORG_ID
            + "\">concorded company name</concept></p>"
            + "</body>";

    Diff diff = new Diff(expectedBody, actual);
    diff.overrideElementQualifier(new ElementNameAndTextQualifier());
    XMLAssert.assertXMLEqual(diff, true);
  }

  @Test
  public void shouldTransformCompanyIfCompositeIdAttributeIsLowerCase() throws Exception {
    final String body =
        String.format(
            BODY_TEMPLATE,
            "<p>" + COMPANY_CONCORDED.replace("CompositeId", "compositeid") + "</p>");
    doc = db.parse(new InputSource(new StringReader(body)));
    nodes = getNodeList(doc);
    String queryUrl = CONCORDANCE_URL + TME_ID_2;
    mockConcordanceQueryResponse(queryUrl, concordances);
    toTest.handle(doc, nodes);
    String actual = serializeDocument(doc);

    String expectedBody =
        "<body>"
            + "<p>Some text</p>"
            + "<p><concept type=\"http://www.ft.com/ontology/company/PublicCompany\" id=\""
            + ORG_ID
            + "\">concorded company name</concept></p>"
            + "</body>";

    Diff diff = new Diff(expectedBody, actual);
    diff.overrideElementQualifier(new ElementNameAndTextQualifier());
    XMLAssert.assertXMLEqual(diff, true);
  }

  @Test
  public void shouldNotCallConcordancesIfCompanyHasNoCompositeId() throws Exception {
    final String body = String.format(BODY_TEMPLATE, COMPANY_NO_COMPOSITE_ID);
    doc = db.parse(new InputSource(new StringReader(body)));
    nodes = getNodeList(doc);
    toTest.handle(doc, nodes);
    String actual = serializeDocument(doc);
    Diff diff = new Diff(body, actual);
    diff.overrideElementQualifier(new ElementNameAndTextQualifier());
    XMLAssert.assertXMLEqual(diff, true);

    verifyZeroInteractions(client);
  }

  private NodeList getNodeList(Document doc) throws Exception {
    XPathFactory xpf = XPathFactory.newInstance();
    XPath xp = xpf.newXPath();
    return nodes = (NodeList) xp.evaluate("//company", doc, XPathConstants.NODESET);
  }

  private String serializeDocument(Document document) {
    DOMSource domSource = new DOMSource(document);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);

    TransformerFactory tf = TransformerFactory.newInstance();
    try {
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty("omit-xml-declaration", "yes");
      transformer.setOutputProperty("standalone", "yes");
      transformer.transform(domSource, result);
      writer.flush();
      String body = writer.toString();
      return body;
    } catch (TransformerException e) {
      throw new BodyProcessingException(e);
    }
  }

  private void mockConcordanceQueryResponse(String queryUrl, Concordances response) {
    WebResource resource = mock(WebResource.class);
    when(client.resource(URI.create(queryUrl))).thenReturn(resource);
    WebResource.Builder builder = mock(WebResource.Builder.class);
    when(resource.header("Host", "public-concordances-api")).thenReturn(builder);
    when(builder.get(Concordances.class)).thenReturn(response);
  }
}

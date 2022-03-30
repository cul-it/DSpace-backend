/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.pubmedeurope.PubmedEuropeMetadataSourceServiceImpl;
import org.dspace.importer.external.scopus.service.LiveImportClientImpl;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link PubmedEuropeMetadataSourceServiceImpl}
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class PubmedEuropeMetadataSourceServiceIT extends AbstractControllerIntegrationTest {

    @Autowired
    private PubmedEuropeMetadataSourceServiceImpl pubmedEuropeMetadataServiceImpl;

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    @Test
    public void pubmedEuropeImportMetadataGetRecordsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        CloseableHttpClient originalHttpClient = liveImportClientImpl.getHttpClient();
        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);
        String path = testProps.get("test.pubmedeurope").toString();
        String path2file = testProps.get("test.pubmedeurope-empty").toString();
        try (FileInputStream file = new FileInputStream(path)) {
        try (FileInputStream file2 = new FileInputStream(path2file)) {
            String xmlMetricsExample = IOUtils.toString(file, Charset.defaultCharset());
            String xmlMetricsExample2 = IOUtils.toString(file2, Charset.defaultCharset());

            liveImportClientImpl.setHttpClient(httpClient);

            CloseableHttpResponse response = mockResponse(xmlMetricsExample, 200, "OK");
            CloseableHttpResponse response2 = mockResponse(xmlMetricsExample2, 200, "OK");

            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response, response2);

            context.restoreAuthSystemState();
            Collection<ImportRecord> recordsImported = pubmedEuropeMetadataServiceImpl.getRecords("test query", 0, 3);
            Collection<ImportRecord> collection2match = getRecords();
            assertEquals(3, recordsImported.size());
            assertTrue(matchRecords(recordsImported, collection2match));
        }
        } finally {
            liveImportClientImpl.setHttpClient(originalHttpClient);
        }
    }

    private boolean matchRecords(Collection<ImportRecord> recordsImported, Collection<ImportRecord> records2match) {
        ImportRecord  firstImported = recordsImported.iterator().next();
        ImportRecord  secondImported = recordsImported.iterator().next();
        ImportRecord  first2match = recordsImported.iterator().next();
        ImportRecord  second2match = recordsImported.iterator().next();
        boolean checkFirstRecord = firstImported.getValueList().containsAll(first2match.getValueList());
        boolean checkSecondRecord = secondImported.getValueList().containsAll(second2match.getValueList());
        return checkFirstRecord && checkSecondRecord;
    }

    private CloseableHttpResponse mockResponse(String xmlExample, int statusCode, String reason)
            throws UnsupportedEncodingException {
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setChunked(true);
        basicHttpEntity.setContent(new StringInputStream(xmlExample));

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine(statusCode, reason));
        when(response.getEntity()).thenReturn(basicHttpEntity);
        return response;
    }

    private Collection<ImportRecord> getRecords() {
        Collection<ImportRecord> records = new LinkedList<ImportRecord>();
        List<MetadatumDTO> metadatums  = new ArrayList<MetadatumDTO>();
        //define first record
        MetadatumDTO title = createMetadatumDTO("dc","title", null,
                "First record of preserved soft parts in a Palaeozoic podocopid"
                + " (Metacopina) ostracod, Cytherellina submagna: phylogenetic implications.");
        MetadatumDTO contributor = createMetadatumDTO("dc", "contributor", "author", "Olempska E");
        MetadatumDTO contributor2 = createMetadatumDTO("dc", "contributor", "author", "Horne DJ");
        MetadatumDTO contributor3 = createMetadatumDTO("dc", "contributor", "author", "Szaniawski H");
        MetadatumDTO doi = createMetadatumDTO("dc", "identifier", "doi", "10.1098/rspb.2011.0943");
        MetadatumDTO source = createMetadatumDTO("dc", "source", null, "Proceedings. Biological sciences");
        MetadatumDTO date = createMetadatumDTO("dc", "date", "issued", "2012");
        MetadatumDTO language = createMetadatumDTO("dc", "language", "iso", "eng");
        MetadatumDTO type = createMetadatumDTO("dc", "type", null, "Research Support, Non-U.S. Gov't");
        MetadatumDTO type2 = createMetadatumDTO("dc", "type", null, "research-article");
        MetadatumDTO type3 = createMetadatumDTO("dc", "type", null, "Journal Article");

        MetadatumDTO issn = createMetadatumDTO("dc", "identifier", "issn", "0962-8452");
        MetadatumDTO pmid = createMetadatumDTO("dc", "identifier", "pmid", "21733903");
        MetadatumDTO description = createMetadatumDTO("dc", "description", "abstract", "The metacopines represent one"
                + " of the oldest and most important extinct groups of ostracods, with a fossil record from"
                + " the Mid-Ordovician to the Early Jurassic. Herein, we report the discovery of a representative"
                + " of the group with three-dimensionally preserved soft parts. The specimen--a male of Cytherellina"
                + " submagna--was found in the Early Devonian (416 Ma) of Podolia, Ukraine. A branchial plate (Bp)"
                + " of the cephalic maxillula (Mx), a pair of thoracic appendages (walking legs), a presumed furca"
                + " (Fu) and a copulatory organ are preserved. The material also includes phosphatized steinkerns"
                + " with exceptionally preserved marginal pore canals and muscle scars. The morphology of the"
                + " preserved limbs and valves of C. submagna suggests its relationship with extant Podocopida,"
                + " particularly with the superfamilies Darwinuloidea and Sigillioidea, which have many similar"
                + " characteristic features, including a large Bp on the Mx, the morphology of walking legs, Fu"
                + " with two terminal claws, internal stop-teeth in the left valve, adductor muscle scar pattern,"
                + " and a very narrow fused zone along the anterior and posterior margins. More precise"
                + " determination of affinities will depend on the soft-part morphology of the cephalic segment,"
                + " which has not been revealed in the present material.");

        metadatums.add(title);
        metadatums.add(contributor);
        metadatums.add(contributor2);
        metadatums.add(contributor3);
        metadatums.add(doi);
        metadatums.add(source);
        metadatums.add(date);
        metadatums.add(language);
        metadatums.add(type);
        metadatums.add(type2);
        metadatums.add(type3);
        metadatums.add(issn);
        metadatums.add(pmid);
        metadatums.add(description);
        ImportRecord firstrRecord = new ImportRecord(metadatums);

        //define second record
        List<MetadatumDTO> metadatums2  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title2 = createMetadatumDTO("dc","title", null, "VODKA SYNEVIR");
        MetadatumDTO contributor4 = createMetadatumDTO("dc", "contributor", "author", "BARANOV VALENTYN VOLODYMYROVYC");
        MetadatumDTO contributor5 = createMetadatumDTO("dc", "contributor", "author", "DANCHAK ROMAN ADAMOVYCH");
        MetadatumDTO contributor6 = createMetadatumDTO("dc", "contributor", "author", "FEDORCHUK NATALIIA HRYHORIVNA");
        MetadatumDTO contributor7 = createMetadatumDTO("dc", "contributor", "author", "FEDOREIKO LIUBOV ROMANIVNA");
        MetadatumDTO language2 = createMetadatumDTO("dc", "language", "iso", "eng");
        MetadatumDTO type4 = createMetadatumDTO("dc", "type", null, "Patent");
        MetadatumDTO pmid2 = createMetadatumDTO("dc", "identifier", "pmid", "UA37818");
        MetadatumDTO description2 = createMetadatumDTO("dc", "description", "abstract",
                "Vodka contains aqueous-alcoholic"
                + " mixture, 65.8 % sugar syrup and apple vinegar."
                + " As a result the vodka has light taste without vodka acid and light apple aroma.");
        metadatums2.add(title2);
        metadatums2.add(contributor4);
        metadatums2.add(contributor5);
        metadatums2.add(contributor6);
        metadatums2.add(contributor7);
        metadatums2.add(language2);
        metadatums2.add(type4);
        metadatums2.add(pmid2);
        metadatums2.add(description2);
        ImportRecord secondRecord = new ImportRecord(metadatums2);

        //define second record
        List<MetadatumDTO> metadatums3  = new ArrayList<MetadatumDTO>();
        MetadatumDTO title3 = createMetadatumDTO("dc","title", null, "A VODKA CHARKA");
        MetadatumDTO contributor8 = createMetadatumDTO("dc", "contributor", "author", "BARANOV VALENTYN VOLODYMYROVYC");
        MetadatumDTO contributor9 = createMetadatumDTO("dc", "contributor", "author", "HDANCHAK ROMAN ADAMOVYCH");
        MetadatumDTO contributor10 = createMetadatumDTO("dc", "contributor", "author", "FEDORCHUK NATALIIA HRYHORIVNA");
        MetadatumDTO contributor11 = createMetadatumDTO("dc", "contributor", "author", "FEDOREIKO LIUBOV ROMANIVNA");
        MetadatumDTO language3 = createMetadatumDTO("dc", "language", "iso", "eng");
        MetadatumDTO type5 = createMetadatumDTO("dc", "type", null, "Patent");
        MetadatumDTO pmid3 = createMetadatumDTO("dc", "identifier", "pmid", "UA37954");
        MetadatumDTO description3 = createMetadatumDTO("dc", "description", "abstract", "The invention relates to"
                + " food industry, and particularly to liqueur and vodka industry, to vodkas compositions."
                + " The aim of this invention is producing vodka with high organoleptic indices, and particularly"
                + " soft taste without vodka bitterness and without vodka aroma, and high biological properties,"
                + " by selection of necessary ingredients at required quantities. Ingredients ratio at 100 l of"
                + " finished drink: Carbohydrate module ôAlkosoftö, l 0.07-0.13Citric oil, kg 0,0015-0,0025"
                + " Aqueous-alcoholic mixture as calculated per strength of 40% of volume rest."
                + " Technical result - preparation of the vodka of given composition with a strength of 40% of"
                + " volume, which is transparent, colorless, has mild taste without vodka bitterness and without"
                + " strong vodka aroma which will not cause alcohol withdrawal syndrome and high charge on the body.");
        metadatums3.add(title3);
        metadatums3.add(contributor8);
        metadatums3.add(contributor9);
        metadatums3.add(contributor10);
        metadatums3.add(contributor11);
        metadatums3.add(language3);
        metadatums3.add(type5);
        metadatums3.add(pmid3);
        metadatums3.add(description3);
        ImportRecord thirdRecord = new ImportRecord(metadatums3);

        records.add(firstrRecord);
        records.add(secondRecord);
        records.add(thirdRecord);
        return records;
    }

    private MetadatumDTO createMetadatumDTO(String schema, String element, String qualifier, String value) {
        MetadatumDTO metadatumDTO = new MetadatumDTO();
        metadatumDTO.setSchema(schema);
        metadatumDTO.setElement(element);
        metadatumDTO.setQualifier(qualifier);
        metadatumDTO.setValue(value);
        return metadatumDTO;
    }

    private StatusLine statusLine(int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }

}
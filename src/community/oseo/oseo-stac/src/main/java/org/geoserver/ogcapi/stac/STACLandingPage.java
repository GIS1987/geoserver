/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.geoserver.ogcapi.JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.http.HttpMethod;

/** A STAC server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class STACLandingPage extends AbstractLandingPageDocumentNoConformance {

    /** <code>rel</code> for the search resource */
    public static String REL_SEARCH = "search";

    public static String LANDING_PAGE_ID = "GeoserverSTACLandingPage";

    private String stacVersion = STACService.STAC_VERSION;
    private final List<String> conformsTo;
    private final String type = STACService.TYPE_CATALOG;
    private final String id = LANDING_PAGE_ID;

    public STACLandingPage(
            OSEOInfo service, String basePath, List<String> conformsTo, Set<String> collectionIds) {
        super(
                (service.getTitle() == null) ? "STAC server" : service.getTitle(),
                (service.getAbstract() == null)
                        ? "STAC server implementation"
                        : service.getAbstract(),
                basePath);
        this.conformsTo = conformsTo;

        // conformance
        new LinksBuilder(ConformanceDocument.class, basePath)
                .segment("conformance")
                .title("Conformance declaration as ")
                .rel(Link.REL_CONFORMANCE)
                .add(this);

        // collections
        new LinksBuilder(CollectionsResponse.class, basePath)
                .segment("collections")
                .title("Collections Metadata as ")
                .rel(Link.REL_DATA)
                .add(this);

        // link to each collection as a child
        for (String collectionId : collectionIds) {
            String href =
                    ResponseUtils.buildURL(
                            APIRequestInfo.get().getBaseURL(),
                            basePath + "/collections/" + ResponseUtils.urlEncode(collectionId),
                            null,
                            URLMangler.URLType.SERVICE);
            Link link = new Link(href, "child", "application/json", null);
            getLinks().add(link);
        }

        // search, GET
        new LinksBuilder(SearchResponse.class, basePath)
                .segment("search")
                .title("Items as ")
                .rel(REL_SEARCH)
                .classification("searchGet")
                .updater((t1, l2) -> l2.setMethod(HttpMethod.GET))
                .add(this);

        // search, POST
        new LinksBuilder(SearchResponse.class, basePath)
                .segment("search")
                .title("Items as ")
                .rel(REL_SEARCH)
                .classification("searchPost")
                .updater((t, l1) -> l1.setMethod(HttpMethod.POST))
                .add(this);

        // queryables
        links.addAll(
                APIRequestInfo.get()
                        .getLinksFor(
                                basePath + "/queryables",
                                Queryables.class,
                                "Queryables as ",
                                Queryables.REL,
                                true,
                                "queryables",
                                null)
                        .stream()
                        .filter(
                                l ->
                                        "text/html".equals(l.getType())
                                                || SCHEMA_TYPE_VALUE.equals(l.getType()))
                        .collect(Collectors.toList()));
    }

    @JsonProperty("stac_version")
    public String getStacVersion() {
        return stacVersion;
    }

    public List<String> getConformsTo() {
        return conformsTo;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getId() {
        return id;
    }
}

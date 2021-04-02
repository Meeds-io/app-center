package org.exoplatform.appcenter.search;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.appcenter.dto.ApplicationSearchResult;
import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplicationSearchConnector {
    private static final Log LOG                          = ExoLogger.getLogger(ApplicationSearchConnector.class);

    private static final String          SEARCH_QUERY_FILE_PATH_PARAM = "query.file.path";

    private final ConfigurationManager   configurationManager;

    private final IdentityManager        identityManager;

    private final ElasticSearchingClient client;

    private String                       index;

    private String                       searchType;

    private String                       searchQueryFilePath;

    private String                       searchQuery;

    public ApplicationSearchConnector(ConfigurationManager configurationManager,
                                      IdentityManager identityManager,
                                      ElasticSearchingClient client,
                                      InitParams initParams) {
        this.configurationManager = configurationManager;
        this.identityManager = identityManager;
        this.client = client;
        PropertiesParam param = initParams.getPropertiesParam("constructor.params");
        this.index = param.getProperty("index");
        this.searchType = param.getProperty("searchType");
        if (initParams.containsKey(SEARCH_QUERY_FILE_PATH_PARAM)) {
            searchQueryFilePath = initParams.getValueParam(SEARCH_QUERY_FILE_PATH_PARAM).getValue();
            try {
                retrieveSearchQuery();
            } catch (Exception e) {
                LOG.error("Can't read elasticsearch search query from path {}", searchQueryFilePath, e);
            }
        }
    }

    public List<ApplicationSearchResult> search(long userIdentityId, String term, long offset, long limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be positive");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        if (userIdentityId < 0) {
            throw new IllegalArgumentException("User identity id must be positive");
        }
        String esQuery = buildQueryStatement(userIdentityId, term, offset, limit);
        String jsonResponse = this.client.sendRequest(esQuery, this.index, this.searchType);
        return buildResult(jsonResponse);
    }


    @SuppressWarnings("rawtypes")
    private List<ApplicationSearchResult> buildResult(String jsonResponse) {
        LOG.debug("Search Query response from ES : {} ", jsonResponse);
        List<ApplicationSearchResult> results = new ArrayList<>();
        JSONParser parser = new JSONParser();

        Map json = null;
        try {
            json = (Map) parser.parse(jsonResponse);
        } catch (ParseException e) {
            throw new ElasticSearchException("Unable to parse JSON response", e);
        }

        JSONObject jsonResult = (JSONObject) json.get("hits");
        if (jsonResult == null) {
            return results;
        }

        JSONArray jsonHits = (JSONArray) jsonResult.get("hits");
        for (Object jsonHit : jsonHits) {
            try {
                ApplicationSearchResult applicationSearchResult = new ApplicationSearchResult();
                JSONObject jsonHitObject = (JSONObject) jsonHit;
                JSONObject hitSource = (JSONObject) jsonHitObject.get("_source");
                long id = parseLong(hitSource, "id");
                String title = (String) hitSource.get("title");
                String description = (String) hitSource.get("description");
                JSONObject highlightSource = (JSONObject) jsonHitObject.get("highlight");
                List<String> excerpts = new ArrayList<>();
                if (highlightSource != null) {
                    if (highlightSource.get("description") != null) {
                        description = ((JSONArray) highlightSource.get("description")).get(0).toString();
                        highlightSource.remove("description");
                    }
                    for (Object value : highlightSource.values()) {
                        JSONArray excerptValue = (JSONArray) value;
                        if (excerptValue != null && !excerptValue.isEmpty()) {
                            excerpts.add(excerptValue.get(0).toString());
                        }
                    }
                }
                // Application
                applicationSearchResult.setId(id);
                applicationSearchResult.setDescription(description);
                applicationSearchResult.setTitle(title);
            } catch (Exception e) {
                LOG.warn("Error processing event search result item, ignore it from results", e);
            }
        }
        return results;
    }

    private String buildQueryStatement(long userIdentityId, String term, long offset, long limit) {
        List<String> termsQuery = Arrays.stream(term.split(" ")).filter(StringUtils::isNotBlank).map(word -> {
            word = word.trim();
            if (word.length() > 5) {
                word = word + "~1";
            }
            return word;
        }).collect(Collectors.toList());
        String termQuery = StringUtils.join(termsQuery, " AND ");
        return retrieveSearchQuery().replaceAll("@term@", term)
                                    .replaceAll("@term_query@", termQuery)
                                    .replaceAll("@permissions@", String.valueOf(userIdentityId))
                                    .replaceAll("@offset@", String.valueOf(offset))
                                    .replaceAll("@limit@", String.valueOf(limit));
    }


    private String retrieveSearchQuery() {
        if (StringUtils.isBlank(this.searchQuery) || PropertyManager.isDevelopping()) {
            try {
                InputStream queryFileIS = this.configurationManager.getInputStream(searchQueryFilePath);
                this.searchQuery = IOUtil.getStreamContentAsString(queryFileIS);
            } catch (Exception e) {
                throw new IllegalStateException("Error retrieving search query from file: " + searchQueryFilePath, e);
            }
        }
        return this.searchQuery;
    }

    private Identity getIdentityById(IdentityManager identityManager, long identityId){
        return identityManager.getIdentity(String.valueOf(identityId));
    }

    private Long parseLong(JSONObject hitSource, String key) {
        String value = (String) hitSource.get(key);
        return StringUtils.isBlank(value) ? null : Long.parseLong(value);
    }
}

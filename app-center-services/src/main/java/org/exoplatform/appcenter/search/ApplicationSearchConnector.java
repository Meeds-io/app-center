package org.exoplatform.appcenter.search;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
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

    private static final String          SEARCH_ALL_QUERY_FILE_PATH_PARAM = "query.all.file.path";


    private ConfigurationManager   configurationManager;

    private IdentityManager        identityManager;

    private ElasticSearchingClient client;

    private String                       index;

    private String                       searchType;

    private String                       searchQueryFilePath;

    private String                       searchAllQueryFilePath;

    private String                       searchQuery;

    public ApplicationSearchConnector() {
    }

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
            searchAllQueryFilePath = initParams.getValueParam(SEARCH_ALL_QUERY_FILE_PATH_PARAM).getValue();
            try {
                retrieveSearchQuery(searchQueryFilePath);
                retrieveSearchQuery(searchAllQueryFilePath);
            } catch (Exception e) {
                LOG.error("Can't read elasticsearch search query from path {}", searchQueryFilePath, e);
            }
        }
    }

    public List<ApplicationSearchResult> search(String currentUser, String term, long offset, long limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be positive");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }


        String esQuery = "";
        if (StringUtils.isBlank(term)) {
            esQuery = buildQueryStatement( term, offset, limit,searchAllQueryFilePath );
        }
        else {
            esQuery = buildQueryStatement( term, offset, limit,searchQueryFilePath );

        }
        String jsonResponse = this.client.sendRequest(esQuery, this.index, this.searchType);
        return buildResult(jsonResponse);
    }


    @SuppressWarnings("rawtypes")
    public List<ApplicationSearchResult> buildResult(String jsonResponse) {
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
                List<String> applicationPermissions = new ArrayList<>();
                JSONObject jsonHitObject = (JSONObject) jsonHit;
                JSONObject hitSource = (JSONObject) jsonHitObject.get("_source");
                long id = parseLong(hitSource, "id");
                String title = (String) hitSource.get("title");
                String description = (String) hitSource.get("description");
                String url = (String) hitSource.get("url");
                String helpPageURL = (String) hitSource.get("helpPageURL");
                String imageFileBody = (String) hitSource.get("imageFileBody");
                String imageFileName = (String) hitSource.get("imageFileName");
                String imageFileId = (String) hitSource.get("imageFileId");
                JSONArray permissions = (JSONArray) hitSource.get("permissions");
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
                applicationSearchResult.setUrl(url);
                applicationSearchResult.setDescription(description);
                applicationSearchResult.setTitle(title);
                applicationSearchResult.setHelpPageURL(helpPageURL);
                applicationSearchResult.setImageFileBody(imageFileBody);
                applicationSearchResult.setImageFileName(imageFileName);
                applicationSearchResult.setImageFileId(Long.valueOf(imageFileId));
                for(Object jsonObject :permissions) {
                    applicationPermissions.add((String) jsonObject);
                }
                applicationSearchResult.setPermissions(applicationPermissions);
                results.add(applicationSearchResult);
            } catch (Exception e) {
                LOG.warn("Error processing event search result item, ignore it from results", e);
            }
        }
        return results;
    }

    public String buildQueryStatement( String term, long offset, long limit,String File) {
        List<String> termsQuery = Arrays.stream(term.split(" ")).filter(StringUtils::isNotBlank).map(word -> {
            word = word.trim();
            if (word.length() > 5) {
                word = word + "~1";
            }
            return word;
        }).collect(Collectors.toList());
        String termQuery = StringUtils.join(termsQuery, " AND ");
        String permissions = getCurrentUserPermissions();
        return retrieveSearchQuery(File).replaceAll("@term@", term)
                                    .replaceAll("@term_query@", termQuery)
                                    .replaceAll("@permissions@", permissions)
                                    .replaceAll("@offset@", String.valueOf(offset))
                                    .replaceAll("@limit@", String.valueOf(limit));
    }


    public String retrieveSearchQuery(String File) {
        if (StringUtils.isBlank(this.searchQuery) || PropertyManager.isDevelopping()) {
            try {
                InputStream queryFileIS = this.configurationManager.getInputStream(File);
                this.searchQuery = IOUtil.getStreamContentAsString(queryFileIS);
            } catch (Exception e) {
                throw new IllegalStateException("Error retrieving search query from file: " + File, e);
            }
        }
        return this.searchQuery;
    }

    private Long parseLong(JSONObject hitSource, String key) {
        String value = (String) hitSource.get(key);
        return StringUtils.isBlank(value) ? null : Long.parseLong(value);
    }

    public static final String getCurrentUserPermissions() {
        ConversationState current = ConversationState.getCurrent();
        String permissions = current.getIdentity().getMemberships().toString();
        return permissions;
    }
}

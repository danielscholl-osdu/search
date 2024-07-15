package org.opengroup.osdu.search.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetailedBadRequestMessageUtil implements IDetailedBadRequestMessageUtil {

    private final ObjectMapper objectMapper;

    @Override
    public String getDetailedBadRequestMessage(SearchRequest searchRequest, Exception e) {
        String defaultErrorMessage = "Invalid parameters were given on search request";
        Throwable[] suppressed = e.getSuppressed();
        if (suppressed != null && suppressed.length > 0) {
            String fullReasonMessage = extractAndCombineReasonMessages(suppressed);
            if (StringUtils.isNotEmpty(fullReasonMessage)) {
                return fullReasonMessage;
            }
        }
        if (e.getCause() == null) {
            return defaultErrorMessage;
        }
        String msg = getKeywordFieldErrorMessage(searchRequest, e.getCause().getMessage());
        if (msg != null) {
            return msg;
        }
        return defaultErrorMessage;
    }

    private String extractAndCombineReasonMessages(Throwable[] suppressed) {
        StringJoiner stringJoiner = new StringJoiner(".");
        for (Throwable throwable : suppressed) {
            if (throwable instanceof ResponseException) {
                ResponseException responseException = (ResponseException) throwable;
                Response response = responseException.getResponse();
                HttpEntity entity = response.getEntity();
                try {
                    InputStream content = entity.getContent();
                    JsonNode errorNode = objectMapper.readValue(content, JsonNode.class);
                    JsonNode reasonNode = errorNode.findValue("root_cause").findValue("reason");
                    String reasonMessage = reasonNode.textValue();
                    if (StringUtils.isNotEmpty(reasonMessage)) {
                        stringJoiner.add(reasonMessage);
                    } else {
                        log.error(String.format("Unable to find fail reason in elasticsearch response:%s", errorNode.textValue()));
                    }
                } catch (IOException ex) {
                    log.error("Unable to parse fail reason from elasticsearch response");
                }
            }
        }
        return stringJoiner.toString();
    }

    private String getKeywordFieldErrorMessage(SearchRequest searchRequest, String msg) {
        if (msg == null) {
            return null;
        }
        if (msg.contains("Text fields are not optimised for operations that require per-document field data like aggregations and sorting")
            || msg.contains("can't sort on geo_shape field without using specific sorting feature, like geo_distance")) {
            if (searchRequest.source().sorts() != null && !searchRequest.source().sorts().isEmpty()) {
                return "Sort is not supported for one or more of the requested fields";
            }
            if (searchRequest.source().aggregations() != null && searchRequest.source().aggregations().count() > 0) {
                return "Aggregations are not supported for one or more of the specified fields";
            }
        }
        return null;
    }
}

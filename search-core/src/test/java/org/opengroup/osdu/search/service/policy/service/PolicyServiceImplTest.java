package org.opengroup.osdu.search.service.policy.service;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.policy.IPolicyFactory;
import org.opengroup.osdu.core.common.policy.IPolicyProvider;
import org.opengroup.osdu.core.common.policy.PolicyException;
import org.opengroup.osdu.search.context.UserContext;
import org.opengroup.osdu.search.policy.di.PolicyServiceConfiguration;
import org.opengroup.osdu.search.policy.service.PolicyServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PolicyServiceImplTest {

    @Mock
    private DpsHeaders headers;

    @Mock
    private HttpServletRequest request;

    @Mock
    private IPolicyFactory policyFactory;

    @Mock
    private PolicyServiceConfiguration policyServiceConfiguration;

    @Mock
    private IPolicyProvider serviceClient;

    @InjectMocks
    private PolicyServiceImpl sut;

    @Mock
    private UserContext userContext;

    @Test
    public void getCompiledPolicyTest() throws PolicyException {
        List<String> groups = new ArrayList<>(Arrays.asList("AAA", "BBB"));
        Map<String, Object> userData = new HashMap<>();
        userData.put("groups", groups);
        Map<String, Object> input = new HashMap<>();
        input.put("groups", groups);
        input.put("operation", "view");
        ArrayList<String> unknownsList = new ArrayList<>();
        unknownsList.add("input.record");
        String PolicyServiceResponse = "{\n" +
                "        \"query\": {\n" +
                "            \"bool\": {\n" +
                "                \"should\": [\n" +
                "                    {\"bool\": {\"filter\": [{\"terms\": {\"acl.owners\": [\"AAA\", \"BBB\"]}}]}},\n" +
                "                    {\"bool\": {\"filter\": [{\"terms\": {\"acl.viewers\": [\"AAA\", \"BBB\"]}}]}}\n" +
                "                ]\n" +
                "            }\n" +
                "        }\n" +
                "    }";
        Map<String, String> dpsHeaders = new HashMap<>();
        List<String> groupsList = Arrays.asList("AAA", "BBB");
        when(userContext.getDataGroups()).thenReturn(groupsList);
        when(headers.getPartitionId()).thenReturn("dpi");
        Mockito.lenient().when(headers.getHeaders()).thenReturn(dpsHeaders);
        when(policyFactory.create(any())).thenReturn(serviceClient);

        String searchPolicyId = "osdu.instance.search";
        when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);
        when(serviceClient.getCompiledPolicy("data.osdu.instance.search.allow == true", unknownsList, input)).thenReturn(PolicyServiceResponse);
        assertNotNull(sut.getCompiledPolicy());

        searchPolicyId = "osdu.partition[\"%s\"].search";
        when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);
        when(serviceClient.getCompiledPolicy("data.osdu.partition[\"dpi\"].search.allow == true", unknownsList, input)).thenReturn(PolicyServiceResponse);
        assertNotNull(sut.getCompiledPolicy());
    }

    @Test
    public void throw_policy_exception() throws PolicyException {
        List<String> groups = new ArrayList<>(Arrays.asList("AAA", "BBB"));
        Map<String, Object> userData = new HashMap<>();
        userData.put("groups", groups);
        Map<String, Object> input = new HashMap<>();
        input.put("groups", groups);
        input.put("operation", "view");
        ArrayList<String> unknownsList = new ArrayList<>();
        unknownsList.add("input.record");
        Map<String, String> dpsHeaders = new HashMap<>();
        List<String> groupsList = Arrays.asList("AAA", "BBB");
        when(userContext.getDataGroups()).thenReturn(groupsList);
        when(headers.getPartitionId()).thenReturn("dpi");
        Mockito.lenient().when(headers.getHeaders()).thenReturn(dpsHeaders);
        when(policyFactory.create(any())).thenReturn(serviceClient);

        String searchPolicyId = "osdu.instance.search";
        when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setBody("{\\\"detail\\\":\\\"Translate API Error: expected query in format data.osdu.partition...\\\"}");
        httpResponse.setResponseCode(HttpStatus.SC_BAD_REQUEST);
        PolicyException policyException = new PolicyException("Error making request to Policy service", httpResponse);
        Mockito.when(serviceClient.getCompiledPolicy(anyString(), anyList(), anyMap())).thenThrow(policyException);
        AppException exception = assertThrows(AppException.class, () -> {
            sut.getCompiledPolicy();
        });
        assertEquals(HttpStatus.SC_BAD_REQUEST, ((PolicyException)exception.getOriginalException()).getResponse().getResponseCode());

    }

    @Test
    public void throw_any_exception() throws PolicyException {
        List<String> groups = new ArrayList<>(Arrays.asList("AAA", "BBB"));
        Map<String, Object> userData = new HashMap<>();
        userData.put("groups", groups);
        Map<String, Object> input = new HashMap<>();
        input.put("groups", groups);
        input.put("operation", "view");
        ArrayList<String> unknownsList = new ArrayList<>();
        unknownsList.add("input.record");
        Map<String, String> dpsHeaders = new HashMap<>();
        List<String> groupsList = Arrays.asList("AAA", "BBB");
        when(userContext.getDataGroups()).thenReturn(groupsList);
        when(headers.getPartitionId()).thenReturn("dpi");
        Mockito.lenient().when(headers.getHeaders()).thenReturn(dpsHeaders);
        when(policyFactory.create(any())).thenReturn(serviceClient);

        String searchPolicyId = "osdu.instance.search";
        when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);

        when(serviceClient.getCompiledPolicy(anyString(), anyList(), anyMap())).thenThrow(new RuntimeException());

        AppException exception = assertThrows(AppException.class, () -> {
            sut.getCompiledPolicy();
        });
        
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getError().getCode());

    }
}

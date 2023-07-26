package org.opengroup.osdu.search.service.policy.service;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.policy.IPolicyFactory;
import org.opengroup.osdu.core.common.policy.IPolicyProvider;
import org.opengroup.osdu.core.common.policy.PolicyException;
import org.opengroup.osdu.search.policy.di.PolicyServiceConfiguration;
import org.opengroup.osdu.search.policy.service.PolicyServiceImpl;
import org.opengroup.osdu.search.provider.interfaces.IProviderHeaderService;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class PolicyServiceImplTest {

    @Mock
    private DpsHeaders headers;

    @Mock
    private IProviderHeaderService providerHeaderService;

    @Mock
    private IPolicyFactory policyFactory;

    @Mock
    private PolicyServiceConfiguration policyServiceConfiguration;

    @Spy
    private IPolicyProvider serviceClient;

    @InjectMocks
    private PolicyServiceImpl sut;

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
        dpsHeaders.put("groups", "AAA,BBB");
        Mockito.when(headers.getHeaders()).thenReturn(dpsHeaders);
        Mockito.when(headers.getPartitionId()).thenReturn("dpi");
        Mockito.when(providerHeaderService.getDataGroupsHeader())
                .thenReturn("groups");
        Mockito.when(policyFactory.create(any())).thenReturn(serviceClient);

        String searchPolicyId = "osdu.instance.search";
        Mockito.when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);
        Mockito.when(serviceClient.getCompiledPolicy("data.osdu.instance.search.allow == true", unknownsList, input)).thenReturn(PolicyServiceResponse);
        Assert.assertNotNull(sut.getCompiledPolicy(providerHeaderService));

        searchPolicyId = "osdu.partition[\"%s\"].search";
        Mockito.when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);
        Mockito.when(serviceClient.getCompiledPolicy("data.osdu.partition[\"dpi\"].search.allow == true", unknownsList, input)).thenReturn(PolicyServiceResponse);
        Assert.assertNotNull(sut.getCompiledPolicy(providerHeaderService));
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
        dpsHeaders.put("groups", "AAA,BBB");
        Mockito.when(headers.getHeaders()).thenReturn(dpsHeaders);
        Mockito.when(headers.getPartitionId()).thenReturn("dpi");
        Mockito.when(providerHeaderService.getDataGroupsHeader())
                .thenReturn("groups");
        Mockito.when(policyFactory.create(any())).thenReturn(serviceClient);

        String searchPolicyId = "osdu.instance.search";
        Mockito.when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setBody("{\\\"detail\\\":\\\"Translate API Error: expected query in format data.osdu.partition...\\\"}");
        httpResponse.setResponseCode(HttpStatus.SC_BAD_REQUEST);
        PolicyException policyException = new PolicyException("Error making request to Policy service", httpResponse);
        Mockito.when(serviceClient.getCompiledPolicy(anyString(), anyList(), anyMap())).thenThrow(policyException);

        AppException exception = assertThrows(AppException.class, () -> {
            sut.getCompiledPolicy(providerHeaderService);
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
        dpsHeaders.put("groups", "AAA,BBB");
        Mockito.when(headers.getHeaders()).thenReturn(dpsHeaders);
        Mockito.when(headers.getPartitionId()).thenReturn("dpi");
        Mockito.when(providerHeaderService.getDataGroupsHeader())
                .thenReturn("groups");
        Mockito.when(policyFactory.create(any())).thenReturn(serviceClient);

        String searchPolicyId = "osdu.instance.search";
        Mockito.when(policyServiceConfiguration.getId()).thenReturn(searchPolicyId);

        Mockito.when(serviceClient.getCompiledPolicy(anyString(), anyList(), anyMap())).thenThrow(new RuntimeException());

        AppException exception = assertThrows(AppException.class, () -> {
            sut.getCompiledPolicy(providerHeaderService);
        });
        
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, exception.getError().getCode());

    }
}

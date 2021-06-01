package org.opengroup.osdu.search.provider.gcp.di;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;
import java.io.IOException;
import java.util.Objects;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
//TODO temp fix for policy integration 
public class GcpServiceAccountJwtClient implements IServiceAccountJwtClient {

    private IdTokenProvider idTokenProvider;

    private String targetAudience;

    public GcpServiceAccountJwtClient(IdTokenProvider idTokenProvider, String targetAudience) {
        this.idTokenProvider = idTokenProvider;
        this.targetAudience = targetAudience;
    }

    public GcpServiceAccountJwtClient(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    @Override
    public String getIdToken(String serviceAccount) {
        try {
            if (Objects.isNull(this.idTokenProvider)) {
                GoogleCredentials adcCreds = GoogleCredentials.getApplicationDefault();
                if (adcCreds instanceof IdTokenProvider) {
                    this.idTokenProvider = (IdTokenProvider) adcCreds;
                } else {
                    throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Misconfigured credentials",
                        "GcpServiceAccountJwtClient have misconfigured token provider");
                }
            }
            IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder()
                .setIdTokenProvider(this.idTokenProvider)
                .setTargetAudience(this.targetAudience)
                .build();
            AccessToken accessToken = tokenCredential.refreshAccessToken();
            return accessToken.getTokenValue();
        } catch (IOException e) {
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Misconfigured credentials",
                "GcpServiceAccountJwtClient have misconfigured token provider", e);
        }
    }
}



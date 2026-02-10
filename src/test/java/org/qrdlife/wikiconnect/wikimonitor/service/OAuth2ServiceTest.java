package org.qrdlife.wikiconnect.wikimonitor.service;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.qrdlife.wikiconnect.mediawiki.client.ActionApi;
import org.qrdlife.wikiconnect.mediawiki.client.Requester;
import org.qrdlife.wikiconnect.wikimonitor.WikiMonitorApplication;
import org.qrdlife.wikiconnect.wikimonitor.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OAuth2ServiceTest {

    @Mock
    private OAuth20Service oauth20Service;
    @Mock
    private ActionApi actionApi;
    @Mock
    private Requester requester;

    private OAuth2Service oauth2Service;
    private MockedStatic<WikiMonitorApplication> wikiMonitorApplicationMock;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        wikiMonitorApplicationMock = Mockito.mockStatic(WikiMonitorApplication.class);
        wikiMonitorApplicationMock.when(WikiMonitorApplication::getOAuth20Service).thenReturn(oauth20Service);

        // We use a spy to mock the getActionApi method which creates new objects
        oauth2Service = spy(new OAuth2Service());
    }

    @AfterEach
    void tearDown() {
        wikiMonitorApplicationMock.close();
    }

    @Test
    void getAuthorizationUrl_Success() {
        String url = "http://auth.url";
        when(oauth20Service.getAuthorizationUrl()).thenReturn(url);

        String result = oauth2Service.getAuthorizationUrl();

        assertEquals(url, result);
        verify(oauth20Service).getAuthorizationUrl();
    }

    @Test
    void getAccessToken_Success() throws Exception {
        OAuth2AccessToken token = new OAuth2AccessToken("access_token");
        when(oauth20Service.getAccessToken("code")).thenReturn(token);

        OAuth2AccessToken result = oauth2Service.getAccessToken("code");

        assertEquals(token, result);
        verify(oauth20Service).getAccessToken("code");
    }

    @Test
    void getUserInfo_Success() throws Exception {
        OAuth2AccessToken token = new OAuth2AccessToken("access_token");

        // Mock getActionApi to return our mock ActionApi
        doReturn(actionApi).when(oauth2Service).getActionApi(anyString());
        when(actionApi.getRequester()).thenReturn(requester);

        String responseJson = """
                {
                    "query": {
                        "userinfo": {
                            "name": "TestUser",
                            "id": 100,
                            "centralids": {
                                "CentralAuth": 999
                            }
                        }
                    }
                }
                """;
        when(requester.get(eq("query"), any())).thenReturn(responseJson);

        User user = oauth2Service.getUserInfo(token);

        assertNotNull(user);
        assertEquals("TestUser", user.getUsername());
        assertEquals(999L, user.getCentralId()); // verify central ID is used
    }
}

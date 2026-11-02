package devPilot.backend.security;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import devPilot.backend.security.AppUserPrincipal;
import devPilot.backend.entity.User;
import devPilot.backend.services.UserService;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class GithubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String accessToken = userRequest.getAccessToken().getTokenValue();
        String scopes = userRequest.getAccessToken().getScopes() != null ? String.join(",", userRequest.getAccessToken().getScopes()) : "read:user, repo";

        User user = userService.upsertFromGitHub(oAuth2User.getAttributes(), accessToken, scopes);

//        System.out.println("HELLO WORLD D D D D D D" + user);
        return new AppUserPrincipal(user, oAuth2User.getAttributes());
    }

}

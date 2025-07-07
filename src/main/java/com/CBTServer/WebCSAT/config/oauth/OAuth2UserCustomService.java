package com.CBTServer.WebCSAT.config.oauth;

import com.CBTServer.WebCSAT.domain.User;
import com.CBTServer.WebCSAT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OAuth2UserCustomService extends DefaultOAuth2UserService {
    // 스프링 시큐리티는 발급받은 Access Token을 가지고 구글의 사용자 정보 API(userInfoEndpoint)를 호출하여
    // 사용자의 프로필 정보(이름, 이메일 등)를 이미 가져왔고,
    // 이 정보를 가지고 DB 저장 등의 후처리하는 사용자 정의 클래스

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo;
        if ("google".equals(registrationId)) {
            userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("naver".equals(registrationId)) {
            userInfo = new NaverOAuth2UserInfo(oAuth2User.getAttributes());
        } else if ("kakao".equals(registrationId)) {
            userInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        String email = userInfo.getEmail();
        String name = userInfo.getName();

        if (email == null) {
            throw new OAuth2AuthenticationException(registrationId + " OAuth2 로그인 시 email을 제공받지 못했습니다.");
        }

        User user = saveOrUpdate(email, name, registrationId);

        // CustomOAuth2User에 User 정보와 OAuth2User를 함께 담아 반환
        return new CustomOAuth2User(oAuth2User, user);
    }


    private User saveOrUpdate(String email, String name, String registrationId) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();
            if (!existingUser.getOauth2().equals(registrationId)) {
                // OAuth2AuthenticationException으로 감싸서 던짐
                OAuth2Error error = new OAuth2Error("provider_mismatch", "다른 로그인 방식으로 이미 가입된 계정입니다!\n다른 방식으로 접근해주십시오.", null);
                throw new OAuth2AuthenticationException(error);
            }
            existingUser.setNickname(name);
            return userRepository.save(existingUser);
        } else {
            User newUser = User.builder()
                    .email(email)
                    .nickname(name)
                    .oauth2(registrationId)
                    .role("USER")
                    .build();
            return userRepository.save(newUser);
        }
    }

}
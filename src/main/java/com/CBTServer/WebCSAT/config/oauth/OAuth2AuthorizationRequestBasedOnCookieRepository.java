package com.CBTServer.WebCSAT.config.oauth;

import com.CBTServer.WebCSAT.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.util.WebUtils;

//OAuth2 로그인 흐름 중, 인증요청객체(OAuth2AuthorizationRequest)를 쿠키에 저장하고 불러오는 역할
//사용자가 우리 서비스에서 소셜 로그인 버튼을 누른 후, 구글의 동의 화면을 거쳐 다시 우리 서비스로 돌아오는 과정으로 이루어집니다.
// 이 과정에서 사용자가 원래 어떤 요청을 했는지(state), 인증 후 어디로 돌아와야 하는지(redirect_uri) 등의 중요한 정보를 잠시 저장해 두어야 합니다.
// 이 클래스는 그 정보를 쿠키에 저장하는 역할을 합니다.

public class OAuth2AuthorizationRequestBasedOnCookieRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public final static String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private final static int COOKIE_EXPIRE_SECONDS = 18000; // 5hr


    // 언제 동작하나요? 인증 과정이 성공적으로 완료되거나 또는 실패하여 더 이상 인증 요청 정보가 필요 없을 때 호출됨
    // 보통 loadAuthorizationRequest가 호출되어 인증 요청 검증이 끝난 직후에 호출됩니다.
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest
                                                                         request, HttpServletResponse response) {

        OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response); // ✅ 쿠키 삭제
        return authorizationRequest;
//      return this.loadAuthorizationRequest(request);
    }

    // 인증 서버(Google 등)에서 리디렉션되어 돌아올 때 호출됩니다.
    // 요청에 포함된 쿠키에서 저장된 OAuth2AuthorizationRequest를 꺼내고,역직렬화하여 스프링시큐리티에게 반환합니다
    // 언제동작? 스프링 시큐리티는 "사용자가 돌아왔으니, 원래 보냈던 요청과 일치하는지 확인해야지" 라고 생각하며 이 메서드를 호출
    // 스프링 시큐리티는 이 객체 안의 state 값 등을 비교하여 CSRF 공격과 같은 보안 위협을 방지
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        return CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class);
    }

    // authorizationRequest 객체(로그인 요청 정보-state 값, redirect_uri )를 쿠키에 직렬화해서 저장
    //CookieUtil.addCookie(...)를 통해 쿠키에 저장 (이 쿠키가 리디렉션될 때 함께 전달됨)
    // 언제 동작하나요?
    // 사용자가 우리 서비스의 /login 페이지 등에서 "Google로 로그인"과 같은 소셜 로그인 버튼을 클릭하는 바로 그 시점에 동작합니다.
    // 스프링 시큐리티가 소셜 미디어의 인증 서버로 사용자를 리다이렉트시키기 직전에 호출됩니다.
    // 이제 사용자는 이 쿠키를 가진 상태로 구글의 인증 페이지로 이동합니다
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest
                                                 authorizationRequest, HttpServletRequest request, HttpServletResponse response) {

        if (authorizationRequest == null) { //authorizationRequest == null일 경우 기존 쿠키 삭제
            removeAuthorizationRequestCookies(request, response);
            return;
        }
        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
    }

    // 쿠키를 삭제합니다.
    // 로그인 요청 후, 로그인 성공 또는 실패 시 쿠키 정리(clean-up) 에 사용
    public void removeAuthorizationRequestCookies(HttpServletRequest request,
                                                  HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    }


}

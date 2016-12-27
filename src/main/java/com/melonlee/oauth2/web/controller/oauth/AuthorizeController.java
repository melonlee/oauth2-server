package com.melonlee.oauth2.web.controller.oauth;

import com.melonlee.oauth2.entity.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.melonlee.oauth2.Constants;
import com.melonlee.oauth2.entity.Status;
import com.melonlee.oauth2.service.ClientService;
import com.melonlee.oauth2.service.OAuthService;
import com.melonlee.oauth2.service.UserService;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Melon on 16/12/22.
 */

@Controller
public class AuthorizeController {

    @Autowired
    private OAuthService oAuthService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private UserService userService;


    @RequestMapping("/authorize")
    public Object authorize(Model model,
                            HttpServletRequest request) throws OAuthSystemException, URISyntaxException {


        OAuthAuthzRequest oauthRequest = null;
        try {
            oauthRequest = new OAuthAuthzRequest(request);
            //check clientid
            if (!oAuthService.checkClientId(oauthRequest.getClientId())) {

                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                        .setErrorDescription(Constants.INVALID_CLIENT_ID)
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }

            //如果用户没有登录，跳转到登录

            if (!login(request)) {
                model.addAttribute("client", clientService.findByClientId(oauthRequest.getClientId()));

                return "oauth2login";
            }

            String username = request.getParameter("username");

            String authorizationCode = null;

            String responseType = oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
            if (responseType.equals(ResponseType.CODE.toString())) {
                OAuthIssuerImpl oAuthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
                authorizationCode = oAuthIssuerImpl.authorizationCode();
                oAuthService.addAuthCode(authorizationCode, username);
            }

            OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_FOUND);

            //set authorize code

            builder.setCode(authorizationCode);

            //跳转回调地址

            String redirectURI = oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI);

            final OAuthResponse response = builder.location(redirectURI).buildQueryMessage();

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(new URI(response.getLocationUri()));
            return new ResponseEntity(headers, HttpStatus.valueOf(response.getResponseStatus()));

        } catch (OAuthProblemException e) {

            String redirectUri = e.getRedirectUri();
            if (OAuthUtils.isEmpty(redirectUri)) {

                //no redirecturi

                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", "application/json; charset=utf-8");
                Status status = new Status();
                status.setCode(HttpStatus.NOT_FOUND.value());
                status.setMsg(Constants.INVALID_REDIRECT_URI);
                Gson gson = new GsonBuilder().create();
                return new ResponseEntity(gson.toJson(status), headers, HttpStatus.NOT_FOUND);
            }

            final OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                    .error(e).location(redirectUri).buildQueryMessage();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(new URI(response.getLocationUri()));
            return new ResponseEntity(headers, HttpStatus.valueOf(response.getResponseStatus()));

        }

    }

    private boolean login(HttpServletRequest request) {

        if ("get".equalsIgnoreCase(request.getMethod())) {
            request.setAttribute("error", "非法的请求");
            return false;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            request.setAttribute("error", "登录失败:用户名或密码不能为空");
            return false;
        }

        User user = userService.findByUsername(username);
        if (user != null) {

            if (!userService.checkUser(username, password, user.getSalt(), user.getPassword())) {
                request.setAttribute("error", "登录失败:密码不正确");
                return false;
            } else {
                return true;
            }


        } else {
            request.setAttribute("error", "登录失败:用户名不正确");
            return false;
        }

    }


}

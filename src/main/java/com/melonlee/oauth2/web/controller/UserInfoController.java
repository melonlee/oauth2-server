package com.melonlee.oauth2.web.controller;


import com.melonlee.oauth2.Constants;
import com.melonlee.oauth2.entity.Status;
import com.melonlee.oauth2.entity.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.melonlee.oauth2.service.OAuthService;
import com.melonlee.oauth2.service.UserService;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

/**
 * Created by Melon on 16/12/22.
 */

@RestController
@RequestMapping("/v1/openapi")
public class UserInfoController {

    @Autowired
    private OAuthService oAuthService;

    @Autowired
    private UserService userService;

    @RequestMapping("/userInfo")
    public HttpEntity userInfo(HttpServletRequest request) throws OAuthSystemException {

        return checkAccessToken(request);

    }

    private HttpEntity nocheckAccessToken(HttpServletRequest request) throws OAuthProblemException, OAuthSystemException {

        OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(request, ParameterStyle.QUERY);

        String access = oauthRequest.getAccessToken();

        String username = oAuthService.getUsernameByAccessToken(access);
        User user = userService.findByUsername(username);
        Gson gson = new GsonBuilder().create();
        return new ResponseEntity(gson.toJson(user), HttpStatus.OK);
    }


    private HttpEntity checkAccessToken(HttpServletRequest request) throws OAuthSystemException {


        try {
            //构建OAuth资源请求
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(request, ParameterStyle.QUERY);

            //获取accessToken

            String accessToken = oauthRequest.getAccessToken();


            System.out.println("000---" + oAuthService.checkAccessToken(accessToken));
            if (!oAuthService.checkAccessToken(accessToken)) {
                //not found

                System.out.println("111111");
                OAuthResponse oauthResponse = OAuthResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setRealm(Constants.RESOURCE_SERVER_NAME)
                        .setError(OAuthError.ResourceResponse.INVALID_TOKEN)
                        .buildHeaderMessage();

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.add("Content-Type", "application/json; charset=utf-8");
                Status status = new Status();
                status.setCode(HttpStatus.UNAUTHORIZED.value());
                Gson gson = new GsonBuilder().create();
                return new ResponseEntity(gson.toJson(status), responseHeaders, HttpStatus.UNAUTHORIZED);

            }
            System.out.println("22222");
            String username = oAuthService.getUsernameByAccessToken(accessToken);
            System.out.println(username);
            User user = userService.findByUsername(username);
            System.out.println(user.toString());
            Gson gson = new GsonBuilder().create();
            System.out.println(gson.toJson(user));
            return new ResponseEntity(gson.toJson(user), HttpStatus.OK);

        } catch (OAuthProblemException e) {
            System.out.println("33333");
            if (OAuthUtils.isEmpty(e.getError())) {
                System.out.println("44444");
                OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setRealm(Constants.RESOURCE_SERVER_NAME)
                        .buildHeaderMessage();
                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
                return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
            }

            OAuthResponse oauthResponse = OAuthRSResponse
                    .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                    .setRealm(Constants.RESOURCE_SERVER_NAME)
                    .setError(e.getError())
                    .setErrorDescription(e.getDescription())
                    .setErrorUri(e.getUri())
                    .buildHeaderMessage();
            System.out.println("555555");
            HttpHeaders headers = new HttpHeaders();
            headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
            return new ResponseEntity(HttpStatus.BAD_REQUEST);

        }
    }

}

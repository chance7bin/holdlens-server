package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.AuthenticationDTO;
import com.echoamoy.holdlens.server.api.request.AuthenticationRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;

public interface IAuthenticationService {

    Response<AuthenticationDTO.Account> register(AuthenticationRequestDTO.Register request);

    Response<AuthenticationDTO.Login> login(AuthenticationRequestDTO.Login request);

    Response<Void> logout();

    Response<AuthenticationDTO.Account> currentAccount();
}

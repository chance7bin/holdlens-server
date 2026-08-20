package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAuthenticationService;
import com.echoamoy.holdlens.server.api.dto.AuthenticationDTO;
import com.echoamoy.holdlens.server.api.request.AuthenticationRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.auth.AuthenticationResult;
import com.echoamoy.holdlens.server.cases.auth.IAuthenticationCase;
import com.echoamoy.holdlens.server.trigger.http.auth.CurrentUser;
import com.echoamoy.holdlens.server.trigger.http.auth.CurrentUserContext;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements IAuthenticationService {

    @Resource
    private IAuthenticationCase authenticationCase;

    public AuthController() {
    }

    AuthController(IAuthenticationCase authenticationCase) {
        this.authenticationCase = authenticationCase;
    }

    @Override
    @PostMapping("/register")
    public Response<AuthenticationDTO.Account> register(@Valid @RequestBody AuthenticationRequestDTO.Register request) {
        return Response.ok(toAccount(authenticationCase.register(request.getUsername(), request.getPassword())));
    }

    @Override
    @PostMapping("/login")
    public Response<AuthenticationDTO.Login> login(@Valid @RequestBody AuthenticationRequestDTO.Login request) {
        AuthenticationResult.Login login = authenticationCase.login(request.getUsername(), request.getPassword());
        return Response.ok(new AuthenticationDTO.Login(login.getRawToken(), login.getExpiresAt(), toAccount(login.getAccount())));
    }

    @Override
    @PostMapping("/session/renew")
    public Response<AuthenticationDTO.Session> renewSession() {
        AuthenticationResult.Renewal renewal = authenticationCase.renew(
                CurrentUserContext.requireCurrentUser().sessionId());
        return Response.ok(new AuthenticationDTO.Session(renewal.getExpiresAt()));
    }

    @Override
    @PostMapping("/logout")
    public Response<Void> logout() {
        authenticationCase.logout(CurrentUserContext.requireCurrentUser().sessionId());
        return Response.ok(null);
    }

    @Override
    @GetMapping("/me")
    public Response<AuthenticationDTO.Account> currentAccount() {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        return Response.ok(toAccount(authenticationCase.currentAccount(currentUser.userId())));
    }

    private AuthenticationDTO.Account toAccount(AuthenticationResult.Account account) {
        return new AuthenticationDTO.Account(account.getId(), account.getUsername());
    }
}

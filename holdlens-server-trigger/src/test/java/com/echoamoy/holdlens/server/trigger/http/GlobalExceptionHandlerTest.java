package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.auth.AuthenticationFailedException;
import com.echoamoy.holdlens.server.cases.auth.UsernameOccupiedException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class GlobalExceptionHandlerTest {

    @Test
    public void authenticationFailuresUseStableUnauthorizedResponse() {
        ResponseEntity<Response<Void>> response = new GlobalExceptionHandler()
                .handleAuthenticationFailedException(new AuthenticationFailedException());

        Assert.assertEquals(401, response.getStatusCode().value());
        Assert.assertEquals("0401", response.getBody().getCode());
    }

    @Test
    public void duplicateUsernameUsesConflictResponse() {
        ResponseEntity<Response<Void>> response = new GlobalExceptionHandler()
                .handleUsernameOccupiedException(new UsernameOccupiedException());

        Assert.assertEquals(409, response.getStatusCode().value());
        Assert.assertEquals("0409", response.getBody().getCode());
    }
}

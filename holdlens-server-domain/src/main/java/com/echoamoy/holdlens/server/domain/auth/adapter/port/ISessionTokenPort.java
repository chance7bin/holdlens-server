package com.echoamoy.holdlens.server.domain.auth.adapter.port;

import com.echoamoy.holdlens.server.domain.auth.model.valobj.IssuedSessionTokenVO;

public interface ISessionTokenPort {

    IssuedSessionTokenVO issue();

    String hash(String rawToken);
}

package com.zerocode.deploy.executor;

import java.io.IOException;

public interface GithubActionsClient {

    GithubActionsDispatchResult dispatch(
            GithubActionsDispatchCommand command) throws IOException, InterruptedException;
}

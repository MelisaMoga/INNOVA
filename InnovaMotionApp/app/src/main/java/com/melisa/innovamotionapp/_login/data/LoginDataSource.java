package com.melisa.innovamotionapp._login.data;

import com.melisa.innovamotionapp._login.data.model.LoggedInUser;

import java.io.IOException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<LoggedInUser> login(String username, String password) {

        try {
            // This legacy email/password flow is deprecated in favor of Google Sign-In.
            // Keep a fake success to preserve sample behavior if used.
            LoggedInUser placeholderUser = new LoggedInUser(
                            java.util.UUID.randomUUID().toString(),
                            username);
            return new Result.Success<>(placeholderUser);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}
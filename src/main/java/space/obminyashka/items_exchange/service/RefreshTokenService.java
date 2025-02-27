package space.obminyashka.items_exchange.service;

import space.obminyashka.items_exchange.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {

    /**
     * Creates a {@link RefreshToken} by logged-in User`s username
     *
     * @param username is a username of the logged-in User
     * @return the created {@link RefreshToken}
     */
    RefreshToken createRefreshToken(String username);

    /**
     * Removes a {@link RefreshToken} by logged-in User`s username
     *
     * @param username is a username of the logged-in User
     */
    void deleteByUsername(String username);

    /**
     * Regenerates a new Access JWT Token by Refresh token
     *
     * @param refreshToken is a value of {@link RefreshToken#getToken()}
     * @return an Optional {@link String} as a new Access JWT Token
     * if Refresh Token exists in the database and not expired, {@link Optional#empty()} otherwise
     */
    Optional<String> renewAccessTokenByRefresh(String refreshToken);
}

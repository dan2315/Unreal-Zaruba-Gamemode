package com.dod.unrealzaruba.Pricing;

import java.util.concurrent.TimeUnit;

import dod.unrealfrontiers.ufessential.api.UFHttpClient;

/**
 * Fetches the rules as the logged-in player, for a game that has no server API key (single player, or a player's own
 * world). Kept in its own class so the client-only Essential classes are touched only when this is called, which
 * {@link RulesService} does on the client side only.
 */
final class ClientRulesFetch {
    private ClientRulesFetch() {}

    /** The JSON body, or null if the player is not logged in or the backend refused. */
    static String fetch() throws Exception {
        if (!UFHttpClient.isLoggedIn()) return null;
        UFHttpClient.Response response = UFHttpClient.get("/schematics/rules").get(15, TimeUnit.SECONDS);
        return response.isSuccess() ? response.body() : null;
    }
}

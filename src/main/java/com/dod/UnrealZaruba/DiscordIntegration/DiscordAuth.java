package com.dod.UnrealZaruba.DiscordIntegration;

import java.io.IOException;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;


public class DiscordAuth {
    public static Set<String> unresolvedRequests = new HashSet<String>();
    public static String backendEndpoint = "http://34.34.44.237:3000";

    public static void OpenAuthPage(String state, UUID playerUuid, String minecraft_username, Integer port) {
            try {
                String authUrl = backendEndpoint 
                + "/auth?state=" + state 
                + "&player_uuid=" + playerUuid 
                + "&minecraft_username=" + minecraft_username
                + "&port=" + port;
                openURL(authUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static void openURL(String url) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[] { "open", url });
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[] { "xdg-open", url });
            } else {
                throw new UnsupportedOperationException("Unsupported operating system");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

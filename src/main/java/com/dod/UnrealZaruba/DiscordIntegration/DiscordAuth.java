package com.dod.UnrealZaruba.DiscordIntegration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DiscordAuth {
    public static String webServiceAddress = "";
    private static void openWebpage(String urlString) {
        try {
            URI uri = new URI("");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
            } else {
                System.err.println("Desktop is not supported. Cannot open URL.");
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}

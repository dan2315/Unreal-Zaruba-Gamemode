package com.dod.unrealzaruba.Schematics;

import java.util.ArrayList;
import java.util.List;

/** A schematic as the backend returns it (SchematicDto in SchematicEndpoints.cs). Filled by Gson. */
public class SchematicDto {
    public long id;
    public String name = "";
    public String description;
    public String author = "";
    public boolean isPublic;
    /** Average score, null without ratings. */
    public Double rating;
    public int ratingsCount;
    /** Only set when the request carried a token. */
    public Integer myRating;
    public int battlesCount;
    public long fileSize;
    public String sha256;
    public String createdAt;
    public String updatedAt;
    /** Price in game dollars, computed by the backend; null when the file could not be read as a structure. */
    public Double price;
    /** Cover image path relative to the backend, or null. */
    public String previewUrl;
    /** Only in GET /schematics/{id} and POST /schematics responses. */
    public List<Image> images;

    public static class Image {
        public long id;
        public String url;
    }

    public static class Page {
        public List<SchematicDto> items = new ArrayList<>();
        public int total;
        public int page = 1;
        public int pageSize = 20;

        public int pageCount() {
            return Math.max(1, (total + pageSize - 1) / Math.max(1, pageSize));
        }
    }
}

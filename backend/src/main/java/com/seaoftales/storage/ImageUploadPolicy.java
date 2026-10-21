package com.seaoftales.storage;

public enum ImageUploadPolicy {
    PROFILE_PICTURE(1_048_576L, 1_024, 1_024, 1_000_000),
    NOVEL_COVER(5L * 1_048_576L, 0, 0, 0);

    private final long maxBytes; private final int maxWidth; private final int maxHeight; private final long maxPixels;
    ImageUploadPolicy(long maxBytes, int maxWidth, int maxHeight, long maxPixels) { this.maxBytes=maxBytes; this.maxWidth=maxWidth; this.maxHeight=maxHeight; this.maxPixels=maxPixels; }
    public long maxBytes(){return maxBytes;} public int maxWidth(){return maxWidth;} public int maxHeight(){return maxHeight;} public long maxPixels(){return maxPixels;}
}

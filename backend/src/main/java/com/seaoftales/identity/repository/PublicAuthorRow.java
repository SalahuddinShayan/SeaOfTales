package com.seaoftales.identity.repository;

public interface PublicAuthorRow {
    String getUsername();
    String getProfilePicture();
    Long getPublicNovelCount();
    Long getNovelId();
    String getTitle();
    String getStatus();
    String getSourceLanguage();
    String getDescription();
    String getCoverImage();
}

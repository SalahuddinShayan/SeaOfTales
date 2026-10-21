package com.seaoftales.knowledge.entity;

import com.seaoftales.content.enums.Language;
import com.seaoftales.knowledge.enums.TerminologyGender;
import com.seaoftales.knowledge.enums.NameStyle;
import com.seaoftales.knowledge.enums.TerminologyScope;
import com.seaoftales.knowledge.enums.TerminologyType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "terminology")
public class Terminology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TerminologyScope scope;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Language language;

    private Long novelId;

    @Column(nullable = false, length = 255)
    private String sourceTerm;

    @Column(nullable = false, length = 255)
    private String translatedTerm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TerminologyType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NameStyle nameStyle;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TerminologyGender gender;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public Terminology() {
    }

    public Long getId() {
        return id;
    }

    public TerminologyScope getScope() {
        return scope;
    }

    public void setScope(TerminologyScope scope) {
        this.scope = scope;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getSourceTerm() {
        return sourceTerm;
    }

    public void setSourceTerm(String sourceTerm) {
        this.sourceTerm = sourceTerm;
    }

    public String getTranslatedTerm() {
        return translatedTerm;
    }

    public void setTranslatedTerm(String translatedTerm) {
        this.translatedTerm = translatedTerm;
    }

    public TerminologyType getType() {
        return type;
    }

    public void setType(TerminologyType type) {
        this.type = type;
    }

    public NameStyle getNameStyle() {
        return nameStyle;
    }

    public void setNameStyle(NameStyle nameStyle) {
        this.nameStyle = nameStyle;
    }

    public TerminologyGender getGender() {
        return gender;
    }

    public void setGender(TerminologyGender gender) {
        this.gender = gender;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

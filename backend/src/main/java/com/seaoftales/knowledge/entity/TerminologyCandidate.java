package com.seaoftales.knowledge.entity;

import com.seaoftales.knowledge.enums.TerminologyCandidateStatus;
import com.seaoftales.knowledge.enums.TerminologyGender;
import com.seaoftales.knowledge.enums.TerminologyType;
import com.seaoftales.knowledge.enums.NameStyle;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "terminology_candidates")
public class TerminologyCandidate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long chapterId;
    @Column(nullable = false, length = 100) private String chapterNumber;
    @Column(nullable = false, length = 255) private String sourceTerm;
    @Column(nullable = false, length = 255) private String translatedTerm;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TerminologyType type;
    @Enumerated(EnumType.STRING) @Column(length = 20) private TerminologyGender gender;
    @Enumerated(EnumType.STRING) @Column(length = 20) private NameStyle nameStyle;
    @Column(columnDefinition = "TEXT") private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TerminologyCandidateStatus status = TerminologyCandidateStatus.UNREVIEWED;
    @CreationTimestamp @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant reviewedAt;

    public TerminologyCandidate() { }
    public Long getId() { return id; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long value) { chapterId = value; }
    public String getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(String value) { chapterNumber = value; }
    public String getSourceTerm() { return sourceTerm; }
    public void setSourceTerm(String value) { sourceTerm = value; }
    public String getTranslatedTerm() { return translatedTerm; }
    public void setTranslatedTerm(String value) { translatedTerm = value; }
    public TerminologyType getType() { return type; }
    public void setType(TerminologyType value) { type = value; }
    public TerminologyGender getGender() { return gender; }
    public void setGender(TerminologyGender value) { gender = value; }
    public NameStyle getNameStyle() { return nameStyle; }
    public void setNameStyle(NameStyle value) { nameStyle = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public TerminologyCandidateStatus getStatus() { return status; }
    public void setStatus(TerminologyCandidateStatus value) { status = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant value) { reviewedAt = value; }
}

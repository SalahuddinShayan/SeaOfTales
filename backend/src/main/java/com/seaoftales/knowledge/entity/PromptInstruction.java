package com.seaoftales.knowledge.entity;

import com.seaoftales.content.enums.Language;
import com.seaoftales.knowledge.enums.PromptInstructionScope;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "prompt_instructions")
public class PromptInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromptInstructionScope scope;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Language language;

    private Long novelId;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String instruction;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public PromptInstruction() {
    }

    public Long getId() {
        return id;
    }

    public PromptInstructionScope getScope() {
        return scope;
    }

    public void setScope(PromptInstructionScope scope) {
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

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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
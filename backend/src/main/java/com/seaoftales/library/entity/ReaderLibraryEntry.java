package com.seaoftales.library.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
@Entity @Table(name="reader_library", uniqueConstraints=@UniqueConstraint(name="uk_reader_library_user_novel", columnNames={"user_id","novel_id"}))
public class ReaderLibraryEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="user_id", nullable=false) private Long userId;
 @Column(name="novel_id", nullable=false) private Long novelId;
 @CreationTimestamp @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
 protected ReaderLibraryEntry() {}
 public ReaderLibraryEntry(Long userId, Long novelId){this.userId=userId;this.novelId=novelId;}
 public Long getId(){return id;} public Long getUserId(){return userId;} public Long getNovelId(){return novelId;} public Instant getCreatedAt(){return createdAt;}
}

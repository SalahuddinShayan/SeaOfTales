package com.seaoftales.library.repository;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.library.entity.ReaderLibraryEntry;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ReaderLibraryRepository extends JpaRepository<ReaderLibraryEntry,Long>{
 boolean existsByUserIdAndNovelId(Long userId,Long novelId);
 void deleteByUserIdAndNovelId(Long userId,Long novelId);
 @Query("select n from ReaderLibraryEntry e, Novel n where e.userId=:userId and e.novelId=n.id order by e.createdAt desc, e.id desc")
 List<Novel> findSavedNovels(@Param("userId") Long userId);
}

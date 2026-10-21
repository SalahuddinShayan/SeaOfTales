package com.seaoftales.library.service;
import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.identity.entity.User;
import com.seaoftales.identity.repository.UserRepository;
import com.seaoftales.library.dto.LibraryNovelResponse;
import com.seaoftales.library.entity.ReaderLibraryEntry;
import com.seaoftales.library.repository.ReaderLibraryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class ReaderLibraryService{
 private final ReaderLibraryRepository library; private final UserRepository users; private final NovelRepository novels;
 public ReaderLibraryService(ReaderLibraryRepository library,UserRepository users,NovelRepository novels){this.library=library;this.users=users;this.novels=novels;}
 private User user(String username){return users.findByUsername(username).orElseThrow(()->new IllegalArgumentException("User not found."));}
 @Transactional public void save(String username,Long novelId){User u=user(username);if(!novels.existsById(novelId))throw new IllegalArgumentException("Novel not found.");if(!library.existsByUserIdAndNovelId(u.getId(),novelId))library.save(new ReaderLibraryEntry(u.getId(),novelId));}
 @Transactional public void remove(String username,Long novelId){library.deleteByUserIdAndNovelId(user(username).getId(),novelId);}
 @Transactional(readOnly=true) public List<LibraryNovelResponse> list(String username){return library.findSavedNovels(user(username).getId()).stream().map(this::toResponse).toList();}
 @Transactional(readOnly=true) public boolean contains(String username,Long novelId){return library.existsByUserIdAndNovelId(user(username).getId(),novelId);}
 private LibraryNovelResponse toResponse(Novel n){String cover=n.getCoverImage()==null||n.getCoverImage().isBlank()?null:"/api/novels/"+n.getId()+"/cover";return new LibraryNovelResponse(n.getId(),n.getTitle(),n.getStatus().name(),n.getSourceLanguage().name(),n.getDescription(),cover);}
}

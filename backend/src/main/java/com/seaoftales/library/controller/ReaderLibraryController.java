package com.seaoftales.library.controller;
import com.seaoftales.library.dto.LibraryNovelResponse;
import com.seaoftales.library.service.ReaderLibraryService;
import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/library") public class ReaderLibraryController{
 private final ReaderLibraryService service; public ReaderLibraryController(ReaderLibraryService service){this.service=service;}
 @GetMapping public List<LibraryNovelResponse> list(Authentication a){return service.list(a.getName());}
 @PostMapping("/novels/{novelId}") public ResponseEntity<Void> save(@PathVariable Long novelId,Authentication a){try{service.save(a.getName(),novelId);return ResponseEntity.noContent().build();}catch(IllegalArgumentException e){return ResponseEntity.notFound().build();}}
 @DeleteMapping("/novels/{novelId}") public ResponseEntity<Void> remove(@PathVariable Long novelId,Authentication a){service.remove(a.getName(),novelId);return ResponseEntity.noContent().build();}
 @GetMapping("/novels/{novelId}") public boolean contains(@PathVariable Long novelId,Authentication a){return service.contains(a.getName(),novelId);}
}

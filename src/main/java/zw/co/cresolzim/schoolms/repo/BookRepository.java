package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import zw.co.cresolzim.schoolms.domain.*;
import zw.co.cresolzim.schoolms.domain.Enums.*;
import java.time.*;
import java.util.*;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    @Query("""
           select b from Book b
           where lower(b.title)  like lower(concat('%', :q, '%'))
              or lower(b.author) like lower(concat('%', :q, '%'))
              or b.isbn          like concat('%', :q, '%')
              or lower(b.classification) like lower(concat('%', :q, '%'))
           order by b.title
           """)
    List<Book> search(@Param("q") String q);

    List<Book> findByCategoryOrderByTitleAsc(String category);

    @Query("select distinct b.category from Book b where b.category is not null order by b.category")
    List<String> allCategories();
}

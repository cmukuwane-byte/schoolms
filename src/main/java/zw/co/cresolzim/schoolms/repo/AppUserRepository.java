package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import zw.co.cresolzim.schoolms.domain.AppUser;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Query("select u from AppUser u left join fetch u.roles where u.username = :username")
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<AppUser> findByStaffId(Long staffId);

    Optional<AppUser> findByGuardianId(Long guardianId);
}

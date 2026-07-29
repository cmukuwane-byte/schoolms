package zw.co.cresolzim.schoolms.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.cresolzim.schoolms.domain.Role;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}

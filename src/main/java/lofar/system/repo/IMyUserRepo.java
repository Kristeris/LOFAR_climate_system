package lofar.system.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lofar.system.model.MyUser;

@Repository
public interface IMyUserRepo extends JpaRepository<MyUser, Long> {
    Optional<MyUser> findByUsername(String username);
    Optional<MyUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
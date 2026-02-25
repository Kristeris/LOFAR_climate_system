package lofar.system.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import lofar.system.model.MyAuthority;

@Repository
public interface IMyAuthorityRepo extends JpaRepository<MyAuthority, Long> {
}





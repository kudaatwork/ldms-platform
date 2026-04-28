package projectlx.user.management.repository;

import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
    Optional<UserAccount> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);
    Optional<UserAccount> findByPhoneNumberAndEntityStatusNot(String phoneNumber, EntityStatus entityStatus);
    Optional<UserAccount> findByPhoneNumber(String phoneNumber);
    Optional<UserAccount> findByAccountNumberAndEntityStatusNot(String accountNumber, EntityStatus entityStatus);
    List<UserAccount> findByEntityStatusNot(EntityStatus entityStatus);
}

package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);
    long countByRoleAndEnabledTrue(UserRole role);

    @Query("""
            select user
            from User user
            where :query = ''
            or lower(user.name)
                    like lower(concat('%', :query, '%'))
            or lower(user.email)
                    like lower(concat('%', :query, '%'))
            or user.phoneNumber
                    like concat('%', :query, '%')
            """)
    Page<User> search(
            @Param("query") String query,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}

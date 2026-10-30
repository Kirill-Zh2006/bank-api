package com.bank.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.demo.entity.CustomerAccount;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long>{
    
    Optional<CustomerAccount> findByAccountFrom(String accountFrom);
}

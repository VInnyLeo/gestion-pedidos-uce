package com.pedidosuce.customer_service.repository;

import com.pedidosuce.customer_service.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
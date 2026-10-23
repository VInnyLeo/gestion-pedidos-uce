package com.pedidosuce.customer_service.service;

import java.util.List;

import com.pedidosuce.customer_service.dto.CustomerRequest;
import com.pedidosuce.customer_service.exception.ResourceNotFoundException;
import com.pedidosuce.customer_service.model.Customer;
import com.pedidosuce.customer_service.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el cliente con id " + id));
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        Customer customer = new Customer(
                request.name(), request.email(), request.phone(), request.address());
        return repository.save(customer);
    }

    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = findById(id);
        customer.update(request.name(), request.email(), request.phone(), request.address());
        return repository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findById(id);
        repository.delete(customer);
    }
}
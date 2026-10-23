package com.pedidosuce.product_service.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.pedidosuce.product_service.dto.DecreaseStockRequest;
import com.pedidosuce.product_service.dto.ProductRequest;
import com.pedidosuce.product_service.dto.StockItemRequest;
import com.pedidosuce.product_service.exception.InsufficientStockException;
import com.pedidosuce.product_service.exception.ResourceNotFoundException;
import com.pedidosuce.product_service.model.Product;
import com.pedidosuce.product_service.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto con id " + id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product product = new Product(
                request.name(), request.description(), request.price(), request.stock());
        return repository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        product.update(request.name(), request.description(), request.price(), request.stock());
        return repository.save(product);
    }

    @Transactional
    public List<Product> decreaseStock(DecreaseStockRequest request) {
        // Ordenar por id evita que dos pedidos se bloqueen mutuamente esperando el candado del otro
        List<StockItemRequest> items = request.items().stream()
                .sorted(Comparator.comparing(StockItemRequest::productId))
                .toList();

        List<Product> updated = new ArrayList<>();
        for (StockItemRequest item : items) {
            Product product = repository.findByIdForUpdate(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No existe el producto con id " + item.productId()));

            if (product.getStock() < item.quantity()) {
                throw new InsufficientStockException(
                        "Stock insuficiente para '" + product.getName() + "': disponible "
                                + product.getStock() + ", solicitado " + item.quantity());
            }

            product.decreaseStock(item.quantity());
            updated.add(product);
        }
        return updated;
    }
}
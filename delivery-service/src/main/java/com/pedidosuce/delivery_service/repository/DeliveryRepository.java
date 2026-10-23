package com.pedidosuce.delivery_service.repository;

import com.pedidosuce.delivery_service.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}

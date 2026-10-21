package com.indra.retail.manager;

import com.indra.retail.entities.Customer;
import com.indra.retail.entities.Order;
import com.indra.retail.entities.OrderItem;
import com.indra.retail.entities.Product;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LazyLoadingTest {

    @Autowired
    private EntityManagerRecordManager manager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long orderId;
    private Long productId;

    @BeforeEach
    void createOrderWithItems() {
        Long[] ids = transactionTemplate.execute(status -> {
            Customer customer = manager.create(new Customer("Carla", UUID.randomUUID() + "@indra.com"));
            Product product = manager.create(new Product("Laptop", new BigDecimal("1500.00"), 4));
            Order order = new Order(customer);
            order.addItem(new OrderItem(product, 1, product.getPrice()));
            Long id = manager.create(order).getId();
            return new Long[] {id, product.getId()};
        });
        orderId = ids[0];
        productId = ids[1];
    }

    @Test
    @DisplayName("Acceder a items fuera de la transacción lanza LazyInitializationException")
    void shouldThrowLazyInitializationExceptionOutsideTransaction() {
        Order order = manager.findById(Order.class, orderId).orElseThrow();

        assertThrows(LazyInitializationException.class, () -> order.getItems().size());
    }

    @Test
    @DisplayName("Acceder a items dentro de una transacción activa funciona")
    void shouldLoadItemsInsideTransaction() {
        int itemCount = transactionTemplate.execute(status -> {
            Order order = manager.findById(Order.class, orderId).orElseThrow();
            return order.getItems().size();
        });

        assertEquals(1, itemCount);
    }

    @Test
    @DisplayName("Customer de un Order no se carga hasta que se usa (LAZY)")
    void shouldLoadCustomerLazily() {
        Order order = manager.findById(Order.class, orderId).orElseThrow();

        assertThrows(LazyInitializationException.class, () -> order.getCustomer().getName());

        String customerName = transactionTemplate.execute(status -> {
            Order managed = manager.findById(Order.class, orderId).orElseThrow();
            return managed.getCustomer().getName();
        });
        assertEquals("Carla", customerName);
    }

    @Test
    @DisplayName("Quitar un ítem de la lista elimina la fila (orphanRemoval)")
    void shouldRemoveOrphanItem() {
        transactionTemplate.execute(status -> {
            Order order = manager.findById(Order.class, orderId).orElseThrow();
            OrderItem item = order.getItems().get(0);
            order.removeItem(item);
            manager.update(order);
            return null;
        });

        boolean stillExists = transactionTemplate.execute(status -> {
            Order order = manager.findById(Order.class, orderId).orElseThrow();
            return !order.getItems().isEmpty();
        });

        assertFalse(stillExists, "El OrderItem debería haberse eliminado de la BD (orphanRemoval)");
    }

    @Test
    @DisplayName("No se puede guardar un OrderItem sin Order")
    void shouldRejectOrphanOrderItem() {
        assertThrows(Exception.class, () -> transactionTemplate.execute(status -> {
            Product product = manager.findById(Product.class, productId).orElseThrow();
            OrderItem orphan = new OrderItem(product, 1, product.getPrice());
            manager.create(orphan);
            status.flush();
            return null;
        }));
    }

    @Test
    @DisplayName("Borrar un OrderItem no borra el Product")
    void shouldNotDeleteProductWhenDeletingItem() {
        transactionTemplate.execute(status -> {
            Order order = manager.findById(Order.class, orderId).orElseThrow();
            OrderItem item = order.getItems().get(0);
            manager.delete(OrderItem.class, item.getId());
            return null;
        });

        assertTrue(manager.findById(Product.class, productId).isPresent(),
                "El Product no debe eliminarse al borrar el OrderItem (sin cascade)");
    }
}
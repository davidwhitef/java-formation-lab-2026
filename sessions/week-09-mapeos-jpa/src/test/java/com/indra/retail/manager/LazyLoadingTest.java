package com.indra.retail.manager;

import com.indra.retail.entities.Address;
import com.indra.retail.entities.Customer;
import com.indra.retail.entities.Order;
import com.indra.retail.entities.OrderItem;
import com.indra.retail.entities.Product;
import org.hibernate.Hibernate;
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

    private static Address bogotaAddress() {
        return new Address("Autopista Sur # 45 25", "Bogota", "Colombia");
    }

    @BeforeEach
    void createOrderWithItems() {        
        orderId = transactionTemplate.execute(status -> {
            Customer customer = manager.create(new Customer("Carla", UUID.randomUUID() + "@indra.com", bogotaAddress()));
            Product product = manager.create(new Product("Laptop", new BigDecimal("1500.00"), 4));
            Order order = new Order(customer);
            order.addItem(new OrderItem(product, 1, product.getPrice()));
            return manager.create(order).getId();
        });
    }

    @Test
    @DisplayName("Acceder a items fuera de la transacción lanza LazyInitializationException")
    void shouldThrowLazyInitializationExceptionOutsideTransaction() {
        // findById abre y cierra su transacción: la Order sale DETACHED
        Order order = manager.findById(Order.class, orderId).orElseThrow(); //Una transaccion que abre y cierra

        assertThrows(LazyInitializationException.class, () -> order.getItems().size()); //order.getItems() es LAZY, y la transac cion ya se cerro, por lo que no puede cargar los items
    }

    @Test
    @DisplayName("Acceder a items dentro de una transacción activa funciona")
    void shouldLoadItemsInsideTransaction() {
        int itemCount = transactionTemplate.execute(status -> {
            Order order = manager.findById(Order.class, orderId).orElseThrow();
            System.out.println("OrderId: " + order.getId() + ", ítems en la orden: " + order.getItems().size());
            return order.getItems().size();
        });

        assertEquals(1, itemCount);
    }

    // ───────────────────────── Retos para participantes ─────────────────────────

    @Test
    @DisplayName("Customer de un Order no se carga hasta que se usa (LAZY)")
    void shouldLoadCustomerLazily() {
        transactionTemplate.executeWithoutResult(status -> {
            Order order = manager.findById(Order.class, orderId).orElseThrow();

            // Al cargar la Order, el customer es un proxy sin inicializar
            assertFalse(Hibernate.isInitialized(order.getCustomer()));

            // Usarlo dispara su SELECT
            System.out.println("Customer: " + order.getCustomer().getName());
            assertTrue(Hibernate.isInitialized(order.getCustomer()));
        });
    }

    @Test
    @DisplayName("Quitar un ítem de la lista elimina la fila (orphanRemoval)")
    void shouldRemoveOrphanItem() {
        // Dentro de una transacción: la Order está managed, así que Hibernate
        // detecta el cambio en la colección al hacer commit
        Long removedItemId = transactionTemplate.execute(status -> {
            Order order = manager.findById(Order.class, orderId).orElseThrow();
            OrderItem item = order.getItems().get(0);
            System.out.println("Quitando OrderItem id: " + item.getId() + " de la Order id: " + order.getId());
            order.removeItem(item);
            return item.getId();
        });

        // La fila ya no existe en la base de datos
        assertTrue(manager.findById(OrderItem.class, removedItemId).isEmpty());

        int itemCount = transactionTemplate.execute(status ->
                manager.findById(Order.class, orderId).orElseThrow().getItems().size());
        System.out.println("Ítems que quedan en la Order: " + itemCount);
        assertEquals(0, itemCount);
    }

    @Test
    @DisplayName("Borrar un OrderItem no borra el Product")
    void shouldNotDeleteProductWhenDeletingItem() {
        // Ids del ítem y de su producto (product es LAZY: getId() no dispara consulta)
        Long[] ids = transactionTemplate.execute(status -> {
            OrderItem item = manager.findById(Order.class, orderId).orElseThrow().getItems().get(0);
            return new Long[] {item.getId(), item.getProduct().getId()};
        });
        System.out.println("Borrando OrderItem id: " + ids[0] + " con Product id: " + ids[1]);

        assertTrue(manager.delete(OrderItem.class, ids[0]));

        // Sin cascade en OrderItem -> Product, el producto sigue en la BD
        assertTrue(manager.findById(OrderItem.class, ids[0]).isEmpty());
        Product product = manager.findById(Product.class, ids[1]).orElseThrow();
        System.out.println("Product id: " + product.getId() + " sigue existiendo: " + product.getName());
    }
}

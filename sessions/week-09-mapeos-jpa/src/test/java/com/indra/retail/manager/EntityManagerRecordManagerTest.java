package com.indra.retail.manager;

import com.indra.retail.entities.Address;
import com.indra.retail.entities.Customer;
import com.indra.retail.entities.Order;
import com.indra.retail.entities.OrderItem;
import com.indra.retail.enums.OrderStatus;
import com.indra.retail.entities.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRUD básico (crear, consultar, actualizar, eliminar) con EntityManager.
 * Cada test usa datos únicos, así que no dependen del orden de ejecución.
 */
@SpringBootTest
class EntityManagerRecordManagerTest {

    @Autowired
    private EntityManagerRecordManager manager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static String uniqueEmail() {
        String alias = ThreadLocalRandom.current().ints(8, 'a', 'z' + 1)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return alias + "@indracompany.com";
    }

    private static Address bogotaAddress() {
        return new Address("Autopista Sur", "Bogota", "Colombia");
    }

    @Test
    @DisplayName("Crea un Customer y lo consulta por id")
    void shouldCreateAndFindCustomer() {
        Customer saved = manager.create(new Customer("Ana", uniqueEmail(), bogotaAddress()));

        assertNotNull(saved.getId());
        Customer found = manager.findById(Customer.class, saved.getId()).orElseThrow();
        assertEquals("Ana", found.getName());
    }

    @Test
    @DisplayName("No permite dos Customer con el mismo email")
    void shouldRejectDuplicateCustomerEmail() {
        String email = uniqueEmail();
        Customer customer1 = new Customer("Pepito", email, bogotaAddress());
        System.out.println("Creando Customer 1: " + customer1.getName() + ", email: " + customer1.getEmail());
        manager.create(customer1);

        Customer customer2 = new Customer("Juanchito", email, bogotaAddress());
        System.out.println("Creando Customer 2: " + customer2.getName() + ", email: " + customer2.getEmail());
        assertThrows(DataIntegrityViolationException.class,
                () -> manager.create(customer2));
    }

    @Test
    @DisplayName("No permite guardar un Customer con name o email nulos")
    void shouldRejectCustomerWithNullFields() {
        Customer customer1 = new Customer(null, uniqueEmail(), bogotaAddress());
        System.out.println("Creando Customer 1: " + customer1.getName() + ", email: " + customer1.getEmail());
        
        Customer customer2 = new Customer("Alejandro", null, bogotaAddress());
        System.out.println("Creando Customer 2: " + customer2.getName() + ", email: " + customer2.getEmail());
        
        var ex = assertThrows(DataIntegrityViolationException.class,
                () -> manager.create(customer1));
        System.out.println(ex.getMostSpecificCause().getMessage());

        ex = assertThrows(DataIntegrityViolationException.class,
                () -> manager.create(customer2));
        System.out.println(ex.getMostSpecificCause().getMessage());
    }

    @Test
    @DisplayName("No se puede guardar un OrderItem sin Order")
    void shouldRejectOrphanOrderItem() {
        Product product = manager.create(new Product("Mouse", new BigDecimal("20000.00"), 5));
        // Nunca se agrega a un Order con addItem: order queda en null
        OrderItem orphan = new OrderItem(product, 1, product.getPrice());
        System.out.println("Creando OrderItem huérfano, order: " + orphan.getOrder());

        var ex = assertThrows(DataIntegrityViolationException.class,
                () -> manager.create(orphan));
        System.out.println(ex.getMostSpecificCause().getMessage());
    }

    @Test
    @DisplayName("Actualiza un Product")
    void shouldUpdateProduct() {
        Product saved = manager.create(new Product("Teclado", new BigDecimal("50.00"), 10));

        saved.setStock(7);
        manager.update(saved);

        Product found = manager.findById(Product.class, saved.getId()).orElseThrow();
        assertEquals(7, found.getStock());
    }

    @Test
    @DisplayName("Rechaza actualizar un Product con una versión desactualizada (@Version)")
    void shouldRejectStaleProductUpdate() {
        Product saved = manager.create(new Product("Monitor", new BigDecimal("300.00"), 10));

        // Dos usuarios leen el mismo producto: ambos con la misma versión
        Product userA = manager.findById(Product.class, saved.getId()).orElseThrow();
        Product userB = manager.findById(Product.class, saved.getId()).orElseThrow();
        System.out.println("Usuario A lee version: " + userA.getVersion() + ", Usuario B lee version: " + userB.getVersion());

        // A guarda primero: la versión en la base se incrementa
        userA.setStock(7);
        Product updatedA = manager.update(userA);
        System.out.println("Usuario A guarda, nueva version: " + updatedA.getVersion());

        // B guarda con la versión vieja: el UPDATE no encuentra la fila y falla
        userB.setStock(8);
        System.out.println("Usuario B intenta guardar con version: " + userB.getVersion());
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> manager.update(userB));

        // El cambio de A se conserva
        Product found = manager.findById(Product.class, saved.getId()).orElseThrow();
        assertEquals(7, found.getStock());
    }

    @Test
    @DisplayName("Elimina un Customer")
    void shouldDeleteCustomer() {
        Customer saved = manager.create(new Customer("Luis", uniqueEmail(), bogotaAddress()));

        assertTrue(manager.delete(Customer.class, saved.getId()));

        assertFalse(manager.findById(Customer.class, saved.getId()).isPresent());
    }

    @Test
    @DisplayName("Devuelve false al eliminar un registro inexistente")
    void shouldReturnFalseWhenDeletingMissingRecord() {
        assertFalse(manager.delete(Customer.class, Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Lista los Product creados")
    void shouldListProducts() {
        Product saved = manager.create(new Product("Mouse", new BigDecimal("20.00"), 5));

        assertTrue(manager.findAll(Product.class).stream()
                .anyMatch(p -> p.getId().equals(saved.getId())));
    }

    @Test
    @DisplayName("Persiste un Order con sus ítems en cascada")
    void shouldPersistOrderWithItems() {
        // Todo en una transacción: las entidades quedan managed y el test no
        // depende de qué cascade tenga el mapeo.
        Long[] ids = transactionTemplate.execute(status -> {
            Customer customer = manager.create(new Customer("Marta", uniqueEmail(), bogotaAddress()));
            Product product = manager.create(new Product("Monitor", new BigDecimal("300.00"), 3));
            Order order = new Order(customer);
            order.addItem(new OrderItem(product, 2, product.getPrice()));
            manager.create(order);
            return new Long[] {order.getId(), customer.getId()};
        });

        Order found = manager.findById(Order.class, ids[0]).orElseThrow();
        assertEquals(OrderStatus.PENDING, found.getStatus());
        assertEquals(ids[1], found.getCustomer().getId());
    }
}

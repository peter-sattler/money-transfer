package net.sattler22.transfer.bootstrap.loader;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.sattler22.transfer.domain.Bank;
import net.sattler22.transfer.domain.Customer;

/**
 * Bootstrap Customer Data Loader
 *
 * @author Pete Sattler
 * @version September 2019
 */
public final class CustomerDataLoader extends BaseDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerDataLoader.class);
    private final Bank bank;

    /**
     * Constructs a new customer data loader
     */
    public CustomerDataLoader(Bank bank, String resourceName) {
        super(resourceName);
        this.bank = bank;
    }

    @Override
    public int load() throws IOException {
        final File inputFile = new File(resource.getFile());
        final TypeReference<List<Customer>> typeRef =
            new TypeReference<List<Customer>>() {};
        final List<Customer> customers = objectMapper.readValue(inputFile, typeRef);
        for (Customer customer : customers) {
            bank.addCustomer(customer);
            LOGGER.info("Added {}", customer);
        }
        return customers.size();
    }

    @Override
    public String toString() {
        return String.format("%s [resource=%s, bank=%s]", getClass().getSimpleName(), resource, bank);
    }
}

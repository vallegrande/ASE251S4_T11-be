package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.service.impl.CustomerServiceImpl;
import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.repository.mongo.StaffUserRepository;
import jakarta.validation.Validator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceImplTest {

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final Validator validator = mock(Validator.class);
    private final CustomerServiceImpl customerService = new CustomerServiceImpl(
            customerRepository,
            mock(StaffUserRepository.class),
            mock(SequenceGeneratorService.class),
            mock(PasswordEncoder.class),
            validator,
            new CustomerImportMapper(validator),
            new CustomerExportService(customerRepository, new CustomerExportMapper()));

    @Test
    void rejectsUnsupportedDocumentType() {
        Customer customer = customer("PASAPORTE", "123456789");

        StepVerifier.create(customerService.create(customer))
                .expectErrorMatches(error -> error instanceof ResponseStatusException exception
                        && exception.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void rejectsDocumentNumberWithWrongLengthForType() {
        Customer customer = customer("DNI", "123456789");

        StepVerifier.create(customerService.create(customer))
                .expectErrorMatches(error -> error instanceof ResponseStatusException exception
                        && exception.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void exportsExcelWithActiveInactiveAndAllCustomerFilters() {
        List<Customer> activeCustomers = customers(true, 10);
        List<Customer> inactiveCustomers = customers(false, 10);
        when(customerRepository.findByIsActive(true)).thenReturn(Flux.fromIterable(activeCustomers));
        when(customerRepository.findByIsActive(false)).thenReturn(Flux.fromIterable(inactiveCustomers));
        when(customerRepository.findAll()).thenReturn(
                Flux.concat(Flux.fromIterable(activeCustomers), Flux.fromIterable(inactiveCustomers)));

        assertExcelRowCount(customerService.exportCustomersExcel(true), 10);
        assertExcelRowCount(customerService.exportCustomersExcel(false), 10);
        assertExcelRowCount(customerService.exportCustomersExcel(), 20);

        verify(customerRepository).findByIsActive(true);
        verify(customerRepository).findByIsActive(false);
        verify(customerRepository).findAll();
    }

    @Test
    void exportsFilteredCustomersAsPdf() {
        List<Customer> activeCustomers = customers(true, 10);
        List<Customer> inactiveCustomers = customers(false, 10);
        when(customerRepository.findByIsActive(true)).thenReturn(Flux.fromIterable(activeCustomers));
        when(customerRepository.findByIsActive(false)).thenReturn(Flux.fromIterable(inactiveCustomers));
        when(customerRepository.findAll()).thenReturn(
                Flux.concat(Flux.fromIterable(activeCustomers), Flux.fromIterable(inactiveCustomers)));

        assertPdfRecordCount(customerService.exportCustomersPdf(true), 10);
        assertPdfRecordCount(customerService.exportCustomersPdf(false), 10);
        assertPdfRecordCount(customerService.exportCustomersPdf(), 20);

        verify(customerRepository).findByIsActive(true);
        verify(customerRepository).findByIsActive(false);
        verify(customerRepository).findAll();
    }

    private void assertExcelRowCount(Mono<byte[]> export, int expectedCustomerCount) {
        StepVerifier.create(export)
                .assertNext(bytes -> assertEquals(expectedCustomerCount, exportedDataRowCount(bytes)))
                .verifyComplete();
    }

    private int exportedDataRowCount(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            return workbook.getSheetAt(0).getPhysicalNumberOfRows() - 1;
        } catch (IOException exception) {
            throw new AssertionError("El archivo generado no es un Excel válido", exception);
        }
    }

    private void assertPdfRecordCount(Mono<byte[]> export, int expectedCustomerCount) {
        StepVerifier.create(export)
                .assertNext(bytes -> assertTrue(pdfText(bytes).contains(
                        expectedCustomerCount + (expectedCustomerCount == 1 ? " registro" : " registros"))))
                .verifyComplete();
    }

    private String pdfText(byte[] bytes) {
        try (PDDocument document = PDDocument.load(bytes)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        } catch (IOException exception) {
            throw new AssertionError("El archivo generado no es un PDF válido", exception);
        }
    }

    private List<Customer> customers(boolean isActive, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> Customer.builder()
                        .id((long) index + 1)
                        .firstName((isActive ? "Active" : "Inactive") + index)
                        .isActive(isActive)
                        .build())
                .toList();
    }

    private Customer customer(String docType, String docNumber) {
        return Customer.builder()
                .docType(docType)
                .docNumber(docNumber)
                .build();
    }
}

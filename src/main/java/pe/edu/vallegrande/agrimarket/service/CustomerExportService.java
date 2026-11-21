package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.repository.mongo.CustomerRepository;
import pe.edu.vallegrande.agrimarket.util.ExcelGenerator;
import pe.edu.vallegrande.agrimarket.util.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerExportService {

    private static final String EXCEL_SHEET_NAME = "Clientes";
    private static final String PDF_REPORT_TITLE = "Reporte de Clientes";

    private final CustomerRepository customerRepository;
    private final CustomerExportMapper customerExportMapper;

    public Mono<byte[]> exportExcel(Boolean isActive) {
        return exportRows(isActive)
                .flatMap(rows -> Mono.fromCallable(() -> ExcelGenerator.write(
                                EXCEL_SHEET_NAME, customerExportMapper.headers(), rows))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<byte[]> exportPdf(Boolean isActive) {
        return exportRows(isActive)
                .flatMap(rows -> Mono.fromCallable(() -> PdfGenerator.write(
                                PDF_REPORT_TITLE, customerExportMapper.headers(), rows))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<List<String[]>> exportRows(Boolean isActive) {
        return customers(isActive)
                .map(customerExportMapper::toRow)
                .collectList();
    }

    private Flux<Customer> customers(Boolean isActive) {
        return isActive == null
                ? customerRepository.findAll()
                : customerRepository.findByIsActive(isActive);
    }
}

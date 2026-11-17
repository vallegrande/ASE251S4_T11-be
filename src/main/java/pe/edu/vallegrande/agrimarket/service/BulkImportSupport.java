package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.dto.BulkImportResult;
import pe.edu.vallegrande.agrimarket.util.TabularFileReader;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class BulkImportSupport {

    private static final int FIRST_DATA_ROW_NUMBER = 2;

    private BulkImportSupport() {
    }

    public static <T> Mono<BulkImportResult> importRows(
            byte[] content,
            String filename,
            Function<Map<String, String>, T> rowMapper,
            Function<T, Mono<?>> saveRow) {
        return Mono.fromCallable(() -> TabularFileReader.read(content, filename))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .index()
                .concatMap(indexedRow -> importRow(
                        (int) (indexedRow.getT1() + FIRST_DATA_ROW_NUMBER),
                        indexedRow.getT2(),
                        rowMapper,
                        saveRow))
                .collectList()
                .map(BulkImportSupport::summarize);
    }

    private static <T> Mono<Optional<BulkImportResult.RowError>> importRow(
            int rowNumber,
            Map<String, String> row,
            Function<Map<String, String>, T> rowMapper,
            Function<T, Mono<?>> saveRow) {
        T value;
        try {
            value = rowMapper.apply(row);
        } catch (IllegalArgumentException exception) {
            return Mono.just(Optional.of(rowError(rowNumber, exception.getMessage())));
        }
        return saveRow.apply(value)
                .thenReturn(Optional.<BulkImportResult.RowError>empty())
                .onErrorResume(error -> Mono.just(Optional.of(rowError(rowNumber, reason(error)))));
    }

    private static BulkImportResult summarize(List<Optional<BulkImportResult.RowError>> results) {
        List<BulkImportResult.RowError> errors = results.stream()
                .flatMap(Optional::stream)
                .toList();
        return BulkImportResult.builder()
                .total(results.size())
                .imported(results.size() - errors.size())
                .failed(errors.size())
                .errors(errors)
                .build();
    }

    private static BulkImportResult.RowError rowError(int rowNumber, String message) {
        return BulkImportResult.RowError.builder()
                .row(rowNumber)
                .message(message)
                .build();
    }

    private static String reason(Throwable error) {
        if (error instanceof ResponseStatusException exception && exception.getReason() != null) {
            return exception.getReason();
        }
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }
}

package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.repository.SalesRepository;
import org.junit.jupiter.api.Test;

class SalesServiceTest {
  private final SalesRepository repository = mock(SalesRepository.class);
  private final SalesService service = new SalesService(repository);

  @Test void delegatesReceiptAndIdQueries() {
    PaymentReceipt receipt = new PaymentReceipt(); receipt.setId(7L);
    Sales sale = new Sales();
    when(repository.findSalesWithProductNames(7L)).thenReturn(List.of());
    when(repository.findById(2L)).thenReturn(Optional.of(sale));
    assertThat(service.getSalesByPaymentReceipt(receipt)).isEmpty();
    assertThat(service.getSaleById(2L)).isSameAs(sale);
    assertThat(service.findById(2L)).contains(sale);
  }

  @Test void getSaleByIdReturnsNullWhenMissingAndMutationsDelegate() {
    when(repository.findById(9L)).thenReturn(Optional.empty());
    assertThat(service.getSaleById(9L)).isNull();
    Sales sale = new Sales(); PaymentReceipt receipt = new PaymentReceipt();
    service.saveSale(sale);
    service.deleteSalesByPaymentReceipt(receipt);
    service.deleteSalesByIdNotInAndPaymentReceipt(receipt, List.of(1L));
    service.getSalesByPaymentReceiptIn(List.of(receipt));
    verify(repository).save(sale);
    verify(repository).deleteSalesByPaymentReceipt(receipt);
    verify(repository).deleteSalesByIdNotInAndPaymentReceipt(List.of(1L), receipt);
    verify(repository).getSalesByPaymentReceiptIn(List.of(receipt));
  }
}

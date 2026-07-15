package kz.store.cash.service;

import static kz.store.cash.model.enums.CashShiftStatus.OPENED;
import static kz.store.cash.model.enums.ReceiptStatus.PENDING;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.PaymentReceiptMapper;
import kz.store.cash.mapper.SalesMapper;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.repository.CashShiftRepository;
import kz.store.cash.repository.PaymentReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentReceiptServiceTest {
  PaymentReceiptMapper receiptMapper = mock(PaymentReceiptMapper.class);
  PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
  CashShiftRepository shifts = mock(CashShiftRepository.class);
  SalesService sales = mock(SalesService.class);
  SalesMapper salesMapper = mock(SalesMapper.class);
  PaymentReceiptService service;
  CashShift opened;

  @BeforeEach void setUp() {
    service = new PaymentReceiptService(receiptMapper, receipts, shifts, sales, salesMapper);
    opened = new CashShift(); opened.setStatus(OPENED);
  }

  @Test void processesPaymentAndAttachesSalesAndOpenShift() {
    PaymentSumDetails details = mock(PaymentSumDetails.class);
    PaymentReceipt receipt = new PaymentReceipt(); ProductItem item = item(null); Sales sale = new Sales();
    when(receiptMapper.saleToPaymentReceipt(details)).thenReturn(receipt);
    when(salesMapper.fromProductItemToSales(item, receipt)).thenReturn(sale);
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.of(opened));
    service.processPayment(details, FXCollections.observableArrayList(item));
    assertThat(receipt.getCashShift()).isSameAs(opened);
    assertThat(receipt.getSalesList()).containsExactly(sale);
    verify(receipts).save(receipt);
  }

  @Test void paymentFailsWithoutOpenShift() {
    PaymentSumDetails details = mock(PaymentSumDetails.class); PaymentReceipt receipt = new PaymentReceipt();
    when(receiptMapper.saleToPaymentReceipt(details)).thenReturn(receipt);
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.processPayment(details, FXCollections.observableArrayList()))
        .isInstanceOf(RuntimeException.class);
    verify(receipts, never()).save(any());
  }

  @Test void createsAndDeletesDeferredReceipt() {
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.of(opened));
    when(receipts.save(any())).thenAnswer(i -> i.getArgument(0));
    PaymentReceipt receipt = service.createDeferredPaymentReceipt();
    assertThat(receipt.getReceiptStatus()).isEqualTo(PENDING);
    assertThat(receipt.getCashShift()).isSameAs(opened);
    receipt.setId(10L); service.deleteDeferredPaymentReceipt(receipt);
    verify(receipts).deleteById(10L);
  }

  @Test void emptyCartDeletesExistingDeferredReceipt() {
    PaymentReceipt receipt = new PaymentReceipt(); receipt.setId(3L);
    service.mergeDeferredPaymentReceipts(receipt, List.of());
    verify(receipts).deleteById(3L);
    verifyNoInteractions(sales);
  }

  @Test void mergeCreatesNewAndSavesNewSale() {
    when(shifts.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED)).thenReturn(Optional.of(opened));
    when(receipts.save(any())).thenAnswer(i -> i.getArgument(0));
    ProductItem item = item(null); Sales mapped = new Sales();
    when(salesMapper.fromProductItemToSales(eq(item), any(PaymentReceipt.class))).thenReturn(mapped);
    service.mergeDeferredPaymentReceipts(null, List.of(item));
    verify(sales).deleteSalesByPaymentReceipt(any(PaymentReceipt.class));
    verify(sales).saveSale(mapped);
  }

  @Test void mergeUpdatesExistingSale() {
    PaymentReceipt receipt = new PaymentReceipt(); ProductItem item = item(8L); Sales existing = new Sales();
    when(sales.findById(8L)).thenReturn(Optional.of(existing));
    service.mergeDeferredPaymentReceipts(receipt, List.of(item));
    verify(sales).deleteSalesByIdNotInAndPaymentReceipt(receipt, List.of(8L));
    verify(salesMapper).updateToSale(existing, item, receipt);
    verify(sales).saveSale(existing);
  }

  @Test void mergeFailsWhenReferencedSaleIsMissing() {
    ProductItem item = item(99L);
    when(sales.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.mergeDeferredPaymentReceipts(new PaymentReceipt(), List.of(item)))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("99");
  }

  @Test void aggregateMethodsDelegateToRepository() {
    service.getSumCash(opened); service.getSumCard(opened); service.getReturnedSumCash(opened);
    service.getReturnedSumCard(opened); service.getDebtSum(opened); service.getDebtReturnSum(opened);
    verify(receipts).sumCashByShiftAndStatus(eq(opened), any(), any());
    verify(receipts).sumCardByShiftAndStatus(eq(opened), any(), any());
    verify(receipts).sumReturnCashByShiftAndStatus(eq(opened), any());
    verify(receipts).sumReturnCardByShiftAndStatus(eq(opened), any());
    verify(receipts).sumDebtTotalByShiftAndStatus(eq(opened), any());
    verify(receipts).sumDebtReturnTotalByShiftAndStatus(eq(opened), any());
  }

  private ProductItem item(Long id) { ProductItem item = new ProductItem("b", "p", 1, 1); item.setSalesId(id); return item; }
}

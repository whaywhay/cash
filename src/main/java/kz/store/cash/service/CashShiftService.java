package kz.store.cash.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kz.store.cash.mapper.CashMovementMapper;
import kz.store.cash.mapper.CashShiftMapper;
import kz.store.cash.model.entity.CashMovement;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.CashMovementType;
import kz.store.cash.model.enums.CashShiftStatus;
import kz.store.cash.repository.CashMovementRepository;
import kz.store.cash.repository.CashShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashShiftService {

  private static final CashShiftStatus OPENED = CashShiftStatus.OPENED;
  private static final CashShiftStatus CLOSED = CashShiftStatus.CLOSED;

  private final CashShiftRepository cashShiftRepository;
  private final CashMovementRepository cashMovementRepository;
  private final CashShiftMapper cashShiftMapper;
  private final CashMovementMapper cashMovementMapper;
  private final PaymentReceiptService paymentReceiptService;

  public Optional<CashShift> getOpenedShift() {
    return cashShiftRepository.findFirstByStatusOrderByShiftOpenedDateDesc(OPENED);
  }

  public Optional<CashShift> getLastClosedShift() {
    return cashShiftRepository.findFirstByStatusOrderByShiftClosedDateDesc(CLOSED);
  }

  public List<CashMovement> findMovements(Long shiftId) {
    return cashMovementRepository.findAllByCashShiftIdOrderByCreatedDesc(shiftId);
  }

  /**
   * Открыть смену, если её нет; если уже есть открытая — вернуть её (не меняя openedUser).
   */
  @Transactional
  public void ensureOpenShift(User openedBy) {
    getOpenedShift().orElseGet(() -> openShift(openedBy));
  }

  /**
   * При открытии: cashDuringOpening = lastClosed.leftInDrawer (или 0).
   */
  @Transactional
  public CashShift openShift(User openedBy) {
    if (getOpenedShift().isPresent()) {
      throw new IllegalStateException("Уже есть открытая смена");
    }
    BigDecimal openingCash = getLastRemainedCashInDrawer();
    CashShift cashShift = cashShiftMapper.toOpenCashShift(openedBy, openingCash);
    return cashShiftRepository.save(cashShift);
  }

  private BigDecimal getLastRemainedCashInDrawer() {
    return getLastClosedShift().map(CashShift::getLeftInDrawer).orElse(BigDecimal.ZERO);
  }

  @Transactional
  public void addMovement(Long shiftId, CashMovementType type, BigDecimal amount,
      String reason, User actor) {
    CashShift s = getOpenShiftById(shiftId);
    var cashMovement = cashMovementMapper.toCashMovement(s, type, amount, reason, actor);
    cashMovementRepository.save(cashMovement);
  }

  /**
   * Закрываем смену: фиксируем leftInDrawer и CLOSE. sumCash/sumCard — потом из чеков.
   */
  @Transactional
  public void closeShift(Long shiftId, BigDecimal leftInDrawer, String note, User closedBy) {
    CashShift cashShift = getOpenShiftById(shiftId);
    if (!paymentReceiptService.getDeferredPaymentReceipts().isEmpty()) {
      throw new RuntimeException(
          "Имеются отложенные чеки, прошу их обработать прежде чем закрыть смену");
    }
    BigDecimal sumCash = paymentReceiptService.getSumCash(cashShift);
    BigDecimal sumCard = paymentReceiptService.getSumCard(cashShift);
    BigDecimal sumReturnedCash = paymentReceiptService.getReturnedSumCash(cashShift);
    BigDecimal sumReturnedCard = paymentReceiptService.getReturnedSumCard(cashShift);
    cashShiftMapper.toCloseCashShift(cashShift, closedBy, leftInDrawer, sumCash,
        sumCard, sumReturnedCash, sumReturnedCard, note);
    cashShiftRepository.save(cashShift);
  }

  private CashShift getOpenShiftById(Long id) {
    CashShift cashShift = cashShiftRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Смена не найдена"));
    if (cashShift.getStatus() != OPENED) {
      throw new IllegalStateException("Смена уже закрыта");
    }
    return cashShift;
  }
}
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

  private static final CashShiftStatus OPENED = CashShiftStatus.OPEN;
  private static final CashShiftStatus CLOSED = CashShiftStatus.CLOSE;

  private final CashShiftRepository cashShiftRepository;
  private final CashMovementRepository cashMovementRepository;
  private final CashShiftMapper cashShiftMapper;
  private final CashMovementMapper cashMovementMapper;

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
    CashShift s = cashShiftMapper.toOpenCashShift(openedBy, openingCash);
    return cashShiftRepository.save(s);
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
    CashShift s = getOpenShiftById(shiftId);
    cashShiftMapper.toCloseCashShift(s, closedBy, leftInDrawer,
        BigDecimal.ZERO, BigDecimal.ZERO, note);
    cashShiftRepository.save(s);
  }

  private CashShift getOpenShiftById(Long id) {
    CashShift s = cashShiftRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Смена не найдена"));
    if (s.getStatus() != OPENED) {
      throw new IllegalStateException("Смена уже закрыта");
    }
    return s;
  }
}
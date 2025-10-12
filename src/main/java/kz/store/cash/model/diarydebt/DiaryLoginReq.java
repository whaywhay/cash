package kz.store.cash.model.diarydebt;

public record DiaryLoginReq(
    String username,
    String password
) {

  public DiaryLoginReq {
    username = username == null ? null : username.trim();
    password = password == null ? null : password.trim();
  }

}

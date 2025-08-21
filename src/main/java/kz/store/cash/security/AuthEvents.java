package kz.store.cash.security;

import kz.store.cash.model.entity.User;
import org.springframework.context.ApplicationEvent;

public class AuthEvents {

  public static class LoginSuccess extends ApplicationEvent {

    public LoginSuccess(User source) {
      super(source);
    }

    @Override
    public User getSource() {
      return (User) super.getSource();
    }
  }

  public static class Logout extends ApplicationEvent {

    public Logout(Object source) {
      super(source);
    }
  }
}

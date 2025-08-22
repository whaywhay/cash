package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import kz.store.cash.model.UserDto;
import kz.store.cash.model.entity.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "password", ignore = true)
  void updateToUser(@MappingTarget User user, UserDto userDto,
      PasswordEncoder passwordEncoder);

  @AfterMapping
  default void encodePassword(UserDto userDto,
      @MappingTarget User user,
      PasswordEncoder passwordEncoder) {
    if (userDto.password() != null && !userDto.password().isBlank()) {
      user.setPassword(passwordEncoder.encode(userDto.password()));
    }
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "password", ignore = true)
  User toUser(UserDto userDto, PasswordEncoder passwordEncoder);
}

package kz.store.cash.model.diarydebt;

import java.util.List;

public record PageResult<T>(
    List<T> content,
    int page,
    int totalPages,
    int totalItems,
    String next,
    String previous
) {

}

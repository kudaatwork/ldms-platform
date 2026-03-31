package projectlx.co.zw.shared_library.utils.dtos;

import java.util.List;

public record PaginatedResponse<T>(List<T> data, int page, int size, long totalItems, int totalPages) {
}

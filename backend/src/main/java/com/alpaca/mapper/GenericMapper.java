package com.alpaca.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface GenericMapper<T, ResponseDTO, RequestDTO> {

  ResponseDTO toResponseDTO(T entity);

  T toEntity(RequestDTO requestDTO);

  List<ResponseDTO> toListResponseDTO(Collection<T> entities);

  default Page<ResponseDTO> toPageResponseDTO(Page<T> entities) {
    if (entities == null || entities.isEmpty()) {
      return new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
    }
    return new PageImpl<>(
        toListResponseDTO(entities.getContent()),
        entities.getPageable(),
        entities.getTotalElements());
  }
}

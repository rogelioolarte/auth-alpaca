package com.example.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collection;
import java.util.List;

public interface GenericMapper<T, ResponseDTO, RequestDTO> {

    ResponseDTO toResponseDTO(T entity);

    T toEntity(RequestDTO requestDTO);

    List<ResponseDTO> toListResponseDTO(Collection<T> entities);

    default Page<ResponseDTO> toPageResponseDTO(Page<T> entities){
        return new PageImpl<>(toListResponseDTO(entities.getContent()),
                entities.getPageable(), entities.getTotalElements());
    }

}

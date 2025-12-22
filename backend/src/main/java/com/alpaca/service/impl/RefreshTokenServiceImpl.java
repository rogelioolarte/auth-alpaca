package com.alpaca.service.impl;

import com.alpaca.entity.RefreshToken;
import com.alpaca.persistence.IGenericDAO;
import com.alpaca.persistence.IRefreshTokenDAO;
import com.alpaca.service.IGenericService;
import com.alpaca.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service layer implementation for managing {@link RefreshToken} entities. Inherits common CRUD
 * operations from {@link IGenericService}.
 *
 * <p>This service delegates persistence operations to the {@link IRefreshTokenDAO} and provides a
 * clear abstraction point for future business logic related to permissions.
 *
 * @see IGenericService
 * @see IRefreshTokenService
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl extends GenericServiceImpl<RefreshToken, UUID>
		implements IRefreshTokenService {

	private final IRefreshTokenDAO dao;

	@Override
	protected IGenericDAO<RefreshToken, UUID> getDAO() {
		return dao;
	}

	@Override
	protected String getEntityName() {
		return "RefreshToken";
	}
}

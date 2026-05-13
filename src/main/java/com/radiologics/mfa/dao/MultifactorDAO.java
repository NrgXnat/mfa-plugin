//Copyright 2019 Radiologics, Inc
//Author: James <ransfordj@radiologics.com>

package com.radiologics.mfa.dao;
import java.util.List;

import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.radiologics.mfa.entities.MultifactorEntity;

@Repository
public class MultifactorDAO extends AbstractHibernateDAO<MultifactorEntity> {
	@Transactional
	public List<MultifactorEntity> getMultifactorEntities() {
        //noinspection deprecation
        return GenericUtils.convertToTypedList(getCriteriaForType().list(), MultifactorEntity.class);
	}
}
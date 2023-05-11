//Copyright 2019 Radiologics, Inc
//Author: James <ransfordj@radiologics.com>

package com.radiologics.mfa.dao;
import java.util.List;

import org.hibernate.Criteria;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.radiologics.mfa.entities.MultifactorEntity;

@Repository
public class MultifactorDAO extends AbstractHibernateDAO<MultifactorEntity> {
	
	@Transactional
	public List getMultifactorEntities() {
		Criteria criteria = getCriteriaForType();
		return criteria.list();
	}
}
package org.sakaiproject.archiver.persistence.impl;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.sakaiproject.archiver.api.Status;
import org.sakaiproject.archiver.dto.Archive;
import org.sakaiproject.archiver.entity.ArchiveEntity;
import org.sakaiproject.archiver.impl.ArchiveMapper;
import org.sakaiproject.archiver.persistence.ArchiverPersistenceService;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArchiverPersistenceServiceImpl extends HibernateDaoSupport implements ArchiverPersistenceService {

	public void init() {

	}

	public void destroy() {

	}

	/**
	 * Create a new archive
	 *
	 * @param siteId
	 * @param userUuid
	 * @return the entity
	 */
	@Override
	public ArchiveEntity create(final String siteId, final String userUuid) {

		final ArchiveEntity entity = new ArchiveEntity();
		entity.setSiteId(siteId);
		entity.setUserUuid(userUuid);
		entity.setStartDate(new Date());
		entity.setStatus(Status.STARTED);

		final Session session = getSessionFactory().getCurrentSession();
		session.save(entity);
		session.flush();

		log.debug("saved: " + entity.getId());

		return entity;
	}

	/**
	 * Update an archive
	 *
	 * @param entity
	 * @return the entity
	 */
	@Override
	public ArchiveEntity update(final ArchiveEntity entity) {

		final Session session = getSessionFactory().getCurrentSession();
		session.update(entity);
		session.flush();

		log.debug("saved: " + entity.getId());
		return entity;
	}

	/**
	 * Get a current archive for the given site. Return null if none exists or is not currently active.
	 *
	 * @param siteId
	 * @param userUuid
	 * @return
	 */
	@Override
	public Archive getCurrent(final String siteId) {

		// TODO look at status iunstead

		final Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(ArchiveEntity.class);
		criteria.add(Restrictions.eq("siteId", siteId));
		criteria.add(Restrictions.isNull("endDate"));
		// TODO catch exception if there is more than one for whatever reason, and deal with it.
		final ArchiveEntity entity = (ArchiveEntity) criteria.uniqueResult();

		return ArchiveMapper.toDto(entity);
	}

}

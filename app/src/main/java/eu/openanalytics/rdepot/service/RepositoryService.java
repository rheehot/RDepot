/**
 * R Depot
 *
 * Copyright (C) 2012-2020 Open Analytics NV
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
package eu.openanalytics.rdepot.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.openanalytics.rdepot.exception.CreateFolderStructureException;
import eu.openanalytics.rdepot.exception.DeleteFileException;
import eu.openanalytics.rdepot.exception.EventNotFound;
import eu.openanalytics.rdepot.exception.LinkFoldersException;
import eu.openanalytics.rdepot.exception.PackageDeleteException;
import eu.openanalytics.rdepot.exception.PackageFolderPopulationException;
import eu.openanalytics.rdepot.exception.PackageMaintainerDeleteException;
import eu.openanalytics.rdepot.exception.PackageMaintainerNotFound;
import eu.openanalytics.rdepot.exception.PackageNotFound;
import eu.openanalytics.rdepot.exception.RepositoryCreateException;
import eu.openanalytics.rdepot.exception.RepositoryDeleteException;
import eu.openanalytics.rdepot.exception.RepositoryEditException;
import eu.openanalytics.rdepot.exception.RepositoryMaintainerDeleteException;
import eu.openanalytics.rdepot.exception.RepositoryMaintainerNotFound;
import eu.openanalytics.rdepot.exception.RepositoryNotFound;
import eu.openanalytics.rdepot.exception.RepositoryPublishException;
import eu.openanalytics.rdepot.exception.UploadToRemoteServerException;
import eu.openanalytics.rdepot.model.Event;
import eu.openanalytics.rdepot.model.Package;
import eu.openanalytics.rdepot.model.PackageMaintainer;
import eu.openanalytics.rdepot.model.Repository;
import eu.openanalytics.rdepot.model.RepositoryEvent;
import eu.openanalytics.rdepot.model.RepositoryMaintainer;
import eu.openanalytics.rdepot.model.User;
import eu.openanalytics.rdepot.repository.RepositoryRepository;
import eu.openanalytics.rdepot.repository.RoleRepository;
import eu.openanalytics.rdepot.storage.RepositoryStorage;
import eu.openanalytics.rdepot.time.DateProvider;
import eu.openanalytics.rdepot.warning.PackageAlreadyDeletedWarning;
import eu.openanalytics.rdepot.warning.RepositoryAlreadyUnpublishedWarning;

@Service
@Transactional(readOnly = true)
@Scope( proxyMode = ScopedProxyMode.TARGET_CLASS )
public class RepositoryService
{	
	Logger logger = LoggerFactory.getLogger(RepositoryService.class);
	Locale locale = LocaleContextHolder.getLocale();
	
	@Resource
	private MessageSource messageSource;
	
	@Resource
	private RepositoryStorage repositoryStorage;
	
	@Resource
	private RepositoryRepository repositoryRepository;
	
	@Resource
	private RoleRepository roleRepository;
	
	@Resource
	private RepositoryMaintainerService repositoryMaintainerService;
	
	@Resource
	private PackageMaintainerService packageMaintainerService;
	
	@Resource
	private PackageService packageService;
	
	@Resource
	private RepositoryEventService repositoryEventService;
	
	@Resource
	private EventService eventService;

	@Transactional(readOnly = false, rollbackFor = RepositoryCreateException.class)
	public Repository create(Repository repository, User creator)  throws RepositoryCreateException {
		try {
			Event createEvent = eventService.getUpdateEvent();
			Repository createdRepository = repositoryRepository.saveAndFlush(repository);
			repositoryEventService.create(createEvent, creator, createdRepository);
			return createdRepository;
			
		} catch (DataIntegrityViolationException | EventNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryCreateException(messageSource, locale, repository);
		}
	}
	
	public Repository findById(int id) {
		return repositoryRepository.findByIdAndDeleted(id, false);
	}
	
	public Repository findByIdAndDeleted(int id, boolean deleted) {
		return repositoryRepository.findByIdAndDeleted(id, deleted);
	}
	
	@Transactional(readOnly = false, rollbackFor={RepositoryDeleteException.class})
	public Repository delete(int id, User deleter) 
			throws RepositoryDeleteException, RepositoryNotFound {
		Repository deletedRepository = repositoryRepository.findByIdAndDeleted(id, false);
		try {
			if (deletedRepository == null)
				throw new RepositoryNotFound(messageSource, locale, id);
			
			Event deleteEvent = eventService.getDeleteEvent();
			
			deletedRepository.setDeleted(true);
			deleteRepositoryMaintainers(deletedRepository, deleter);
			deletePackageMaintainers(deletedRepository, deleter);
			
			try {
				unpublishRepository(deletedRepository, deleter);
				deletePackages(deletedRepository, deleter);
				
			} catch (PackageAlreadyDeletedWarning | RepositoryAlreadyUnpublishedWarning w) {
				logger.warn(w.getClass().getName() + ": " + w.getMessage(), w);
			}
			
			repositoryEventService.create(deleteEvent, deleter, deletedRepository);
			return deletedRepository;
		}
		catch(	RepositoryEditException | 
				PackageMaintainerDeleteException | 
				RepositoryMaintainerDeleteException | 
				EventNotFound |
				PackageDeleteException|
				PackageNotFound |
				PackageMaintainerNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryDeleteException(messageSource, locale, deletedRepository);
		}
	}
	
	@Transactional(readOnly = false, rollbackFor=RepositoryEditException.class)
	public void unpublishRepository(Repository repository, User updater) 
			throws RepositoryEditException, RepositoryAlreadyUnpublishedWarning {
		if(!repository.isPublished())
			throw new RepositoryAlreadyUnpublishedWarning(messageSource, locale, repository);
		
		try {
			Event updateEvent = eventService.getUpdateEvent();
			RepositoryEvent publishEvent = new RepositoryEvent(
					0, DateProvider.now(), updater, repository, updateEvent, 
					"published", Boolean.toString(true), Boolean.toString(false), DateProvider.now());

			repository.setPublished(false);
			update(publishEvent);
			
			boostRepositoryVersion(repository, updater);
			
			repositoryStorage.deleteCurrentDirectory(repository);
		} catch (DeleteFileException | EventNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryEditException(messageSource, locale, repository);
		}
	}
	
	@Transactional(readOnly = false, rollbackFor={RepositoryDeleteException.class})
	public Repository shiftDelete(int id) throws RepositoryDeleteException, RepositoryNotFound {
		Repository deletedRepository = repositoryRepository.findByIdAndDeleted(id, true);

		try {
			if (deletedRepository == null)
				throw new RepositoryNotFound(messageSource, locale, id);
			
			shiftDeleteRepositoryMaintainers(deletedRepository);
			shiftDeletePackageMaintainers(deletedRepository);
			shiftDeletePackages(deletedRepository);
			for(RepositoryEvent event : deletedRepository.getRepositoryEvents())
				repositoryEventService.delete(event.getId());
			repositoryRepository.delete(deletedRepository);
			repositoryStorage.deleteRepositoryDirectory(deletedRepository);
			return deletedRepository;
		}
		catch(PackageDeleteException | 
				PackageMaintainerDeleteException | 
				RepositoryMaintainerNotFound | 
				DeleteFileException | 
				PackageNotFound | PackageMaintainerNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryDeleteException(messageSource, locale, deletedRepository);
		}
	}

	public List<Repository> findAll() {
		return repositoryRepository.findByDeleted(false, Sort.by(new Order(Direction.ASC, "name")));
	}
	
	public List<Repository> findAllEvenDeleted() {
		return repositoryRepository.findAll(Sort.by(new Order(Direction.ASC, "name")));
	}
	
	public List<Repository> findByDeleted(boolean deleted) {
		return repositoryRepository.findByDeleted(deleted, Sort.by(new Order(Direction.ASC, "name")));
	}
	
	public List<Repository> findMaintainedBy(User user, boolean includeDeleted) {
		switch(user.getRole().getName()) {
			case "admin":
				if(includeDeleted)
					return findAllEvenDeleted();
				else
					return findAll();
			case "repositorymaintainer":
				List<Repository> repositories = new ArrayList<Repository>();
				for(RepositoryMaintainer repositoryMaintainer : user.getRepositoryMaintainers()) {
					if(!repositoryMaintainer.isDeleted() && 
							(includeDeleted || !repositoryMaintainer.getRepository().isDeleted()))
						repositories.add(repositoryMaintainer.getRepository());
				}
				return repositories;
		}
		return new ArrayList<Repository>();
	}
	
	@Transactional(readOnly = false)
	public void deleteRepositoryMaintainers(Repository repository, User deleter)
			throws RepositoryMaintainerDeleteException {
		for(RepositoryMaintainer repositoryMaintainer : repository.getRepositoryMaintainers()) {
			if(!repositoryMaintainer.isDeleted())
				repositoryMaintainerService.delete(repositoryMaintainer.getId(), deleter);
		}
	}
	
	@Transactional(readOnly = false)
	public void deletePackageMaintainers(Repository repository, User deleter)
			throws PackageMaintainerDeleteException, PackageMaintainerNotFound {
		for(PackageMaintainer packageMaintainer : repository.getPackageMaintainers()) {
			if(!packageMaintainer.isDeleted())
				packageMaintainerService.delete(packageMaintainer.getId(), deleter);
		}
	}
	
	@Transactional(readOnly = false)
	public void deletePackages(Repository repository, User deleter) 
			throws PackageDeleteException, PackageAlreadyDeletedWarning, PackageNotFound {
		for(Package p : repository.getNonDeletedPackages()) {
			packageService.delete(p.getId(), deleter);
		}
	}
	
	@Transactional(readOnly = false)
	public void shiftDeleteRepositoryMaintainers(Repository repository)
			throws RepositoryMaintainerNotFound {
		for(RepositoryMaintainer repositoryMaintainer : repository.getRepositoryMaintainers()) {
			repositoryMaintainerService.shiftDelete(repositoryMaintainer.getId());
		}
	}
	
	@Transactional(readOnly = false)
	public void shiftDeletePackageMaintainers(Repository repository)
			throws PackageMaintainerDeleteException, PackageMaintainerNotFound {
		for(PackageMaintainer packageMaintainer : repository.getPackageMaintainers()) {
			packageMaintainerService.shiftDelete(packageMaintainer.getId());
		}
	}
	
	@Transactional(readOnly = false)
	public void shiftDeletePackages(Repository repository)
			throws PackageDeleteException, PackageNotFound {
		for(Package p :  repository.getPackages()) {
			packageService.shiftDelete(p.getId());
		}
	}
	
	@Transactional(readOnly=false)
	private void update(RepositoryEvent updateRepositoryEvent) {
		repositoryEventService.create(updateRepositoryEvent);
	}
	
	@Transactional(readOnly=false, rollbackFor=RepositoryEditException.class)
	public void updateVersion(Repository repository, User updater, int newVersion)
			throws RepositoryEditException {
		int currentVersion = repository.getVersion();
		
		try {
			Event updateEvent = eventService.getUpdateEvent();
			repository.setVersion(newVersion);
			RepositoryEvent updateVersionEvent = new RepositoryEvent(
					0, DateProvider.now(), updater, repository, updateEvent,
					"version", "" + currentVersion, "" + newVersion, DateProvider.now());
			update(updateVersionEvent);
		} catch(EventNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryEditException(messageSource, locale, repository);
		}
	}
	
	@Transactional(readOnly=false, rollbackFor=RepositoryEditException.class)
	public void updatePublicationUri(Repository repository, User updater, String newPublicationUri) 
			throws RepositoryEditException {
		String currentPublicationUri = repository.getPublicationUri();
		
		try {
			Event updateEvent = eventService.getUpdateEvent();
			repository.setPublicationUri(newPublicationUri);
			
			RepositoryEvent updatePublicationUriEvent = new RepositoryEvent(
					0, DateProvider.now(), updater, repository, updateEvent,
					"publication URI", currentPublicationUri, 
					repository.getPublicationUri(), DateProvider.now());
			
			update(updatePublicationUriEvent);
		} catch(EventNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryEditException(messageSource, locale, repository);
		}
	}
	
	@Transactional(readOnly=false, rollbackFor=RepositoryEditException.class)
	public void updateServerAddress(Repository repository, User updater, String newServerAddress) 
			throws RepositoryEditException {
		String currentServerAddress = repository.getServerAddress();
		
		try {
			Event updateEvent = eventService.getUpdateEvent();
			repository.setServerAddress(newServerAddress);
			RepositoryEvent updateServerAddressEvent = new RepositoryEvent(
					0, DateProvider.now(), updater, repository, updateEvent, 
					"server address", currentServerAddress, 
					repository.getServerAddress(), DateProvider.now());
			update(updateServerAddressEvent);
		} catch(EventNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryEditException(messageSource, locale, repository);
		}
	}
	
	@Transactional(readOnly=false, rollbackFor=RepositoryEditException.class)
	public void updateName(Repository repository, User updater, String newName) 
			throws RepositoryEditException {
		String currentName = repository.getName();
		try {
			Event updateEvent = eventService.getUpdateEvent();
			repository.setName(newName);
			RepositoryEvent updateNameEvent = new RepositoryEvent(
					0, DateProvider.now(), updater, repository, updateEvent,
					"name", currentName, repository.getName(), DateProvider.now());
			update(updateNameEvent);
		} catch(EventNotFound e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryEditException(messageSource, locale, repository);
		}
	}
	
	@Transactional
	public void evaluateAndUpdate(Repository repository, User updater)
			throws RepositoryEditException, RepositoryNotFound {
		Repository currentRepository = repositoryRepository.findByIdAndDeleted(repository.getId(), false);
		
		if(currentRepository == null)
			throw new RepositoryNotFound(messageSource, locale, repository.getId());
		if(!repository.getName().equals(currentRepository.getName()))
			updateName(currentRepository, updater, repository.getName());
		if(!repository.getPublicationUri().equals(currentRepository.getPublicationUri()))
			updatePublicationUri(currentRepository, updater, repository.getPublicationUri());
		if(!repository.getServerAddress().equals(currentRepository.getServerAddress()))
			updateServerAddress(currentRepository, updater, repository.getServerAddress());
		if(repository.getVersion() != currentRepository.getVersion())
			updateVersion(currentRepository, updater, repository.getVersion());
	}

	public Repository findByName(String name) {
		return repositoryRepository.findByNameAndDeleted(name, false);
	}

	public Repository findByPublicationUri(String publicationUri) {
		return repositoryRepository.findByPublicationUriAndDeleted(publicationUri, false);
	}
	
	@Transactional(readOnly=false)
	public void boostRepositoryVersion(Repository repository, User updater)
			throws RepositoryEditException {
		updateVersion(repository, updater, repository.getVersion() + 1);
	}

	@Transactional(readOnly = false, rollbackFor=RepositoryPublishException.class)
	public void publishRepository(Repository repository, User uploader)
			throws RepositoryPublishException {
		String dateStamp = (new SimpleDateFormat("yyyyMMdd")).format(DateProvider.now());
		List<Package> packages = packageService.findByRepositoryAndActive(repository, true);
		List<Package> latestPackages = packageService.findByRepositoryAndActiveAndNewest(repository, true);
		List<Package> archivePackages = new ArrayList<>();

		for(Package packageBag : packages) {
			if(!latestPackages.contains(packageBag)) {
				archivePackages.add(packageBag);
			}
		}
		//List<Package> archive - sort out older packages
		try {
			Event updateEvent = eventService.getUpdateEvent();
			RepositoryEvent publishEvent = new RepositoryEvent(
					0, DateProvider.now(), uploader, repository, updateEvent, "published", "", "", DateProvider.now());

			update(publishEvent);
			boostRepositoryVersion(repository, uploader);
			
			repository.setPublished(true);
			
			repositoryStorage.createFolderStructureForGeneration(
					repository, dateStamp);
			repositoryStorage.populateGeneratedFolder(packages, repository, dateStamp);
			repositoryStorage.copyFromRepositoryToRemoteServer(latestPackages, archivePackages,
					repositoryStorage.linkCurrentFolderToGeneratedFolder(repository, dateStamp),
					repository);
						
		} catch (EventNotFound |
				CreateFolderStructureException |
				RepositoryEditException e) {
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryPublishException(messageSource, locale, repository);
		} catch (PackageFolderPopulationException |
				UploadToRemoteServerException | 
				LinkFoldersException e) {
			
			try {
				repositoryStorage.deleteGenerationDirectory(repository, dateStamp);
			} catch (DeleteFileException dfe) {
				logger.error("Cannot remove generation directory after publication failure!\n" 
							+ dfe.getMessage());
			}
			
			logger.error(e.getClass().getName() + ": " + e.getMessage(), e);
			throw new RepositoryPublishException(messageSource, locale, repository);
			
		}
		// TODO:
		// 4. Generate the HTML pages? -> use template inside the resources folder?
	}
}

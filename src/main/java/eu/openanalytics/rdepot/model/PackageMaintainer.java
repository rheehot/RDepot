/**
 * RDepot
 *
 * Copyright (C) 2012-2017 Open Analytics NV
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
package eu.openanalytics.rdepot.model;

// Generated Jun 24, 2013 12:33:03 PM by Hibernate Tools 4.0.0

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonManagedReference;

/**
 * PackageMaintainer generated by hbm2java
 */
@Entity
@Table(name = "package_maintainer", schema = "public")
public class PackageMaintainer implements java.io.Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7768199899146657571L;
	private int id;
	private User user;
	private Repository repository;
	private String package_;
	private boolean deleted = false;
	private Set<PackageMaintainerEvent> packageMaintainerEvents = new HashSet<PackageMaintainerEvent>(0);

	public PackageMaintainer()
	{
	}

	public PackageMaintainer(int id, User user, Repository repository,
			String package_, boolean deleted)
	{
		this.id = id;
		this.user = user;
		this.repository = repository;
		this.package_ = package_;
		this.deleted = deleted;
	}
	
	public PackageMaintainer(int id, User user, Repository repository,
			String package_, boolean deleted, Set<PackageMaintainerEvent> packageMaintainerEvents)
	{
		this.id = id;
		this.user = user;
		this.repository = repository;
		this.package_ = package_;
		this.packageMaintainerEvents = packageMaintainerEvents;
		this.deleted = deleted;
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	public int getId()
	{
		return this.id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "user_id", nullable = false)
	@JsonManagedReference
	public User getUser()
	{
		return this.user;
	}

	public void setUser(User user)
	{
		this.user = user;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "repository_id", nullable = false)
	@JsonManagedReference
	public Repository getRepository()
	{
		return this.repository;
	}

	public void setRepository(Repository repository)
	{
		this.repository = repository;
	}

	@Column(name = "package", nullable = false)
	public String getPackage()
	{
		return this.package_;
	}

	public void setPackage(String package_)
	{
		this.package_ = package_;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "packageMaintainer")
	public Set<PackageMaintainerEvent> getPackageMaintainerEvents()
	{
		return this.packageMaintainerEvents;
	}

	public void setPackageMaintainerEvents(Set<PackageMaintainerEvent> packageMaintainerEvents)
	{
		this.packageMaintainerEvents = packageMaintainerEvents;
	}
	
	@Column(name = "deleted", nullable = false)
	public boolean isDeleted()
	{
		return this.deleted;
	}

	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}
	
}

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

//import java.util.ArrayList;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

public class UploadRequest
{
	private CommonsMultipartFile[] fileData;
	private Repository repository;
	private String[] changes;
	
	public UploadRequest()
	{
		//this.fileData = new ArrayList<CommonsMultipartFile>(1);
		//this.repository = new ArrayList<Repository>(1);
	}
	
	public UploadRequest(CommonsMultipartFile[] fileData, Repository repository)
	{
		this.fileData = fileData;
		this.repository = repository;
	}
	
	public UploadRequest(CommonsMultipartFile[] fileData, Repository repository, String[] changes)
	{
		this.fileData = fileData;
		this.repository = repository;
		this.changes = changes;
	}

	public CommonsMultipartFile[] getFileData() {
		return fileData;
	}

	public void setFileData(CommonsMultipartFile[] fileData) {
		this.fileData = fileData;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public String[] getChanges() {
		return changes;
	}

	public void setChanges(String[] changes) {
		this.changes = changes;
	}
	
}
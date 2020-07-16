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
package eu.openanalytics.rdepot.exception;

import java.util.Locale;

import org.springframework.context.MessageSource;

import eu.openanalytics.rdepot.model.Submission;

public class SubmissionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5703747963635328853L;
	
	public SubmissionException(String messageCode, MessageSource messageSource, Locale locale, Submission submission) {
		super("Submission " + Integer.toString(submission.getId()) + 
			": " + messageSource.getMessage(messageCode, null, messageCode, locale));
	}

	public SubmissionException(String messageCode, MessageSource messageSource, Locale locale,
			int submissionId) {
		super("Submission " + Integer.toString(submissionId) + 
				": " + messageSource.getMessage(messageCode, null, messageCode, locale));
	}
}
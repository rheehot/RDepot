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
package eu.openanalytics.rdepot.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import eu.openanalytics.rdepot.repo.storage.StorageService;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FileUploadIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private StorageService storageService;

    @LocalServerPort
    private int port;

    @Test
    public void shouldUploadFile() throws Exception {
        ClassPathResource resources = new ClassPathResource("testupload.txt", getClass());

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("files", resources);
        ResponseEntity<String> response = this.restTemplate.postForEntity("/", map, String.class);

        assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
        assertThat(response.getBody().toString()).isEqualTo("OK");
        then(storageService).should().store(any(MultipartFile[].class), any(String.class));
    }
    
    @Test
    public void shouldUploadFileToArchive() throws Exception {
    	ClassPathResource resources = new ClassPathResource("testupload.txt", getClass());

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
        map.add("files", resources);
        ResponseEntity<String> response = this.restTemplate.postForEntity("/archive", map, String.class);

        assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
        assertThat(response.getBody().toString()).isEqualTo("OK");
        then(storageService).should().storeInArchive(any(MultipartFile[].class), any(String.class));
    }

}

/*
 * Copyright 2023 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;

public class DataFiles extends ExternalResource {

    public static final String LAZ_NAME = "26690_12570.laz";
    private static final String LAZ_BASE_URL = "http://maps.zh.ch/download/hoehen/2014/lidar";
    public static final int LAZ_NUM_POINT_RECORDS = 265401;

    public static final String LAS_NAME = "2312.las";
    private static final String LAS_BASE_URL = "https://dc-lidar-2018.s3.amazonaws.com/Classified_LAS";
    public static final int LAS_NUM_POINT_RECORDS = 1653361;

    public static final String LAZ_14_NAME = "autzen-classified.copc.laz";
    private static final String LAZ_14_BASE_URL = "https://github.com/PDAL/data/raw/ce0024257c573526389c4db9ab26e82739b8aaa9/autzen";
    public static final int LAZ_14_NUM_POINT_RECORDS = 10653336;

    // test file contributed via https://github.com/mreutegg/laszip4j/pull/141
    public static final String LAZ_14_V3_RGB_NAME = "500m_5460_59370_IM2023_subset.laz";

    // file created with txt2las as described here: https://groups.google.com/g/lasroom/c/DWQ2GXKE8f8
    public static final String EXTRA_TYPES_NAME = "extra-bytes.las";

    private final File target = new File("target");
    private final File resources = new File(new File("src", "test"), "resources");

    public final File laz = new File(target, LAZ_NAME);
    public final File las = new File(target, LAS_NAME);
    public final File laz14 = new File(target, LAZ_14_NAME);
    public final File extraBytes = new File(resources, EXTRA_TYPES_NAME);
    public final File laz14v3rgb = new File(resources, LAZ_14_V3_RGB_NAME);

    @Override
    protected void before() throws Throwable {
        download();
    }

    public void download() throws Exception {
        if (!laz.exists()) {
            URI url = new URI(LAZ_BASE_URL + "/" + LAZ_NAME);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    try (OutputStream out = Files.newOutputStream(laz.toPath())) {
                        response.getEntity().writeTo(out);
                    }
                }
            }
        }
        if (!las.exists()) {
            URI url = new URI(LAS_BASE_URL + "/" + LAS_NAME);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    try (OutputStream out = Files.newOutputStream(las.toPath())) {
                        response.getEntity().writeTo(out);
                    }
                }
            }
        }
        if (!laz14.exists()) {
            URI url = new URI(LAZ_14_BASE_URL + "/" + LAZ_14_NAME);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    try (OutputStream out = Files.newOutputStream(laz14.toPath())) {
                        response.getEntity().writeTo(out);
                    }
                }
            }
        }
    }
}

package com.example.event_app.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.event_app.TestDataLoader;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class ImageDataModelTest {

    @Test
    public void constructorSetsFieldsAndTimestamp() throws IOException {
        Map<String, String> data = TestDataLoader.loadRecord("test-data/images.csv", "IMG8001");

        long before = System.currentTimeMillis();
        ImageData imageData = new ImageData(data.get("imageId"), data.get("imageUrl"), data.get("uploadedBy"), data.get("type"));
        long after = System.currentTimeMillis();

        assertEquals(data.get("imageId"), imageData.getImageId());
        assertEquals(data.get("imageUrl"), imageData.getImageUrl());
        assertEquals(data.get("uploadedBy"), imageData.getUploadedBy());
        assertEquals(data.get("type"), imageData.getType());
        assertTrue(imageData.getUploadedAt() >= before && imageData.getUploadedAt() <= after);
    }

    @Test
    public void settersOverrideOptionalAssociation() throws IOException {
        Map<String, String> data = TestDataLoader.loadRecord("test-data/images.csv", "IMG9100");
        ImageData imageData = new ImageData();

        imageData.setImageId(data.get("imageId"));
        imageData.setImageUrl(data.get("imageUrl"));
        imageData.setUploadedBy(data.get("uploadedBy"));
        imageData.setAssociatedWith(data.get("associatedWith"));
        imageData.setType(data.get("type"));
        imageData.setUploadedAt(1707436800000L);

        assertEquals(data.get("associatedWith"), imageData.getAssociatedWith());
        assertEquals(1707436800000L, imageData.getUploadedAt());
    }
}

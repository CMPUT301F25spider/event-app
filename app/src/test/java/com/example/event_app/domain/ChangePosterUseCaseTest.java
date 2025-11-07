package com.example.event_app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import com.example.event_app.data.EventRepository;
import com.example.event_app.data.PosterStorage;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ChangePosterUseCaseTest {

    @Test
    public void executeSuccessDeletesOldPoster() {
        FakePosterValidator validator = new FakePosterValidator(Result.ok(null));
        FakePosterStorage storage = new FakePosterStorage(Result.ok("https://cdn.example.com/posters/new.png"), Result.ok(null));
        FakeEventRepository repository = new FakeEventRepository(Result.ok(null));

        ChangePosterUseCase useCase = new ChangePosterUseCase(validator, storage, repository);
        TestCallback callback = new TestCallback();

        useCase.execute("EVT1001", "https://cdn.example.com/posters/old.png", Uri.parse("content://posters/new"), callback);

        assertTrue(callback.progressStarted.get());
        assertTrue(callback.progressFinished.get());
        assertEquals("https://cdn.example.com/posters/new.png", callback.successUrl.get());
        assertEquals("EVT1001", repository.lastEventId);
        assertEquals("https://cdn.example.com/posters/new.png", storage.lastUploadedUrl.get());
        assertEquals("https://cdn.example.com/posters/old.png", storage.lastDeletedUrl.get());
    }

    @Test
    public void executeFailsOnMissingEventId() {
        FakePosterValidator validator = new FakePosterValidator(Result.ok(null));
        FakePosterStorage storage = new FakePosterStorage(Result.ok("https://cdn.example.com/posters/new.png"), Result.ok(null));
        FakeEventRepository repository = new FakeEventRepository(Result.ok(null));

        ChangePosterUseCase useCase = new ChangePosterUseCase(validator, storage, repository);
        TestCallback callback = new TestCallback();

        useCase.execute("", "", Uri.parse("content://posters/new"), callback);

        assertTrue(callback.progressStarted.get());
        assertTrue(callback.progressFinished.get());
        assertEquals("Missing eventId", callback.errorMessage.get());
        assertFalse(repository.updateCalled.get());
    }

    @Test
    public void executeStopsWhenValidationFails() {
        FakePosterValidator validator = new FakePosterValidator(Result.err("Invalid image"));
        FakePosterStorage storage = new FakePosterStorage(Result.ok("https://cdn.example.com/posters/new.png"), Result.ok(null));
        FakeEventRepository repository = new FakeEventRepository(Result.ok(null));

        ChangePosterUseCase useCase = new ChangePosterUseCase(validator, storage, repository);
        TestCallback callback = new TestCallback();

        useCase.execute("EVT1001", "", Uri.parse("content://posters/new"), callback);

        assertEquals("Invalid image", callback.errorMessage.get());
        assertFalse(repository.updateCalled.get());
        assertFalse(storage.uploadCalled.get());
    }

    @Test
    public void executeCleansUpWhenDatabaseUpdateFails() {
        FakePosterValidator validator = new FakePosterValidator(Result.ok(null));
        FakePosterStorage storage = new FakePosterStorage(Result.ok("https://cdn.example.com/posters/new.png"), Result.ok(null));
        storage.deleteResultForCleanup = Result.ok(null);
        FakeEventRepository repository = new FakeEventRepository(Result.err("firestore down"));

        ChangePosterUseCase useCase = new ChangePosterUseCase(validator, storage, repository);
        TestCallback callback = new TestCallback();

        useCase.execute("EVT1001", "https://cdn.example.com/posters/old.png", Uri.parse("content://posters/new"), callback);

        assertEquals("Failed to update database: firestore down", callback.errorMessage.get());
        assertEquals("https://cdn.example.com/posters/new.png", storage.cleanedUpUrl.get());
        assertEquals("https://cdn.example.com/posters/new.png", storage.lastDeletedUrl.get());
        assertFalse(callback.successCalled.get());
    }

    @Test
    public void executeStopsWhenUploadFails() {
        FakePosterValidator validator = new FakePosterValidator(Result.ok(null));
        FakePosterStorage storage = new FakePosterStorage(Result.err("upload failed"), Result.ok(null));
        FakeEventRepository repository = new FakeEventRepository(Result.ok(null));

        ChangePosterUseCase useCase = new ChangePosterUseCase(validator, storage, repository);
        TestCallback callback = new TestCallback();

        useCase.execute("EVT1001", "https://cdn.example.com/posters/old.png", Uri.parse("content://posters/new"), callback);

        assertEquals("Upload failed: upload failed", callback.errorMessage.get());
        assertFalse(repository.updateCalled.get());
    }

    private static class FakePosterValidator extends PosterValidator {
        private final Result<Void> result;

        FakePosterValidator(Result<Void> result) {
            super(null, 0);
            this.result = result;
        }

        @Override
        public Result<Void> validate(Uri uri) {
            return result;
        }
    }

    private static class FakePosterStorage implements PosterStorage {
        private final Result<String> uploadResult;
        private final Result<Void> deleteResult;
        private final AtomicBoolean uploadCalled = new AtomicBoolean(false);
        private final AtomicReference<String> lastUploadedUrl = new AtomicReference<>(null);
        private final AtomicReference<String> lastDeletedUrl = new AtomicReference<>(null);
        private final AtomicReference<String> cleanedUpUrl = new AtomicReference<>(null);
        private Result<Void> deleteResultForCleanup = Result.ok(null);

        FakePosterStorage(Result<String> uploadResult, Result<Void> deleteResult) {
            this.uploadResult = uploadResult;
            this.deleteResult = deleteResult;
        }

        @Override
        public void upload(String eventId, Uri file, Callback<String> cb) {
            uploadCalled.set(true);
            cb.onComplete(uploadResult);
            if (uploadResult.isOk()) {
                lastUploadedUrl.set(uploadResult.data);
            }
        }

        @Override
        public void deleteByUrl(String fileUrl, Callback<Void> cb) {
            lastDeletedUrl.set(fileUrl);
            Result<Void> resultToReturn = deleteResult;
            if (fileUrl != null && fileUrl.equals(lastUploadedUrl.get())) {
                resultToReturn = deleteResultForCleanup;
                cleanedUpUrl.set(fileUrl);
            }
            cb.onComplete(resultToReturn);
        }
    }

    private static class FakeEventRepository implements EventRepository {
        private final Result<Void> result;
        private final AtomicBoolean updateCalled = new AtomicBoolean(false);
        private String lastEventId;

        FakeEventRepository(Result<Void> result) {
            this.result = result;
        }

        @Override
        public void updatePosterUrl(String eventId, String newUrl, Callback cb) {
            updateCalled.set(true);
            lastEventId = eventId;
            cb.onComplete(result);
        }
    }

    private static class TestCallback implements ChangePosterUseCase.Callback {
        private final AtomicBoolean progressStarted = new AtomicBoolean(false);
        private final AtomicBoolean progressFinished = new AtomicBoolean(false);
        private final AtomicReference<String> successUrl = new AtomicReference<>(null);
        private final AtomicReference<String> errorMessage = new AtomicReference<>(null);
        private final AtomicBoolean successCalled = new AtomicBoolean(false);

        @Override
        public void onProgress(boolean loading) {
            if (loading) {
                progressStarted.set(true);
            } else {
                progressFinished.set(true);
            }
        }

        @Override
        public void onSuccess(String newUrl) {
            successCalled.set(true);
            successUrl.set(newUrl);
        }

        @Override
        public void onError(String message) {
            errorMessage.set(message);
        }
    }
}

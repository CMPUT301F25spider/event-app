package com.example.event_app.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.event_app.data.EntrantsRepository;
import com.example.event_app.domain.EntrantRow;
import com.example.event_app.domain.EntrantsFilter;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;


import java.util.Collections;
import java.util.List;

public class EntrantsListViewModel extends ViewModel {

    private final EntrantsRepository repo;

    private String eventId;
    private EntrantsFilter currentFilter = EntrantsFilter.Selected;

    private final MutableLiveData<List<EntrantRow>> entrants = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> countLabel = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> toast = new MutableLiveData<>(null);

    private ListenerRegistration dataReg = null;
    private ListenerRegistration countReg = null; // may be NOOP in repo

    public EntrantsListViewModel(@NonNull EntrantsRepository repo) {
        this.repo = repo;
    }

    public void init(String eventId, EntrantsFilter initialFilter) {
        this.eventId = eventId;
        if (initialFilter != null) currentFilter = initialFilter;
        resubscribe();
    }

    public LiveData<List<EntrantRow>> entrants() { return entrants; }
    public LiveData<String> countLabel() { return countLabel; }
    public LiveData<Boolean> loading() { return loading; }
    public LiveData<String> toast() { return toast; }

    public void setFilter(EntrantsFilter filter) {
        if (filter == null) return;
        if (filter == currentFilter) return;
        currentFilter = filter;
        resubscribe();
    }

    private void resubscribe() {
        loading.postValue(true);

        if (dataReg != null) dataReg.remove();
        dataReg = repo.listenToEntrants(
                eventId,
                currentFilter,
                list -> {
                    loading.postValue(false);
                    entrants.postValue(list);
                },
                err -> {
                    loading.postValue(false);
                    toast.postValue("Failed to load entrants");
                }
        );

        // Count label (Selected/Confirmed only; repo may return NOOP)
        if (countReg != null) countReg.remove();
        countReg = repo.listenToCount(
                eventId,
                currentFilter,
                n -> {
                    if (currentFilter == EntrantsFilter.Selected) {
                        countLabel.postValue("Selected: " + n);
                    } else if (currentFilter == EntrantsFilter.Confirmed) {
                        countLabel.postValue("Confirmed: " + n);
                    } else {
                        countLabel.postValue("");
                    }
                },
                err -> { /* ignore badge errors silently */ }
        );
    }

    public void cancel(String entrantDocId, String reason, boolean alsoPromote) {
        repo.cancelEntrant(
                eventId,
                entrantDocId,
                reason,
                () -> {
                    if (alsoPromote) {
                        repo.promoteNextFromWaitlist(eventId, () -> {}, e -> {});
                    }
                    toast.postValue("Entrant cancelled");
                },
                e -> toast.postValue("Cancel failed")
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (dataReg != null) dataReg.remove();
        if (countReg != null) countReg.remove();
    }
}

package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PlayerProfile
import com.example.data.repository.PlayerRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class PlayerViewModel(
    private val repository: PlayerRepository,
    private val currentUserId: String,
) : ViewModel() {

    val profileState: StateFlow<UiState<PlayerProfile?>> = repository.observeProfile(currentUserId)
        .map<PlayerProfile?, UiState<PlayerProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing player profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load player profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun clearActionError() {
        _actionError.value = null
    }

    fun saveRegistration(
        fullName: String,
        phone: String,
        freeFireUid: String,
        freeFireIgn: String,
        termsAccepted: Boolean,
    ) {
        if (!termsAccepted || _isSubmitting.value) return
        _actionError.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            val result = repository.registerProfile(
                userId = currentUserId,
                fullName = fullName.trim(),
                phone = phone.trim(),
                freeFireUid = freeFireUid.trim(),
                freeFireIgn = freeFireIgn.trim(),
            )
            _isSubmitting.value = false
            result.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Could not save your TournaX profile."
            }
        }
    }

    fun updateProfile(
        fullName: String,
        phone: String,
        freeFireIgn: String,
        onUpdated: () -> Unit,
    ) {
        if (_isSubmitting.value) return
        _actionError.value = null
        _isSubmitting.value = true

        viewModelScope.launch {
            val result = repository.updateProfileDetails(
                userId = currentUserId,
                fullName = fullName,
                phone = phone,
                freeFireIgn = freeFireIgn,
            )
            _isSubmitting.value = false
            result.onSuccess {
                onUpdated()
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    companion object {
        private const val TAG = "PlayerViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

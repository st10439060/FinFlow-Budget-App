package com.finflow.app.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.User
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * ViewModel for local user authentication
 * Uses RoomDB for storing usernames and hashed passwords
 * Implements simple password hashing with SHA-256
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userDao = database.userDao()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _userId = MutableLiveData<Long?>()
    val userId: LiveData<Long?> = _userId

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val sharedPref = getApplication<Application>().getSharedPreferences(
            "finflow_prefs",
            Context.MODE_PRIVATE
        )
        val savedUserId = sharedPref.getLong("current_user_id", -1L)

        if (savedUserId != -1L) {
            _userId.value = savedUserId
            _authState.value = AuthState.Authenticated(savedUserId)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signUp(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Check if user already exists
                val existingUser = userDao.getUserByUsername(username)
                if (existingUser != null) {
                    _authState.value = AuthState.Error("Username already exists")
                    return@launch
                }

                // Hash password and create user
                val passwordHash = hashPassword(password)
                val user = User(
                    username = username,
                    passwordHash = passwordHash
                )
                val userId = userDao.insertUser(user)

                // Save to SharedPreferences
                val sharedPref = getApplication<Application>().getSharedPreferences(
                    "finflow_prefs",
                    Context.MODE_PRIVATE
                )
                sharedPref.edit().putLong("current_user_id", userId).apply()

                _userId.value = userId
                _authState.value = AuthState.Success("Account created successfully!")
                _authState.value = AuthState.Authenticated(userId)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = userDao.getUserByUsername(username)

                if (user == null) {
                    _authState.value = AuthState.Error("Invalid username or password")
                    return@launch
                }

                // Verify password
                val passwordHash = hashPassword(password)
                if (user.passwordHash != passwordHash) {
                    _authState.value = AuthState.Error("Invalid username or password")
                    return@launch
                }

                // Save to SharedPreferences
                val sharedPref = getApplication<Application>().getSharedPreferences(
                    "finflow_prefs",
                    Context.MODE_PRIVATE
                )
                sharedPref.edit().putLong("current_user_id", user.id).apply()

                _userId.value = user.id
                _authState.value = AuthState.Authenticated(user.id)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    fun signOut() {
        val sharedPref = getApplication<Application>().getSharedPreferences(
            "finflow_prefs",
            Context.MODE_PRIVATE
        )
        sharedPref.edit().remove("current_user_id").apply()

        _userId.value = null
        _authState.value = AuthState.Unauthenticated
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: Long) : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

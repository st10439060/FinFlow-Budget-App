package com.finflow.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.finflow.app.ui.viewmodels.AuthState
import com.finflow.app.ui.viewmodels.AuthViewModel

/**
 * LoginActivity - entry point for user authentication.
 * Supports both Login and Sign Up modes using a toggle link.
 *
 * Authentication:
 *  - Passwords are hashed with SHA-256 before storage in RoomDB.
 *  - On success, user ID is stored in SharedPreferences for session management.
 *
 * Implements: event handling, Intent navigation (login → main app), EditText usage.
 */
class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private val authViewModel: AuthViewModel by viewModels()
    private var isLoginMode = true

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvSwitchMode: TextView
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d(TAG, "LoginActivity created")

        etUsername = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvSwitchMode = findViewById(R.id.tv_switch_mode)
        progressBar = findViewById(R.id.progress_bar)

        // Event handling for the main action button (login or sign-up)
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Button clicked: mode=${if (isLoginMode) "login" else "signup"}, username=$username")

            if (isLoginMode) {
                authViewModel.signIn(username, password)
            } else {
                authViewModel.signUp(username, password)
            }
        }

        // Toggle between Login and Sign Up modes
        tvSwitchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                btnLogin.text = "Login"
                tvSwitchMode.text = "Don't have an account? Sign Up"
                Log.d(TAG, "Switched to login mode")
            } else {
                btnLogin.text = "Sign Up"
                tvSwitchMode.text = "Already have an account? Login"
                Log.d(TAG, "Switched to sign up mode")
            }
        }

        // Observe authentication state changes from the ViewModel
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    Log.d(TAG, "Auth state: Loading")
                    progressBar.visibility = View.VISIBLE
                    btnLogin.isEnabled = false
                }
                is AuthState.Authenticated -> {
                    Log.d(TAG, "Auth state: Authenticated userId=${state.userId}")
                    progressBar.visibility = View.GONE
                    // Intent navigates to MainActivity and clears the back stack
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is AuthState.Success -> {
                    Log.d(TAG, "Auth state: Success - ${state.message}")
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is AuthState.Error -> {
                    Log.e(TAG, "Auth state: Error - ${state.message}")
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is AuthState.Unauthenticated -> {
                    Log.d(TAG, "Auth state: Unauthenticated")
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }
    }
}

package com.finflow.app

import android.content.Intent
import android.os.Bundle
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
 * Login Activity for local authentication
 * Uses username/password stored in RoomDB
 * Implements event handling and intent navigation
 */
class LoginActivity : AppCompatActivity() {

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

        etUsername = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvSwitchMode = findViewById(R.id.tv_switch_mode)
        progressBar = findViewById(R.id.progress_bar)

        // Event handling for login button
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLoginMode) {
                authViewModel.signIn(username, password)
            } else {
                authViewModel.signUp(username, password)
            }
        }

        // Event handling for mode switching
        tvSwitchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                btnLogin.text = "Login"
                tvSwitchMode.text = "Don't have an account? Sign Up"
            } else {
                btnLogin.text = "Sign Up"
                tvSwitchMode.text = "Already have an account? Login"
            }
        }

        // Observe authentication state
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnLogin.isEnabled = false
                }
                is AuthState.Authenticated -> {
                    progressBar.visibility = View.GONE
                    // Intent to navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is AuthState.Success -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is AuthState.Unauthenticated -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }
    }
}

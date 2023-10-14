package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val authenticationViewModel by viewModels<FirebaseAuthenticationViewModel>()

    companion object {
        const val TAG = "AuthenticationActivity"
    }

    private var activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            val response = IdpResponse.fromResultIntent(data)
            if (result.resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "Signed-in user: ${FirebaseAuth.getInstance().currentUser?.displayName}")
                startReminderActivity()
                finish()
            } else {
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        authenticationViewModel.authenticationState.observe(this, Observer {
            if(it == AuthenticationState.AUTHENTICATED) {
                Log.d(TAG, "User authenticated")
                startReminderActivity()
                finish()
            } else {
                Log.e(TAG, "Error: Unauthenticated")
            }
        })
    }

    private fun startReminderActivity() {
        val intent = Intent(this, RemindersActivity::class.java)
        startActivity(intent)
    }

    fun onLoginButtonClicked(view: View) {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        activityResultLauncher.launch(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers)
                .build()
        )
    }
}
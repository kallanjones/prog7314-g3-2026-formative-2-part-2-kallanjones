package com.eventfinder.app.ui.screens.auth

import com.eventfinder.app.R
import com.eventfinder.app.data.repository.AuthRepository
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.security.GoogleSignInResult
import com.eventfinder.app.ui.components.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel-level tests for the login and password-reset flows (FR-01). A fake
 * [AuthRepository] keeps the test on the JVM with no Android dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthRepository : AuthRepository {
        override val currentUser: Flow<User?> = flowOf(null)
        override val biometricEnrolled: Flow<Boolean> = flowOf(false)
        var resetResult: Result<Unit> = Result.success(Unit)
        var lastReset: Pair<String, String>? = null

        /** Email + full name of the last Google sign-in, or null if never called. */
        var lastGoogleSignIn: Pair<String, String>? = null

        override suspend fun signInWithGoogle(
            email: String,
            fullName: String,
            language: String
        ): Result<User> {
            lastGoogleSignIn = email to fullName
            return Result.success(
                User(
                    id = "google-user",
                    fullName = fullName,
                    email = email,
                    preferredLanguage = language,
                    defaultCity = "South Africa",
                    defaultRadiusKm = 50,
                    biometricEnabled = false,
                    createdAt = 0L
                )
            )
        }

        override suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
            lastReset = email to newPassword
            return resetResult
        }

        override suspend fun register(
            fullName: String,
            email: String,
            password: String,
            language: String
        ): Result<User> = error("not used")

        override suspend fun login(email: String, password: String): Result<User> = error("not used")
        override suspend fun biometricLogin(): Result<User> = error("not used")
        override suspend fun logout() = error("not used")
        override suspend fun updateProfile(fullName: String, email: String): Result<User> =
            error("not used")

        override suspend fun setBiometricEnabled(enabled: Boolean) = error("not used")
        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
            error("not used")

        override suspend fun deleteAccount(): Result<Unit> = error("not used")
        override suspend fun isLoggedIn(): Boolean = false
        override suspend fun continueAsGuest(): Result<User> = error("not used")
    }

    @Test
    fun `mismatched confirmation never reaches the repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        val messages = mutableListOf<UiMessage>()
        val job = launch { viewModel.messages.collect { messages += it } }

        viewModel.requestPasswordReset("NewPass1!", "Different1!")
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(UiMessage.Resource(R.string.passwords_mismatch)), messages)
        assertNull(repo.lastReset)
    }

    @Test
    fun `successful local reset shows completion`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        repo.resetResult = Result.success(Unit)
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        viewModel.onEmailChange("user@example.com")
        val messages = mutableListOf<UiMessage>()
        val job = launch { viewModel.messages.collect { messages += it } }

        viewModel.requestPasswordReset("NewPass1!", "NewPass1!")
        advanceUntilIdle()
        job.cancel()

        assertTrue(messages.contains(UiMessage.Resource(R.string.reset_success)))
        assertTrue(viewModel.uiState.value.passwordResetComplete)
        assertEquals("user@example.com" to "NewPass1!", repo.lastReset)
    }

    @Test
    fun `reset with invalid email shows error`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        repo.resetResult = Result.failure(IllegalArgumentException("invalid_email"))
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        viewModel.onEmailChange("bad-email")
        val messages = mutableListOf<UiMessage>()
        val job = launch { viewModel.messages.collect { messages += it } }

        viewModel.requestPasswordReset("NewPass1!", "NewPass1!")
        advanceUntilIdle()
        job.cancel()

        assertTrue(messages.any { it == UiMessage.Resource(R.string.invalid_email) })
        assertFalse(viewModel.uiState.value.passwordResetComplete)
    }

    @Test
    fun `reset with unknown email shows error`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        repo.resetResult = Result.failure(IllegalArgumentException("unknown_email"))
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        viewModel.onEmailChange("unknown@example.com")
        val messages = mutableListOf<UiMessage>()
        val job = launch { viewModel.messages.collect { messages += it } }

        viewModel.requestPasswordReset("NewPass1!", "NewPass1!")
        advanceUntilIdle()
        job.cancel()

        assertTrue(messages.any { it == UiMessage.Resource(R.string.unknown_email) })
        assertFalse(viewModel.uiState.value.passwordResetComplete)
    }

    @Test
    fun `reset with weak password shows error`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        repo.resetResult = Result.failure(IllegalArgumentException("weak_password"))
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        viewModel.onEmailChange("user@example.com")
        val messages = mutableListOf<UiMessage>()
        val job = launch { viewModel.messages.collect { messages += it } }

        viewModel.requestPasswordReset("weak", "weak")
        advanceUntilIdle()
        job.cancel()

        assertTrue(messages.any { it == UiMessage.Resource(R.string.weak_password) })
        assertFalse(viewModel.uiState.value.passwordResetComplete)
    }

    @Test
    fun `reset with same password shows error`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        repo.resetResult = Result.failure(IllegalArgumentException("same_password"))
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        viewModel.onEmailChange("user@example.com")
        val messages = mutableListOf<UiMessage>()
        val job = launch { viewModel.messages.collect { messages += it } }

        viewModel.requestPasswordReset("SamePass1!", "SamePass1!")
        advanceUntilIdle()
        job.cancel()

        assertTrue(messages.any { it == UiMessage.Resource(R.string.password_unchanged) })
        assertFalse(viewModel.uiState.value.passwordResetComplete)
    }

    @Test
    fun `consumePasswordReset clears the completion flag`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val viewModel = LoginViewModel(repo, biometricAvailable = false)

        viewModel.uiState.value.let { state ->
            assertFalse(state.passwordResetComplete)
        }

        viewModel.consumePasswordReset()
        assertFalse(viewModel.uiState.value.passwordResetComplete)
    }

    @Test
    fun `a successful Google sign-in reaches the repository and signals success`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        var loggedIn = false

        viewModel.signInWithGoogle(
            signIn = { GoogleSignInResult.Success("Tester@Example.com", "Test User") },
            onSuccess = { loggedIn = true }
        )
        advanceUntilIdle()

        assertEquals("Tester@Example.com" to "Test User", repo.lastGoogleSignIn)
        assertTrue(loggedIn)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `a cancelled Google sign-in never reaches the repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val viewModel = LoginViewModel(repo, biometricAvailable = false)
        var loggedIn = false

        viewModel.signInWithGoogle(
            signIn = { GoogleSignInResult.Cancelled },
            onSuccess = { loggedIn = true }
        )
        advanceUntilIdle()

        assertNull(repo.lastGoogleSignIn)
        assertFalse(loggedIn)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `an unconfigured Google sign-in never reaches the repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val viewModel = LoginViewModel(repo, biometricAvailable = false)

        viewModel.signInWithGoogle(
            signIn = { GoogleSignInResult.NotConfigured },
            onSuccess = { }
        )
        advanceUntilIdle()

        assertNull(repo.lastGoogleSignIn)
    }
}
package com.example.clearspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.clearspace.data.model.ApprovalDecision
import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.manager.AppUsageManager
import com.example.clearspace.manager.FriendManager
import com.example.clearspace.manager.LimitManager
import com.example.clearspace.service.AuthService
import com.example.clearspace.service.ChallengeService
import com.example.clearspace.service.NotificationService
import com.example.clearspace.service.UnlockService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val demoOutput = runBackendDemo()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Text(
                    text = demoOutput,
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    private fun runBackendDemo(): String {
        val authService = AuthService()
        val appUsageManager = AppUsageManager()
        val limitManager = LimitManager()
        val challengeService = ChallengeService()
        val unlockService = UnlockService()
        val notificationService = NotificationService()
        val friendManager = FriendManager()

        val log = StringBuilder()

        log.appendLine("=== CLEARSPACE BACKEND DEMO ===")
        log.appendLine()

        // 1. Register user
        val user = authService.register(
            name = "Arda",
            email = "arda@email.com"
        )
        log.appendLine("User created:")
        log.appendLine("Name: ${user.name}")
        log.appendLine("Email: ${user.email}")
        log.appendLine("Daily Limit: ${user.dailyLimitMinutes} min")
        log.appendLine("Session Limit: ${user.sessionLimitMinutes} min")
        log.appendLine()

        // 2. Create regulated app
        val app = RegulatedApp(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            isEnabled = true,
            sessionLimitMinutes = 10,
            dailyLimitMinutes = 60
        )
        log.appendLine("Regulated app created:")
        log.appendLine("App: ${app.displayName}")
        log.appendLine("Package: ${app.packageName}")
        log.appendLine()

        // 3. Start session via manager
        val session = appUsageManager.beginUsageSession(user, app)
        log.appendLine("Session started:")
        log.appendLine("Session ID: ${session.sessionId}")
        log.appendLine("Status: ${session.status}")
        log.appendLine()

        // 4. Simulate time passing
        session.durationSeconds = 700
        log.appendLine("Simulated session duration: ${session.durationSeconds} seconds")
        log.appendLine()

        // 5. Check limit via manager
        val sessionBreached = appUsageManager.isSessionOverLimit(session)
        log.appendLine("Session limit breached? $sessionBreached")

        if (sessionBreached) {
            session.breachedSessionLimit = true
            log.appendLine("Session marked as breached.")
        }
        log.appendLine()

        // 6. Challenge flow
        val challenge = challengeService.getRandomChallenge()
        val attempt = challengeService.createChallengeAttempt(session, challenge)

        log.appendLine("Challenge assigned:")
        log.appendLine("Title: ${challenge.title}")
        log.appendLine("Type: ${challenge.type}")
        log.appendLine()

        challengeService.completeChallengeAttempt(attempt)
        user.points += attempt.pointsAwarded

        log.appendLine("Challenge completed:")
        log.appendLine("Outcome: ${attempt.outcome}")
        log.appendLine("Points awarded: ${attempt.pointsAwarded}")
        log.appendLine("User total points: ${user.points}")
        log.appendLine()

        // 7. End session via manager
        appUsageManager.endUsageSession(session)
        log.appendLine("Session ended:")
        log.appendLine("Status: ${session.status}")
        log.appendLine("Final duration: ${session.durationSeconds} seconds")
        log.appendLine()

        // 8. Friend flow via manager
        val friend = friendManager.addFriend(
            user = user,
            displayName = "Lucas",
            contactHandle = "lucas@email.com"
        )
        friendManager.acceptFriend(friend)

        log.appendLine("Friend added:")
        log.appendLine("Friend name: ${friend.displayName}")
        log.appendLine("Friend status: ${friend.status}")
        log.appendLine()

        // 9. Unlock flow
        val unlockRequest = unlockService.createUnlockRequest(
            user = user,
            app = app,
            requiredApprovals = 1,
            durationMinutes = 30
        )
        log.appendLine("Unlock request created:")
        log.appendLine("Request ID: ${unlockRequest.requestId}")
        log.appendLine("Status: ${unlockRequest.status}")
        log.appendLine()

        val approval = unlockService.addApproval(
            request = unlockRequest,
            friend = friend,
            decision = ApprovalDecision.APPROVE
        )
        log.appendLine("Approval submitted:")
        log.appendLine("Friend: ${approval.friend.displayName}")
        log.appendLine("Decision: ${approval.decision}")
        log.appendLine("Request status after approval: ${unlockRequest.status}")
        log.appendLine()

        // 10. Notification
        val notification = notificationService.createUnlockRequestNotification(user, unlockRequest)
        log.appendLine("Notification created:")
        log.appendLine("Title: ${notification.title}")
        log.appendLine("Body: ${notification.body}")
        log.appendLine()

        // 11. Daily limit / escalation checks
        val userSessions = appUsageManager.run {
            listOf(session)
        }

        val dailyLimitReached = limitManager.hasDailyLimitBeenReached(user, userSessions)
        val shouldEscalate = limitManager.shouldEscalate(user, userSessions)

        log.appendLine("Daily limit reached? $dailyLimitReached")
        log.appendLine("Should escalate? $shouldEscalate")
        log.appendLine()

        log.appendLine("=== DEMO COMPLETE ===")

        return log.toString()
    }
}
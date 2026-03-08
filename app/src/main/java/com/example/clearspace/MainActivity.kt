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
import com.example.clearspace.data.model.Friend
import com.example.clearspace.data.model.FriendStatus
import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.service.AuthService
import com.example.clearspace.service.BreachDetectionService
import com.example.clearspace.service.ChallengeService
import com.example.clearspace.service.NotificationService
import com.example.clearspace.service.SessionService
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
        val sessionService = SessionService()
        val breachDetectionService = BreachDetectionService()
        val challengeService = ChallengeService()
        val unlockService = UnlockService()
        val notificationService = NotificationService()

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

        // 3. Start session
        val session = sessionService.startSession(user, app)
        log.appendLine("Session started:")
        log.appendLine("Session ID: ${session.sessionId}")
        log.appendLine("Status: ${session.status}")
        log.appendLine()

        // 4. Simulate time passing by forcing duration
        session.durationSeconds = 700 // 11 min 40 sec
        log.appendLine("Simulated session duration: ${session.durationSeconds} seconds")
        log.appendLine()

        // 5. Breach detection
        val breached = breachDetectionService.hasSessionLimitBeenBreached(session)
        log.appendLine("Session limit breached? $breached")

        if (breached) {
            breachDetectionService.markSessionBreach(session)
            log.appendLine("Session marked as breached.")
        }
        log.appendLine()

        // 6. Create challenge
        val challenge = challengeService.getRandomChallenge()
        val attempt = challengeService.createChallengeAttempt(session, challenge)
        log.appendLine("Challenge assigned:")
        log.appendLine("Title: ${challenge.title}")
        log.appendLine("Type: ${challenge.type}")
        log.appendLine()

        // 7. Complete challenge
        challengeService.completeChallengeAttempt(attempt)
        user.points += attempt.pointsAwarded
        log.appendLine("Challenge completed:")
        log.appendLine("Outcome: ${attempt.outcome}")
        log.appendLine("Points awarded: ${attempt.pointsAwarded}")
        log.appendLine("User total points: ${user.points}")
        log.appendLine()

        // 8. End session
        sessionService.endSession(session)
        log.appendLine("Session ended:")
        log.appendLine("Status: ${session.status}")
        log.appendLine("Final duration: ${session.durationSeconds} seconds")
        log.appendLine()

        // 9. Simulate accountability
        val friend = Friend(
            friendId = "f1",
            user = user,
            displayName = "Lucas",
            contactHandle = "lucas@email.com",
            status = FriendStatus.ACCEPTED
        )
        log.appendLine("Friend added:")
        log.appendLine("Friend name: ${friend.displayName}")
        log.appendLine()

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

        log.appendLine("=== DEMO COMPLETE ===")

        return log.toString()
    }
}
package com.example.clearspace

import com.example.clearspace.data.model.ApprovalDecision
import com.example.clearspace.data.model.AttemptOutcome
import com.example.clearspace.data.model.ChallengeAttempt
import com.example.clearspace.data.model.Friend
import com.example.clearspace.data.model.NotificationType
import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.data.model.RequestStatus
import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.SessionStatus
import com.example.clearspace.data.model.UnlockRequest
import com.example.clearspace.data.model.User
import com.example.clearspace.data.repository.ChallengeRepository
import com.example.clearspace.data.repository.UnlockRequestRepository
import com.example.clearspace.manager.AppUsageManager
import com.example.clearspace.service.AuthService
import com.example.clearspace.service.BreachDetectionService
import com.example.clearspace.service.ChallengeService
import com.example.clearspace.service.NotificationService
import com.example.clearspace.service.SessionService
import com.example.clearspace.service.UnlockService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BackendTests {

    private lateinit var authService: AuthService
    private lateinit var sessionService: SessionService
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var testUser: User

    @Before
    fun setUp() {
        authService = AuthService()
        sessionService = SessionService()
        appUsageManager = AppUsageManager(sessionService = sessionService)
        testUser = authService.register("Test User", "test@test.com")
    }

    @Test
    fun TC_201_Backend_saves_user_onboarding_preferences() {
        println("Starting TC-201: Backend saves user onboarding preferences")
        // Objective: Verify selected apps and limits are stored correctly.
        // Input: User ID, selected apps, session limit, daily limit.

        // Simulating onboarding preferences save
        println("Setting user limits: daily=120, session=30")
        testUser.dailyLimitMinutes = 120
        testUser.sessionLimitMinutes = 30

        RegulatedApp(
            packageName = "com.zhiliaoapp.musically",
            displayName = "TikTok",
            isEnabled = true,
            sessionLimitMinutes = 30,
            dailyLimitMinutes = 120
        )

        // Expected Result: Preferences are stored against correct user.
        println("Fetching user to verify stored preferences")
        val fetchedUser = authService.login("test@test.com")
        assertNotNull(fetchedUser)
        assertEquals(120, fetchedUser?.dailyLimitMinutes)
        assertEquals(30, fetchedUser?.sessionLimitMinutes)
        println("TC-201 Completed successfully")
    }

    @Test
    fun TC_202_Backend_starts_monitoring_regulated_app_session() {
        println("Starting TC-202: Backend starts monitoring regulated app session")
        // Objective: Verify session begins when monitored app opens.
        
        val targetApp = RegulatedApp(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            isEnabled = true
        )

        // Step: Send app-open event for regulated app. (Simulated via AppUsageManager)
        println("Simulating app open event for regulated app")
        val session = appUsageManager.beginUsageSession(testUser, targetApp)
        assertNotNull(session)
        val startedSession = session!!

        // Expected Result: Backend creates session entry. Session start timestamp is recorded.
        println("Verifying session creation and start timestamp")
        assertEquals(SessionStatus.ACTIVE, startedSession.status)
        assertTrue(startedSession.startTime > 0)
        assertEquals("com.instagram.android", startedSession.app.packageName)
        assertEquals(testUser.userId, startedSession.user.userId)
        
        // Verify via service lookup
        val activeSession = sessionService.getActiveSessionForUser(testUser)
        assertNotNull(activeSession)
        assertEquals(startedSession.sessionId, activeSession?.sessionId)
        println("TC-202 Completed successfully")
    }

    @Test
    fun TC_203_Backend_ignores_non_regulated_apps() {
        println("Starting TC-203: Backend ignores non-regulated apps")
        // Objective: Ensure system only tracks selected apps.
        
        val unregulatedApp = RegulatedApp(
            packageName = "com.android.calculator2",
            displayName = "Calculator",
            isEnabled = false
        )

        // Step: Send app-open event for non-regulated app.
        // The business logic should potentially ignore it if it's disabled or not part of the target list
        println("Simulating app open event for non-regulated app (Calculator)")
        
        val session = appUsageManager.beginUsageSession(testUser, unregulatedApp)

        // Expected Result: No monitored session is created.
        assertNull("Backend should ignore non-regulated apps", session)

        // Verify no active session registered
        val activeSession = sessionService.getActiveSessionForUser(testUser)
        assertNull(activeSession)
        println("TC-203 Completed successfully")
    }

    @Test
    fun TC_204_Backend_triggers_interruption_at_session_limit() {
        println("Starting TC-204: Backend triggers interruption at session limit")
        // Objective: Verify session threshold logic works.
        
        val targetApp = RegulatedApp(
            packageName = "com.twitter.android",
            displayName = "X (Twitter)",
            isEnabled = true,
            sessionLimitMinutes = 10 // 10 minutes wait
        )

        // Step: Start monitored session.
        println("Testing monitored session with 10 min limit")
        val session = appUsageManager.beginUsageSession(testUser, targetApp)
        assertNotNull(session)
        val startedSession = session!!
        
        // Step: Simulate usage reaching configured threshold. 
        println("Fast-forwarding duration to 15 mins (exceeding limit)")
        // We fast-forward the duration to exceed the limit.
        startedSession.durationSeconds = 15 * 60 // 15 mins > 10 mins

        // Check evaluating limits
        println("Validating session breach flag")
        val isOverLimit = appUsageManager.isSessionOverLimit(startedSession)

        // Expected Result: Backend flags session breach. Interruption event/response is returned.
        assertTrue("Session should be flagged as breaching the threshold limits", isOverLimit)
        println("TC-204 Completed successfully")
    }

    @Test
    fun TC_205_Backend_flags_session_limit_at_exact_threshold() {
        val breachDetectionService = BreachDetectionService()
        val targetApp = RegulatedApp(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            isEnabled = true,
            sessionLimitMinutes = 10
        )

        val exactThresholdSession = Session(
            sessionId = "session-exact-threshold",
            user = testUser,
            app = targetApp,
            startTime = System.currentTimeMillis() - (10 * 60 * 1000L),
            durationSeconds = 10 * 60
        )

        assertTrue(breachDetectionService.hasSessionLimitBeenBreached(exactThresholdSession))

        exactThresholdSession.durationSeconds = (10 * 60) - 1
        assertFalse(breachDetectionService.hasSessionLimitBeenBreached(exactThresholdSession))
    }

    @Test
    fun TC_206_Backend_calculates_duration_from_timestamps_when_duration_missing() {
        val recentStart = System.currentTimeMillis() - 65_000L
        val targetApp = RegulatedApp(
            packageName = "com.youtube.android",
            displayName = "YouTube",
            isEnabled = true
        )

        val inferredDurationSession = Session(
            sessionId = "session-duration-inferred",
            user = testUser,
            app = targetApp,
            startTime = recentStart,
            endTime = recentStart + 65_000L,
            durationSeconds = 0
        )

        assertEquals(65, sessionService.calculateDurationSeconds(inferredDurationSession))

        inferredDurationSession.durationSeconds = 42
        assertEquals(42, sessionService.calculateDurationSeconds(inferredDurationSession))
    }

    @Test
    fun TC_207_Backend_filters_disabled_challenges_from_active_pool() {
        val challengeRepository = ChallengeRepository()
        val challengeService = ChallengeService(challengeRepository)

        assertEquals(4, challengeService.getAllChallenges().size)
        assertEquals(4, challengeService.getEnabledChallenges().size)

        val disabledChallenge = challengeRepository.getChallengeById("c2")!!.copy(isEnabled = false)
        challengeRepository.updateChallenge(disabledChallenge)

        val enabledChallenges = challengeService.getEnabledChallenges()
        assertEquals(3, enabledChallenges.size)
        assertTrue(enabledChallenges.none { it.challengeId == "c2" })
    }

    @Test
    fun TC_208_Backend_records_challenge_attempt_completion_and_points() {
        val challengeRepository = ChallengeRepository()
        val challengeService = ChallengeService(challengeRepository)
        val targetApp = RegulatedApp(
            packageName = "com.tiktok.android",
            displayName = "TikTok",
            isEnabled = true
        )

        val session = sessionService.startSession(testUser, targetApp)
        val challenge = challengeService.getEnabledChallenges().first { it.challengeId == "c1" }

        val attempt = challengeService.createChallengeAttempt(session, challenge)
        assertEquals(AttemptOutcome.FAILED, attempt.outcome)

        val completedAttempt = challengeService.completeChallengeAttempt(attempt)
        assertEquals(AttemptOutcome.COMPLETED, completedAttempt.outcome)
        assertEquals(challenge.rewardPoints, completedAttempt.pointsAwarded)
        assertNotNull(completedAttempt.completedAt)
        assertEquals(1, challengeService.getAttemptsForSession(session).size)

        val skippedAttempt = challengeService.skipChallengeAttempt(
            ChallengeAttempt(
                attemptId = "attempt-skipped",
                session = session,
                challenge = challenge,
                startedAt = System.currentTimeMillis() - 5_000L
            )
        )
        assertEquals(AttemptOutcome.SKIPPED, skippedAttempt.outcome)
        assertEquals(0, skippedAttempt.pointsAwarded)
    }

    @Test
    fun TC_209_Backend_approves_unlock_request_after_required_approvals() {
        val unlockRequestRepository = UnlockRequestRepository()
        val unlockService = UnlockService(unlockRequestRepository)
        val targetApp = RegulatedApp(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            isEnabled = true
        )

        val request = unlockService.createUnlockRequest(
            user = testUser,
            app = targetApp,
            requiredApprovals = 2,
            durationMinutes = 30
        )

        val friendOne = Friend(
            friendId = "friend-1",
            user = testUser,
            displayName = "Sam",
            contactHandle = "@sam"
        )
        val friendTwo = Friend(
            friendId = "friend-2",
            user = testUser,
            displayName = "Riley",
            contactHandle = "@riley"
        )

        unlockService.addApproval(request, friendOne, ApprovalDecision.APPROVE)
        assertEquals(RequestStatus.PENDING, request.status)
        assertFalse(unlockService.isRequestApproved(request))

        unlockService.addApproval(request, friendTwo, ApprovalDecision.APPROVE)
        assertTrue(unlockService.isRequestApproved(request))
        assertEquals(RequestStatus.APPROVED, request.status)
        assertEquals(2, unlockService.getApprovalsForRequest(request).size)
        assertEquals(1, unlockService.getRequestsForUser(testUser).size)
    }

    @Test
    fun TC_210_Backend_keeps_expired_unlock_requests_expired_even_with_approval() {
        val unlockRequestRepository = UnlockRequestRepository()
        val unlockService = UnlockService(unlockRequestRepository)
        val targetApp = RegulatedApp(
            packageName = "com.youtube.android",
            displayName = "YouTube",
            isEnabled = true
        )
        val expiredRequest = UnlockRequest(
            requestId = "request-expired",
            user = testUser,
            targetApp = targetApp,
            createdAt = System.currentTimeMillis() - 60_000L,
            expiresAt = System.currentTimeMillis() - 1_000L,
            requiredApprovals = 1
        )

        unlockRequestRepository.addUnlockRequest(expiredRequest)

        val friend = Friend(
            friendId = "friend-3",
            user = testUser,
            displayName = "Taylor",
            contactHandle = "@taylor"
        )

        unlockService.addApproval(expiredRequest, friend, ApprovalDecision.APPROVE)

        assertEquals(RequestStatus.EXPIRED, expiredRequest.status)
        assertTrue(unlockService.isRequestExpired(expiredRequest))
        assertEquals(1, unlockService.getApprovalsForRequest(expiredRequest).size)
    }

    @Test
    fun TC_211_Backend_tracks_notifications_and_unread_state() {
        val notificationService = NotificationService()
        val unlockService = UnlockService()
        val targetApp = RegulatedApp(
            packageName = "com.zhiliaoapp.musically",
            displayName = "TikTok",
            isEnabled = true
        )
        val request = unlockService.createUnlockRequest(testUser, targetApp)

        val unlockRequestNotification = notificationService.createUnlockRequestNotification(testUser, request)
        val warningNotification = notificationService.createWarningNotification(testUser, "Instagram")
        val reminderNotification = notificationService.createReminderNotification(testUser, "Time for a short break")

        assertTrue(unlockRequestNotification.body.contains("TikTok"))
        assertEquals(NotificationType.UNLOCK_REQUEST, unlockRequestNotification.type)
        assertEquals(3, notificationService.getNotificationsForUser(testUser).size)
        assertEquals(3, notificationService.getUnreadNotificationsForUser(testUser).size)

        notificationService.markAsRead(warningNotification)

        assertNotNull(warningNotification.readAt)
        assertEquals(2, notificationService.getUnreadNotificationsForUser(testUser).size)
        assertTrue(notificationService.getAllNotifications().contains(reminderNotification))
    }
}

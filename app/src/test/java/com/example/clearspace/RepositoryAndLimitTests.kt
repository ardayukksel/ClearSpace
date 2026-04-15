package com.example.clearspace

import com.example.clearspace.data.model.ApprovalDecision
import com.example.clearspace.data.model.AttemptOutcome
import com.example.clearspace.data.model.Challenge
import com.example.clearspace.data.model.ChallengeAttempt
import com.example.clearspace.data.model.ChallengeType
import com.example.clearspace.data.model.Notification
import com.example.clearspace.data.model.NotificationType
import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.data.model.RequestStatus
import com.example.clearspace.data.model.Session
import com.example.clearspace.data.model.SessionStatus
import com.example.clearspace.data.model.UnlockApproval
import com.example.clearspace.data.model.UnlockRequest
import com.example.clearspace.data.model.User
import com.example.clearspace.data.repository.ChallengeRepository
import com.example.clearspace.data.repository.NotificationRepository
import com.example.clearspace.data.repository.SessionRepository
import com.example.clearspace.data.repository.UnlockRequestRepository
import com.example.clearspace.data.repository.UserRepository
import com.example.clearspace.manager.LimitManager
import com.example.clearspace.service.BreachDetectionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositoryAndLimitTests {

    private lateinit var userRepository: UserRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var challengeRepository: ChallengeRepository
    private lateinit var unlockRequestRepository: UnlockRequestRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var limitManager: LimitManager
    private lateinit var breachDetectionService: BreachDetectionService
    private lateinit var user: User

    @Before
    fun setUp() {
        userRepository = UserRepository()
        sessionRepository = SessionRepository()
        challengeRepository = ChallengeRepository()
        unlockRequestRepository = UnlockRequestRepository()
        notificationRepository = NotificationRepository()
        limitManager = LimitManager()
        breachDetectionService = BreachDetectionService()
        user = User(userId = "user-1", name = "Test User", email = "test@example.com")
    }

    private fun logCase(message: String) {
        println("[RepositoryAndLimitTests] $message")
    }

    @Test
    fun TC_212_UserRepository_supports_crud_and_case_insensitive_lookup() {
        logCase("Starting TC-212")
        val secondUser = User(userId = "user-2", name = "Second", email = "second@example.com")
        userRepository.addUser(user)
        userRepository.addUser(secondUser)

        assertEquals(2, userRepository.getAllUsers().size)
        assertEquals(user, userRepository.getUserById("user-1"))
        assertEquals(secondUser, userRepository.getUserByEmail("SECOND@example.com"))

        val updated = user.copy(name = "Updated Name")
        userRepository.updateUser(updated)
        assertEquals("Updated Name", userRepository.getUserById("user-1")?.name)

        userRepository.removeUser("user-2")
        assertNull(userRepository.getUserById("user-2"))
        assertEquals(1, userRepository.getAllUsers().size)

        userRepository.clearAll()
        assertTrue(userRepository.getAllUsers().isEmpty())
        logCase("Finished TC-212")
    }

    @Test
    fun TC_213_SessionRepository_tracks_active_sessions_and_updates_status() {
        logCase("Starting TC-213")
        val app = RegulatedApp("com.example.app", "Example App", isEnabled = true)
        val activeSession = Session(
            sessionId = "session-active",
            user = user,
            app = app,
            startTime = System.currentTimeMillis()
        )
        val endedSession = Session(
            sessionId = "session-ended",
            user = user,
            app = app,
            startTime = System.currentTimeMillis() - 60_000L,
            status = SessionStatus.ENDED
        )

        sessionRepository.addSession(activeSession)
        sessionRepository.addSession(endedSession)

        assertEquals(2, sessionRepository.getAllSessions().size)
        assertEquals(1, sessionRepository.getActiveSessions().size)
        assertEquals(activeSession.sessionId, sessionRepository.getActiveSessionForUser(user.userId)?.sessionId)

        val updatedActiveSession = activeSession.copy(status = SessionStatus.ENDED)
        sessionRepository.updateSession(updatedActiveSession)
        assertTrue(sessionRepository.getActiveSessions().isEmpty())

        sessionRepository.removeSession("session-ended")
        assertNull(sessionRepository.getSessionById("session-ended"))

        sessionRepository.clearAll()
        assertTrue(sessionRepository.getAllSessions().isEmpty())
        logCase("Finished TC-213")
    }

    @Test
    fun TC_214_ChallengeRepository_filters_enabled_challenges_and_session_attempts() {
        logCase("Starting TC-214")
        val enabledChallenge = Challenge(
            challengeId = "challenge-1",
            type = ChallengeType.BREATHING,
            title = "Breathe",
            description = "Take a breath",
            difficulty = 1,
            rewardPoints = 5,
            isEnabled = true
        )
        val disabledChallenge = enabledChallenge.copy(challengeId = "challenge-2", isEnabled = false)
        challengeRepository.addChallenges(listOf(enabledChallenge, disabledChallenge))

        assertEquals(2, challengeRepository.getAllChallenges().size)
        assertEquals(1, challengeRepository.getEnabledChallenges().size)
        assertEquals(enabledChallenge, challengeRepository.getChallengeById("challenge-1"))

        val session = Session(
            sessionId = "session-1",
            user = user,
            app = RegulatedApp("com.example.app", "Example App")
        )
        val attempt = ChallengeAttempt(
            attemptId = "attempt-1",
            session = session,
            challenge = enabledChallenge,
            outcome = AttemptOutcome.FAILED,
            pointsAwarded = 0
        )

        challengeRepository.addChallengeAttempt(attempt)
        assertEquals(1, challengeRepository.getAllChallengeAttempts().size)
        assertEquals(1, challengeRepository.getAttemptsForSession(session.sessionId).size)

        challengeRepository.clearAll()
        assertTrue(challengeRepository.getAllChallenges().isEmpty())
        assertTrue(challengeRepository.getAllChallengeAttempts().isEmpty())
        logCase("Finished TC-214")
    }

    @Test
    fun TC_215_UnlockRequestRepository_tracks_requests_approvals_and_pending_state() {
        logCase("Starting TC-215")
        val app = RegulatedApp("com.example.app", "Example App")
        val request = UnlockRequest(
            requestId = "request-1",
            user = user,
            targetApp = app,
            expiresAt = System.currentTimeMillis() + 60_000L,
            requiredApprovals = 2
        )
        val otherRequest = UnlockRequest(
            requestId = "request-2",
            user = User("user-2", "Other", "other@example.com"),
            targetApp = app,
            expiresAt = System.currentTimeMillis() + 60_000L
        )
        unlockRequestRepository.addUnlockRequest(request)
        unlockRequestRepository.addUnlockRequest(otherRequest)

        assertEquals(2, unlockRequestRepository.getAllUnlockRequests().size)
        assertEquals(1, unlockRequestRepository.getUnlockRequestsForUser(user.userId).size)
        assertEquals(2, unlockRequestRepository.getPendingRequests().size)

        val approval = UnlockApproval(
            approvalId = "approval-1",
            request = request,
            friend = com.example.clearspace.data.model.Friend(
                friendId = "friend-1",
                user = user,
                displayName = "Friend",
                contactHandle = "@friend"
            ),
            decision = ApprovalDecision.APPROVE
        )
        unlockRequestRepository.addApproval(approval)
        assertEquals(1, unlockRequestRepository.getApprovalsForRequest(request.requestId).size)
        assertEquals(1, unlockRequestRepository.getAllApprovals().size)

        request.status = RequestStatus.APPROVED
        unlockRequestRepository.updateUnlockRequest(request)
        assertEquals(RequestStatus.APPROVED, unlockRequestRepository.getUnlockRequestById(request.requestId)?.status)

        unlockRequestRepository.clearAll()
        assertTrue(unlockRequestRepository.getAllUnlockRequests().isEmpty())
        assertTrue(unlockRequestRepository.getAllApprovals().isEmpty())
        logCase("Finished TC-215")
    }

    @Test
    fun TC_216_NotificationRepository_filters_by_user_and_read_state() {
        logCase("Starting TC-216")
        val unread = Notification(
            notificationId = "n-1",
            user = user,
            type = NotificationType.WARNING,
            title = "Warning",
            body = "You are close to your limit"
        )
        val read = Notification(
            notificationId = "n-2",
            user = user,
            type = NotificationType.REMINDER,
            title = "Reminder",
            body = "Take a break",
            readAt = System.currentTimeMillis()
        )
        val otherUserNotification = Notification(
            notificationId = "n-3",
            user = User("user-2", "Other", "other@example.com"),
            type = NotificationType.UNLOCK_REQUEST,
            title = "Unlock Request",
            body = "Another request"
        )

        notificationRepository.addNotification(unread)
        notificationRepository.addNotification(read)
        notificationRepository.addNotification(otherUserNotification)

        assertEquals(3, notificationRepository.getAllNotifications().size)
        assertEquals(2, notificationRepository.getNotificationsForUser(user.userId).size)
        assertEquals(1, notificationRepository.getUnreadNotificationsForUser(user.userId).size)

        unread.readAt = System.currentTimeMillis()
        notificationRepository.updateNotification(unread)
        assertTrue(notificationRepository.getUnreadNotificationsForUser(user.userId).isEmpty())

        notificationRepository.clearAll()
        assertTrue(notificationRepository.getAllNotifications().isEmpty())
        logCase("Finished TC-216")
    }

    @Test
    fun TC_217_LimitManager_and_breach_detection_handle_boundary_conditions() {
        logCase("Starting TC-217")
        val app = RegulatedApp(
            packageName = "com.example.boundary",
            displayName = "Boundary App",
            isEnabled = true,
            sessionLimitMinutes = 10
        )
        val exactThresholdSession = Session(
            sessionId = "boundary-session",
            user = user,
            app = app,
            startTime = System.currentTimeMillis() - 10 * 60 * 1000L,
            durationSeconds = 10 * 60
        )
        val belowThresholdSession = exactThresholdSession.copy(sessionId = "boundary-session-2", durationSeconds = (10 * 60) - 1)

        assertTrue(limitManager.hasSessionLimitBeenReached(exactThresholdSession))
        assertFalse(limitManager.hasSessionLimitBeenReached(belowThresholdSession))

        val recentBreachedSession = exactThresholdSession.copy(sessionId = "recent-breach", durationSeconds = 10 * 60, breachedSessionLimit = true)
        val oldBreachedSession = exactThresholdSession.copy(
            sessionId = "old-breach",
            startTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) - 1_000L,
            durationSeconds = 10 * 60,
            breachedSessionLimit = true
        )
        user.escalationThreshold = 1

        assertTrue(breachDetectionService.hasSessionLimitBeenBreached(exactThresholdSession))
        assertTrue(breachDetectionService.countBreachesForUserToday(user, listOf(recentBreachedSession, oldBreachedSession)) == 1)
        assertTrue(breachDetectionService.shouldEscalate(user, listOf(recentBreachedSession, oldBreachedSession)))

        val dailyLimitUser = user.copy(userId = "user-daily", dailyLimitMinutes = 30)
        val withinWindowSessionOne = Session(
            sessionId = "daily-1",
            user = dailyLimitUser,
            app = app,
            startTime = System.currentTimeMillis() - 1_000L,
            durationSeconds = 15 * 60
        )
        val withinWindowSessionTwo = withinWindowSessionOne.copy(sessionId = "daily-2", durationSeconds = 15 * 60)
        val outsideWindowSession = withinWindowSessionOne.copy(
            sessionId = "daily-3",
            startTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) - 1_000L,
            durationSeconds = 120 * 60
        )

        assertTrue(limitManager.hasDailyLimitBeenReached(dailyLimitUser, listOf(withinWindowSessionOne, withinWindowSessionTwo)))
        assertFalse(limitManager.hasDailyLimitBeenReached(dailyLimitUser, listOf(withinWindowSessionOne, outsideWindowSession)))
        logCase("Finished TC-217")
    }
}



package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PhonePaused
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BanglaChatRepository
import com.example.data.WebRtcCallManager
import com.example.data.WebRtcConnectionState
import com.example.model.AppLanguage
import com.example.model.CallSession
import com.example.model.PublicProfile
import com.example.service.ActiveAppTracker
import com.example.service.CallRingbackPlayer
import com.example.service.CallRingtonePlayer
import com.example.service.NotificationHelper
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatLastSeenText
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

sealed interface ActiveCallRequest {
    data class Outgoing(val peer: PublicProfile, val callType: String) : ActiveCallRequest
    data class Incoming(val session: CallSession) : ActiveCallRequest
}

@Composable
fun IncomingCallBanner(
    session: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    // Play looping phone ringtone + vibration while the incoming banner is ringing
    DisposableEffect(session.id) {
        CallRingtonePlayer.startRinging(context, session.id)
        onDispose {
            CallRingtonePlayer.stopRinging(session.id)
        }
    }

    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("incoming_call_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = session.callerName,
                photoUrl = session.callerPhotoUrl,
                size = 48.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.callerName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (session.callType == "video") strings.incomingVideoCall else strings.incomingVoiceCall,
                    color = Color(0xFF93C5FD),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(
                onClick = onDecline,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(DangerRed)
                    .testTag("decline_incoming_call_button")
            ) {
                Icon(Icons.Rounded.CallEnd, contentDescription = strings.declineCall, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = onAccept,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .testTag("accept_incoming_call_button")
            ) {
                Icon(
                    imageVector = if (session.callType == "video") Icons.Rounded.Videocam else Icons.Rounded.Call,
                    contentDescription = strings.acceptCall,
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun LiveCallScreen(
    request: ActiveCallRequest,
    myProfile: PublicProfile,
    repository: BanglaChatRepository,
    language: AppLanguage = AppLanguage.BANGLA,
    isMinimized: Boolean = false,
    onToggleMinimize: (Boolean) -> Unit = {},
    onCallClosed: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isVideoCall = when (request) {
        is ActiveCallRequest.Outgoing -> request.callType == "video"
        is ActiveCallRequest.Incoming -> request.session.callType == "video"
    }
    val remoteUid = when (request) {
        is ActiveCallRequest.Outgoing -> request.peer.uid
        is ActiveCallRequest.Incoming -> request.session.callerUid
    }
    val remoteName = when (request) {
        is ActiveCallRequest.Outgoing -> request.peer.name.ifBlank { "Bangla Chat User" }
        is ActiveCallRequest.Incoming -> request.session.callerName.ifBlank { "Bangla Chat User" }
    }
    val remotePhoto = when (request) {
        is ActiveCallRequest.Outgoing -> request.peer.visiblePhotoUrl()
        is ActiveCallRequest.Incoming -> request.session.callerPhotoUrl
    }

    val requiredPerms = remember(isVideoCall) {
        if (isVideoCall) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        else arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPerms.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
        if (!permissionsGranted) {
            onCallClosed()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permLauncher.launch(requiredPerms)
        }
    }

    var activeSessionId by remember {
        mutableStateOf((request as? ActiveCallRequest.Incoming)?.session?.id)
    }
    var participants by remember {
        mutableStateOf(
            when (request) {
                is ActiveCallRequest.Outgoing -> listOf(myProfile.uid, request.peer.uid)
                is ActiveCallRequest.Incoming -> request.session.participants
            }
        )
    }
    var callDurationSec by remember { mutableIntStateOf(0) }
    var callConnected by remember { mutableStateOf(false) }
    var remoteDeclined by remember { mutableStateOf(false) }
    var remoteBusy by remember { mutableStateOf(false) }
    var callEnded by remember { mutableStateOf(false) }
    var timedOutNoAnswer by remember { mutableStateOf(false) }
    var logSaved by remember { mutableStateOf(false) }
    var remoteSessionStatus by remember {
        mutableStateOf(
            when (request) {
                is ActiveCallRequest.Outgoing -> "calling"
                is ActiveCallRequest.Incoming -> request.session.status.ifBlank { "ringing" }
            }
        )
    }

    val livePeerProfile by remember(remoteUid) {
        repository.observePublicProfile(remoteUid)
    }.collectAsStateWithLifecycle(initialValue = (request as? ActiveCallRequest.Outgoing)?.peer)

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val pendingLocalCandidates = remember { mutableListOf<Triple<String, Int, String>>() }
    val seenRemoteCandidateIds = remember { mutableSetOf<String>() }

    val rtcManager = remember {
        WebRtcCallManager(context) { sdpMid, sdpMLineIndex, sdp ->
            val sid = activeSessionId
            if (sid != null) {
                scope.launch {
                    repository.addIceCandidate(sid, participants, sdpMid, sdpMLineIndex, sdp)
                }
            } else {
                pendingLocalCandidates.add(Triple(sdpMid, sdpMLineIndex, sdp))
            }
        }
    }

    val localVideoTrack by rtcManager.localVideoTrack.collectAsStateWithLifecycle()
    val remoteVideoTrack by rtcManager.remoteVideoTrack.collectAsStateWithLifecycle()
    val connectionState by rtcManager.connectionState.collectAsStateWithLifecycle()
    val isMuted by rtcManager.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by rtcManager.isSpeakerOn.collectAsStateWithLifecycle()
    val isVideoEnabled by rtcManager.isVideoEnabled.collectAsStateWithLifecycle()

    fun finishAndClose(statusToWrite: String? = "ended") {
        if (callEnded) return
        callEnded = true
        CallRingbackPlayer.stopRingback()
        val sid = activeSessionId
        val duration = callDurationSec.toLong()
        scope.launch {
            if (sid != null && statusToWrite != null) {
                repository.updateCallSessionStatus(sid, statusToWrite)
            }
            if (!logSaved) {
                logSaved = true
                val direction = when (request) {
                    is ActiveCallRequest.Outgoing -> "outgoing"
                    is ActiveCallRequest.Incoming -> if (callConnected || duration > 0L) "incoming" else "missed"
                }
                repository.recordCallLog(
                    uid = myProfile.uid,
                    peerUid = remoteUid,
                    peerName = remoteName,
                    peerPhotoUrl = remotePhoto,
                    direction = direction,
                    callType = if (isVideoCall) "video" else "audio",
                    durationSeconds = duration
                )
                if (request is ActiveCallRequest.Outgoing) {
                    val callStatus = when {
                        callConnected || duration > 0L -> "answered"
                        remoteBusy -> "busy"
                        remoteDeclined -> "declined"
                        else -> "missed"
                    }
                    repository.logCallMessageInChat(
                        callerProfile = myProfile,
                        peerProfile = request.peer,
                        callType = if (isVideoCall) "video" else "audio",
                        callStatus = callStatus,
                        durationSeconds = duration
                    )
                }
            }
            onCallClosed()
        }
    }

    BackHandler(enabled = !isMinimized) {
        onToggleMinimize(true)
    }

    // Immediately silence incoming ringtone & clear incoming call notification once call screen opens
    LaunchedEffect(activeSessionId) {
        val sid = activeSessionId
        ActiveAppTracker.activeCallId = sid
        CallRingtonePlayer.stopRinging()
        if (!sid.isNullOrBlank()) {
            NotificationHelper.cancelIncomingCallNotification(context, sid)
        }
    }

    // WhatsApp-style caller ringback tone: plays ONLY once callee's online device transitions session to "ringing"
    LaunchedEffect(activeSessionId, remoteSessionStatus, callConnected, callEnded) {
        val sid = activeSessionId
        if (request is ActiveCallRequest.Outgoing &&
            remoteSessionStatus == "ringing" &&
            !callConnected &&
            !callEnded &&
            !sid.isNullOrBlank()
        ) {
            CallRingbackPlayer.startRingback(sid)
        } else {
            CallRingbackPlayer.stopRingback()
        }
    }

    // Auto-timeout unanswered outgoing calls after 45 seconds (WhatsApp-style missed call)
    LaunchedEffect(request, callConnected, callEnded) {
        if (request is ActiveCallRequest.Outgoing && !callConnected && !callEnded) {
            delay(45_000L)
            if (!callConnected && !callEnded) {
                timedOutNoAnswer = true
                CallRingbackPlayer.stopRingback()
                delay(1200L)
                finishAndClose("missed")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ActiveAppTracker.activeCallId = null
            CallRingtonePlayer.stopRinging()
            CallRingbackPlayer.stopRingback()
            rtcManager.release()
        }
    }

    // Start WebRTC media & signaling once permissions are granted
    LaunchedEffect(permissionsGranted) {
        if (!permissionsGranted) return@LaunchedEffect
        rtcManager.startLocalMediaAndPeerConnection(isVideoCall = isVideoCall)
        when (request) {
            is ActiveCallRequest.Outgoing -> {
                rtcManager.createOffer { offerSdp ->
                    scope.launch {
                        repository.createCallSession(
                            caller = myProfile,
                            callee = request.peer,
                            callType = request.callType,
                            offerSdp = offerSdp
                        ).onSuccess { created ->
                            activeSessionId = created.id
                            participants = created.participants
                            val queued = pendingLocalCandidates.toList()
                            pendingLocalCandidates.clear()
                            queued.forEach { (mid, idx, sdp) ->
                                repository.addIceCandidate(created.id, created.participants, mid, idx, sdp)
                            }
                        }.onFailure {
                            finishAndClose(null)
                        }
                    }
                }
            }
            is ActiveCallRequest.Incoming -> {
                val offer = request.session.offerSdp
                if (offer.isNotBlank()) {
                    rtcManager.handleRemoteOfferAndCreateAnswer(offer) { answerSdp ->
                        scope.launch {
                            repository.acceptCallSession(request.session.id, answerSdp)
                            callConnected = true
                        }
                    }
                }
            }
        }
    }

    // Observe remote call session updates (calling -> ringing -> accepted / declined / end)
    LaunchedEffect(activeSessionId) {
        val sid = activeSessionId ?: return@LaunchedEffect
        repository.observeCallSession(sid).collect { session ->
            if (session == null) return@collect
            remoteSessionStatus = session.status
            if (session.status == "accepted") {
                callConnected = true
                CallRingbackPlayer.stopRingback()
                if (request is ActiveCallRequest.Outgoing && session.answerSdp.isNotBlank()) {
                    rtcManager.setRemoteAnswer(session.answerSdp)
                }
            } else if (session.status == "busy") {
                CallRingbackPlayer.stopRingback()
                remoteBusy = true
                delay(2400L)
                finishAndClose(null)
            } else if (session.status in setOf("declined", "ended", "missed")) {
                CallRingbackPlayer.stopRingback()
                if (session.status == "declined") {
                    remoteDeclined = true
                }
                finishAndClose(null)
            }
        }
    }

    // Observe remote ICE candidates
    LaunchedEffect(activeSessionId) {
        val sid = activeSessionId ?: return@LaunchedEffect
        repository.observeIceCandidates(sid, myProfile.uid).collect { list ->
            list.forEach { item ->
                if (item.senderUid != myProfile.uid && seenRemoteCandidateIds.add(item.id)) {
                    rtcManager.addRemoteIceCandidate(item.sdpMid, item.sdpMLineIndex, item.sdp)
                }
            }
        }
    }

    // Active duration timer
    LaunchedEffect(callConnected, connectionState) {
        if (callConnected || connectionState == WebRtcConnectionState.CONNECTED) {
            while (!callEnded) {
                delay(1000L)
                callDurationSec += 1
            }
        }
    }

    val isOutgoingRinging = request is ActiveCallRequest.Outgoing &&
        remoteSessionStatus == "ringing" &&
        !callConnected &&
        !callEnded &&
        !timedOutNoAnswer

    val isPeerEffectivelyOnline = livePeerProfile?.isEffectivelyOnline(nowMillis) == true
    val showOfflineNotice = request is ActiveCallRequest.Outgoing &&
        !callConnected &&
        !callEnded &&
        !timedOutNoAnswer &&
        remoteSessionStatus == "calling" &&
        livePeerProfile != null &&
        !isPeerEffectivelyOnline

    val statusSubtitle = when {
        remoteBusy -> strings.callBusyStatus
        timedOutNoAnswer -> strings.callNoAnswerTimeout
        callEnded -> strings.callEndedStatus
        callConnected || connectionState == WebRtcConnectionState.CONNECTED -> {
            val mins = callDurationSec / 60
            val secs = callDurationSec % 60
            String.format("%02d:%02d", mins, secs)
        }
        request is ActiveCallRequest.Incoming -> strings.connectingStatus
        remoteSessionStatus == "ringing" -> strings.ringingStatus
        else -> strings.callingStatus
    }

    val statusColor = when {
        remoteBusy -> Color(0xFFFBBF24)
        timedOutNoAnswer -> Color(0xFFFCA5A5)
        callConnected || connectionState == WebRtcConnectionState.CONNECTED -> OnlineGreen
        isOutgoingRinging -> Color(0xFF4ADE80)
        else -> Color(0xFF93C5FD)
    }

    // When minimized, render ONLY the sleek floating top pill while keeping WebRTC & signaling alive!
    if (isMinimized) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.96f),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onToggleMinimize(false) }
                    .testTag("minimized_floating_call_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(OnlineGreen)
                    )
                    Spacer(Modifier.width(10.dp))
                    UserAvatar(name = remoteName, photoUrl = remotePhoto, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = remoteName,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "$statusSubtitle • ${strings.returnToCallHint}",
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { rtcManager.toggleMute() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.16f))
                            .testTag("minimized_call_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = strings.muteMic,
                            tint = if (isMuted) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { finishAndClose("ended") },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                            .testTag("minimized_call_end_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = strings.endCall,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { onToggleMinimize(false) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("minimized_call_expand_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = strings.returnToCallHint,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
        return
    }

    val pulseTransition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B1120), Color(0xFF1E1B4B), Color(0xFF090D16))
                )
            )
            .testTag("live_call_screen")
    ) {
        // Remote full-screen video (when video call and remote track ready)
        if (isVideoCall && remoteVideoTrack != null) {
            WebRtcVideoSurface(
                videoTrack = remoteVideoTrack,
                eglBase = rtcManager.eglBase,
                mirror = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top-left minimize button so user can browse chats while call stays live
        IconButton(
            onClick = { onToggleMinimize(true) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(14.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .testTag("minimize_call_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = strings.minimizeCall,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // Caller / Peer avatar & status overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 44.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isVideoCall || remoteVideoTrack == null) {
                Box(contentAlignment = Alignment.Center) {
                    if (isOutgoingRinging) {
                        Box(
                            modifier = Modifier
                                .size(134.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(OnlineGreen.copy(alpha = 0.22f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = if (isOutgoingRinging) {
                                    Brush.linearGradient(listOf(OnlineGreen, BrandBlue))
                                } else {
                                    Brush.linearGradient(listOf(BrandBlue, BrandPurple))
                                },
                                shape = CircleShape
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatar(name = remoteName, photoUrl = remotePhoto, size = 112.dp)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
            Text(
                text = remoteName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusSubtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isOutgoingRinging) FontWeight.SemiBold else FontWeight.Normal,
                color = statusColor,
                modifier = Modifier.testTag("call_status_subtitle")
            )

            AnimatedVisibility(visible = remoteBusy) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .testTag("call_peer_busy_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhonePaused,
                            contentDescription = null,
                            tint = Color(0xFFFCD34D),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = strings.callBusySubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFEF3C7),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showOfflineNotice && !remoteBusy) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("call_peer_offline_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WifiOff,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.callPeerOfflineNotice,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFFEF3C7),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    val lastSeen = livePeerProfile?.takeIf { it.showLastSeen }?.lastSeen
                    if (lastSeen != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatLastSeenText(lastSeen, language, nowMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }
                }
            }
        }

        // Local PiP video preview in bottom-right above controls
        if (isVideoCall && isVideoEnabled && localVideoTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 140.dp)
                    .size(width = 108.dp, height = 152.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            ) {
                WebRtcVideoSurface(
                    videoTrack = localVideoTrack,
                    eglBase = rtcManager.eglBase,
                    mirror = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Bottom Call Controls Bar
        Surface(
            color = Color.Black.copy(alpha = 0.52f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallControlCircle(
                    icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    label = strings.muteMic,
                    active = isMuted,
                    onClick = { rtcManager.toggleMute() }
                )
                CallControlCircle(
                    icon = if (isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                    label = strings.speakerAudio,
                    active = isSpeakerOn,
                    onClick = { rtcManager.toggleSpeaker() }
                )
                if (isVideoCall) {
                    CallControlCircle(
                        icon = if (isVideoEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                        label = strings.toggleCamera,
                        active = !isVideoEnabled,
                        onClick = { rtcManager.toggleVideo() }
                    )
                    CallControlCircle(
                        icon = Icons.Rounded.Cameraswitch,
                        label = strings.flipCamera,
                        active = false,
                        onClick = { rtcManager.switchCamera() }
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { finishAndClose("ended") },
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = strings.endCall,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.endCall,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CallControlCircle(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (active) Color.White else Color.White.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFF0F172A) else Color.White
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun WebRtcVideoSurface(
    videoTrack: VideoTrack?,
    eglBase: EglBase,
    mirror: Boolean,
    modifier: Modifier = Modifier
) {
    var currentRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(videoTrack, currentRenderer) {
        val renderer = currentRenderer
        if (renderer != null && videoTrack != null) {
            runCatching { videoTrack.addSink(renderer) }
        }
        onDispose {
            if (renderer != null && videoTrack != null) {
                runCatching { videoTrack.removeSink(renderer) }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglBase.eglBaseContext, null)
                setMirror(mirror)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                currentRenderer = this
            }
        },
        onRelease = { renderer ->
            runCatching { videoTrack?.removeSink(renderer) }
            runCatching { renderer.release() }
            if (currentRenderer === renderer) currentRenderer = null
        },
        modifier = modifier
    )
}

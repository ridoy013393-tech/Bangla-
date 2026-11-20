package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BanglaChatRepository
import com.example.data.WebRtcCallManager
import com.example.data.WebRtcConnectionState
import com.example.model.CallSession
import com.example.model.PublicProfile
import com.example.ui.components.UserAvatar
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
    var callEnded by remember { mutableStateOf(false) }
    var logSaved by remember { mutableStateOf(false) }

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
            }
            onCallClosed()
        }
    }

    BackHandler { finishAndClose("ended") }

    DisposableEffect(Unit) {
        onDispose { rtcManager.release() }
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

    // Observe remote call session updates (answer SDP, decline, end)
    LaunchedEffect(activeSessionId) {
        val sid = activeSessionId ?: return@LaunchedEffect
        repository.observeCallSession(sid).collect { session ->
            if (session == null) return@collect
            if (session.status == "accepted") {
                callConnected = true
                if (request is ActiveCallRequest.Outgoing && session.answerSdp.isNotBlank()) {
                    rtcManager.setRemoteAnswer(session.answerSdp)
                }
            } else if (session.status in setOf("declined", "ended", "missed")) {
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

    val statusSubtitle = when {
        callEnded -> strings.callEndedStatus
        callConnected || connectionState == WebRtcConnectionState.CONNECTED -> {
            val mins = callDurationSec / 60
            val secs = callDurationSec % 60
            String.format("%02d:%02d", mins, secs)
        }
        connectionState == WebRtcConnectionState.CONNECTING && request is ActiveCallRequest.Incoming ->
            strings.connectingStatus
        else -> strings.callingStatus
    }

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

        // Caller / Peer avatar & status overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 44.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isVideoCall || remoteVideoTrack == null) {
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .border(3.dp, Brush.linearGradient(listOf(BrandBlue, BrandPurple)), CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(name = remoteName, photoUrl = remotePhoto, size = 112.dp)
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
                color = Color(0xFF93C5FD)
            )
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

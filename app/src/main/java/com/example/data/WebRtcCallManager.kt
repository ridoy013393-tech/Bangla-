package com.example.data

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

enum class WebRtcConnectionState {
    IDLE, CONNECTING, CONNECTED, DISCONNECTED, FAILED
}

class WebRtcCallManager(
    context: Context,
    private val onLocalIceCandidate: (sdpMid: String, sdpMLineIndex: Int, sdp: String) -> Unit
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    val eglBase: EglBase = EglBase.create()

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _connectionState = MutableStateFlow(WebRtcConnectionState.IDLE)
    val connectionState: StateFlow<WebRtcConnectionState> = _connectionState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private var remoteDescriptionSet = false
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()

    private fun ensureFactory(): PeerConnectionFactory {
        factory?.let { return it }
        synchronized(FactoryInitLock) {
            if (!isFactoryInitialized) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                isFactoryInitialized = true
            }
        }
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
            .also { factory = it }
    }

    fun startLocalMediaAndPeerConnection(isVideoCall: Boolean) {
        try {
            val pcFactory = ensureFactory()
            configureAudioMode(speakerOn = isVideoCall)
            _isVideoEnabled.value = isVideoCall
            _connectionState.value = WebRtcConnectionState.CONNECTING

            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
            )
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            }

            peerConnection = pcFactory.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate?) {
                        if (candidate != null) {
                            onLocalIceCandidate(
                                candidate.sdpMid ?: "0",
                                candidate.sdpMLineIndex,
                                candidate.sdp
                            )
                        }
                    }

                    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                        val track = receiver?.track()
                        if (track is VideoTrack) {
                            _remoteVideoTrack.value = track
                        }
                    }

                    override fun onAddStream(stream: MediaStream?) {
                        stream?.videoTracks?.firstOrNull()?.let { track ->
                            _remoteVideoTrack.value = track
                        }
                    }

                    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                        when (newState) {
                            PeerConnection.IceConnectionState.CONNECTED,
                            PeerConnection.IceConnectionState.COMPLETED ->
                                _connectionState.value = WebRtcConnectionState.CONNECTED
                            PeerConnection.IceConnectionState.DISCONNECTED ->
                                _connectionState.value = WebRtcConnectionState.DISCONNECTED
                            PeerConnection.IceConnectionState.FAILED ->
                                _connectionState.value = WebRtcConnectionState.FAILED
                            else -> Unit
                        }
                    }

                    override fun onSignalingChange(p0: PeerConnection.SignalingState?) = Unit
                    override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
                    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) = Unit
                    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) = Unit
                    override fun onRemoveStream(p0: MediaStream?) = Unit
                    override fun onDataChannel(p0: DataChannel?) = Unit
                    override fun onRenegotiationNeeded() = Unit
                }
            )

            // Audio track
            val aSource = pcFactory.createAudioSource(MediaConstraints())
            audioSource = aSource
            val aTrack = pcFactory.createAudioTrack("banglachat_audio", aSource)
            aTrack.setEnabled(true)
            localAudioTrack = aTrack
            peerConnection?.addTrack(aTrack, listOf("banglachat_stream"))

            // Video track (when video call)
            if (isVideoCall) {
                val capturer = createCameraCapturer()
                if (capturer != null) {
                    videoCapturer = capturer
                    val helper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
                    surfaceTextureHelper = helper
                    val vSource = pcFactory.createVideoSource(capturer.isScreencast)
                    videoSource = vSource
                    capturer.initialize(helper, appContext, vSource.capturerObserver)
                    runCatching { capturer.startCapture(720, 1280, 30) }
                        .onFailure { Log.w(TAG, "Camera capture start warning: ${it.message}") }
                    val vTrack = pcFactory.createVideoTrack("banglachat_video", vSource)
                    vTrack.setEnabled(true)
                    _localVideoTrack.value = vTrack
                    peerConnection?.addTrack(vTrack, listOf("banglachat_stream"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebRTC media/peer connection", e)
            _connectionState.value = WebRtcConnectionState.FAILED
        }
    }

    fun createOffer(onOfferCreated: (String) -> Unit) {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", _isVideoEnabled.value.toString()))
        }
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    pc.setLocalDescription(SimpleSdpObserver(), desc)
                    onOfferCreated(desc.description)
                }
            }
        }, constraints)
    }

    fun handleRemoteOfferAndCreateAnswer(offerSdp: String, onAnswerCreated: (String) -> Unit) {
        val pc = peerConnection ?: return
        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                flushPendingRemoteCandidates()
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", _isVideoEnabled.value.toString()))
                }
                pc.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        if (desc != null) {
                            pc.setLocalDescription(SimpleSdpObserver(), desc)
                            onAnswerCreated(desc.description)
                        }
                    }
                }, constraints)
            }
        }, remoteDesc)
    }

    fun setRemoteAnswer(answerSdp: String) {
        val pc = peerConnection ?: return
        if (remoteDescriptionSet) return
        val remoteDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        pc.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                flushPendingRemoteCandidates()
            }
        }, remoteDesc)
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        if (remoteDescriptionSet) {
            peerConnection?.addIceCandidate(candidate)
        } else {
            pendingRemoteCandidates.add(candidate)
        }
    }

    private fun flushPendingRemoteCandidates() {
        val pc = peerConnection ?: return
        pendingRemoteCandidates.forEach { pc.addIceCandidate(it) }
        pendingRemoteCandidates.clear()
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        localAudioTrack?.setEnabled(!next)
    }

    fun toggleSpeaker() {
        configureAudioMode(speakerOn = !_isSpeakerOn.value)
    }

    fun toggleVideo() {
        val next = !_isVideoEnabled.value
        _isVideoEnabled.value = next
        _localVideoTrack.value?.setEnabled(next)
    }

    fun switchCamera() {
        runCatching { videoCapturer?.switchCamera(null) }
    }

    private fun configureAudioMode(speakerOn: Boolean) {
        _isSpeakerOn.value = speakerOn
        runCatching {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = speakerOn
        }
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(appContext)
        val devices = enumerator.deviceNames
        val front = devices.firstOrNull { enumerator.isFrontFacing(it) }
        val fallback = devices.firstOrNull()
        val chosen = front ?: fallback ?: return null
        return enumerator.createCapturer(chosen, null)
    }

    fun release() {
        remoteDescriptionSet = false
        pendingRemoteCandidates.clear()
        runCatching { videoCapturer?.stopCapture() }
        runCatching { videoCapturer?.dispose() }
        videoCapturer = null
        runCatching { surfaceTextureHelper?.dispose() }
        surfaceTextureHelper = null
        runCatching { _localVideoTrack.value?.dispose() }
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        runCatching { videoSource?.dispose() }
        videoSource = null
        runCatching { localAudioTrack?.dispose() }
        localAudioTrack = null
        runCatching { audioSource?.dispose() }
        audioSource = null
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        peerConnection = null
        runCatching { factory?.dispose() }
        factory = null
        runCatching { eglBase.release() }
        runCatching {
            audioManager?.mode = AudioManager.MODE_NORMAL
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
        }
        _connectionState.value = WebRtcConnectionState.IDLE
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) {
            Log.e(TAG, "SDP create failure: $error")
        }
        override fun onSetFailure(error: String?) {
            Log.e(TAG, "SDP set failure: $error")
        }
    }

    private companion object {
        const val TAG = "WebRtcCallManager"
        val FactoryInitLock = Any()
        @Volatile
        var isFactoryInitialized = false
    }
}

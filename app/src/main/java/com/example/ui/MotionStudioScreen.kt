package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.MediaType
import com.example.ui.components.PreviewStage
import com.example.ui.components.ProjectExportCard
import com.example.ui.components.StudioHeader
import com.example.ui.components.TextLayerPanel
import com.example.ui.components.TimelineTrack
import com.example.ui.components.ToolsDeck
import com.example.ui.state.MotionStudioViewModel
import kotlinx.coroutines.delay

@Composable
fun MotionStudioScreen(
    viewModel: MotionStudioViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var videoPositionMs by remember { mutableLongStateOf(0L) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }

    // Manage ExoPlayer lifecycle & listeners
    DisposableEffect(exoPlayer, lifecycleOwner) {
        val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = exoPlayer.duration
                    if (dur > 0L) videoDurationMs = dur
                } else if (playbackState == Player.STATE_ENDED) {
                    viewModel.setPlaying(false)
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            }
        }

        exoPlayer.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        }
    }

    // Bind active video clip to ExoPlayer
    val activeClip = uiState.activeClip
    LaunchedEffect(activeClip?.id) {
        if (activeClip != null && activeClip.mediaType == MediaType.VIDEO) {
            videoAspectRatio = 16f / 9f
            videoPositionMs = 0L
            videoDurationMs = activeClip.durationMs
            exoPlayer.setMediaItem(MediaItem.fromUri(activeClip.uri))
            exoPlayer.prepare()
            exoPlayer.seekTo(0L)
            exoPlayer.playWhenReady = false
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            videoPositionMs = 0L
            videoDurationMs = 0L
        }
    }

    // Sync mute & loop controls
    LaunchedEffect(uiState.isMuted) {
        exoPlayer.volume = if (uiState.isMuted) 0f else 1f
    }
    LaunchedEffect(uiState.isLooping) {
        exoPlayer.repeatMode = if (uiState.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Update position ticker while playing
    LaunchedEffect(uiState.isPlaying, activeClip?.id) {
        while (uiState.isPlaying && activeClip?.mediaType == MediaType.VIDEO) {
            videoPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (dur > 0L) videoDurationMs = dur
            delay(150L)
        }
    }

    // Show status snackbars
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusMessage()
    }

    // Zero-permission Photo Picker for images & videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMedia(context, uris)
        }
    }

    // Save project notes (.txt) via Storage Access Framework
    val saveNotesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        uri?.let { viewModel.writeNotesToUri(context, it) }
    }

    // Export edited frame snapshot (.png) via Storage Access Framework
    val exportFrameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png"),
    ) { uri: Uri? ->
        uri?.let { viewModel.exportSnapshotToUri(context, it, videoPositionMs) }
    }

    val launchMediaPicker = {
        mediaPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
        )
    }

    val togglePlayPause = {
        if (activeClip?.mediaType == MediaType.VIDEO) {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                    exoPlayer.seekTo(0L)
                }
                exoPlayer.play()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        topBar = {
            StudioHeader(
                clipCount = uiState.clips.size,
                textLayerCount = uiState.textLayers.size,
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            val isExpanded = maxWidth >= 720.dp

            if (isExpanded) {
                // Two-pane tablet / foldable studio workspace
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PreviewStage(
                            activeClip = activeClip,
                            filters = uiState.filters,
                            textLayers = uiState.textLayers,
                            selectedTextLayerId = uiState.selectedTextLayerId,
                            exoPlayer = exoPlayer,
                            videoAspectRatio = videoAspectRatio,
                            isPlaying = uiState.isPlaying,
                            isMuted = uiState.isMuted,
                            isLooping = uiState.isLooping,
                            videoPositionMs = videoPositionMs,
                            videoDurationMs = videoDurationMs,
                            hasAnyContent = uiState.clips.isNotEmpty() || uiState.textLayers.isNotEmpty(),
                            onImportClick = launchMediaPicker,
                            onPlayPauseToggle = togglePlayPause,
                            onSeekTo = { pos ->
                                exoPlayer.seekTo(pos)
                                videoPositionMs = pos
                            },
                            onToggleMute = viewModel::toggleMute,
                            onToggleLoop = viewModel::toggleLoop,
                            onClearClick = viewModel::clearProject,
                            onSelectTextLayer = viewModel::selectTextLayer,
                            onDragTextLayer = viewModel::moveTextLayer,
                        )

                        TimelineTrack(
                            clips = uiState.clips,
                            activeClipId = activeClip?.id,
                            textLayers = uiState.textLayers,
                            selectedTextLayerId = uiState.selectedTextLayerId,
                            onSelectClip = viewModel::selectClip,
                            onMoveClip = viewModel::moveClip,
                            onRemoveClip = viewModel::removeClip,
                            onSelectTextLayer = viewModel::selectTextLayer,
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ToolsDeck(
                            activeTab = uiState.activeToolTab,
                            filters = uiState.filters,
                            onSelectTab = viewModel::selectToolTab,
                            onApplyPreset = viewModel::applyPreset,
                            onContrastChange = viewModel::updateContrast,
                            onSaturationChange = viewModel::updateSaturation,
                            onBlurChange = viewModel::updateBlur,
                            onBrightnessChange = viewModel::updateBrightness,
                            onResetFilters = viewModel::resetFilters,
                            onQuickAddText = viewModel::addOrCommitTextLayer,
                        )

                        TextLayerPanel(
                            draftText = uiState.draftText,
                            draftFontSizeSp = uiState.draftFontSizeSp,
                            draftColor = uiState.draftTextColor,
                            textLayers = uiState.textLayers,
                            selectedLayerId = uiState.selectedTextLayerId,
                            onDraftTextChange = viewModel::updateDraftText,
                            onFontSizeChange = viewModel::updateDraftFontSize,
                            onColorChange = viewModel::updateDraftTextColor,
                            onCommitLayer = viewModel::addOrCommitTextLayer,
                            onNewLayer = viewModel::startNewTextLayer,
                            onCenterSelected = viewModel::centerSelectedTextLayer,
                            onSelectLayer = viewModel::selectTextLayer,
                            onToggleVisibility = viewModel::toggleTextLayerVisibility,
                            onRemoveLayer = viewModel::removeTextLayer,
                        )

                        ProjectExportCard(
                            hasActiveClip = activeClip != null,
                            isExporting = uiState.isExporting,
                            onSaveNotes = { saveNotesLauncher.launch("motion-studio-project.txt") },
                            onShareNotes = { viewModel.shareProjectNotes(context) },
                            onExportFrame = {
                                exportFrameLauncher.launch("motion-studio-frame.png")
                            },
                            onShareFrame = { viewModel.shareSnapshot(context, videoPositionMs) },
                        )
                    }
                }
            } else {
                // Mobile handheld vertical studio layout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                    ) {
                        PreviewStage(
                            activeClip = activeClip,
                            filters = uiState.filters,
                            textLayers = uiState.textLayers,
                            selectedTextLayerId = uiState.selectedTextLayerId,
                            exoPlayer = exoPlayer,
                            videoAspectRatio = videoAspectRatio,
                            isPlaying = uiState.isPlaying,
                            isMuted = uiState.isMuted,
                            isLooping = uiState.isLooping,
                            videoPositionMs = videoPositionMs,
                            videoDurationMs = videoDurationMs,
                            hasAnyContent = uiState.clips.isNotEmpty() || uiState.textLayers.isNotEmpty(),
                            onImportClick = launchMediaPicker,
                            onPlayPauseToggle = togglePlayPause,
                            onSeekTo = { pos ->
                                exoPlayer.seekTo(pos)
                                videoPositionMs = pos
                            },
                            onToggleMute = viewModel::toggleMute,
                            onToggleLoop = viewModel::toggleLoop,
                            onClearClick = viewModel::clearProject,
                            onSelectTextLayer = viewModel::selectTextLayer,
                            onDragTextLayer = viewModel::moveTextLayer,
                        )

                        ToolsDeck(
                            activeTab = uiState.activeToolTab,
                            filters = uiState.filters,
                            onSelectTab = viewModel::selectToolTab,
                            onApplyPreset = viewModel::applyPreset,
                            onContrastChange = viewModel::updateContrast,
                            onSaturationChange = viewModel::updateSaturation,
                            onBlurChange = viewModel::updateBlur,
                            onBrightnessChange = viewModel::updateBrightness,
                            onResetFilters = viewModel::resetFilters,
                            onQuickAddText = viewModel::addOrCommitTextLayer,
                        )

                        TextLayerPanel(
                            draftText = uiState.draftText,
                            draftFontSizeSp = uiState.draftFontSizeSp,
                            draftColor = uiState.draftTextColor,
                            textLayers = uiState.textLayers,
                            selectedLayerId = uiState.selectedTextLayerId,
                            onDraftTextChange = viewModel::updateDraftText,
                            onFontSizeChange = viewModel::updateDraftFontSize,
                            onColorChange = viewModel::updateDraftTextColor,
                            onCommitLayer = viewModel::addOrCommitTextLayer,
                            onNewLayer = viewModel::startNewTextLayer,
                            onCenterSelected = viewModel::centerSelectedTextLayer,
                            onSelectLayer = viewModel::selectTextLayer,
                            onToggleVisibility = viewModel::toggleTextLayerVisibility,
                            onRemoveLayer = viewModel::removeTextLayer,
                        )

                        TimelineTrack(
                            clips = uiState.clips,
                            activeClipId = activeClip?.id,
                            textLayers = uiState.textLayers,
                            selectedTextLayerId = uiState.selectedTextLayerId,
                            onSelectClip = viewModel::selectClip,
                            onMoveClip = viewModel::moveClip,
                            onRemoveClip = viewModel::removeClip,
                            onSelectTextLayer = viewModel::selectTextLayer,
                        )

                        ProjectExportCard(
                            hasActiveClip = activeClip != null,
                            isExporting = uiState.isExporting,
                            onSaveNotes = { saveNotesLauncher.launch("motion-studio-project.txt") },
                            onShareNotes = { viewModel.shareProjectNotes(context) },
                            onExportFrame = {
                                exportFrameLauncher.launch("motion-studio-frame.png")
                            },
                            onShareFrame = { viewModel.shareSnapshot(context, videoPositionMs) },
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

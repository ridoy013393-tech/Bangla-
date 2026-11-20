package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AiRewriteTone
import com.example.model.AiTaskState
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummarySheet(
    summaryState: AiTaskState,
    onRetry: () -> Unit,
    onCopied: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = BrandPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.aiSummaryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.aiPoweredByDeepSeek,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (summaryState) {
                is AiTaskState.Idle,
                is AiTaskState.Loading -> {
                    AiLoadingBlock(label = strings.aiGenerating)
                }

                is AiTaskState.Streaming -> {
                    AiResultTextCard(text = summaryState.text)
                }

                is AiTaskState.Success -> {
                    AiResultTextCard(text = summaryState.text)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(summaryState.text))
                            onCopied()
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("ai_summary_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.copy)
                    }
                }

                is AiTaskState.Error -> {
                    Text(
                        text = summaryState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(strings.retry)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiExplainSheet(
    sourceMessage: String,
    explainState: AiTaskState,
    onRetry: () -> Unit,
    onCopied: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.aiExplainTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.aiPoweredByDeepSeek,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sourceMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = strings.aiExplainOriginalLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sourceMessage,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (explainState) {
                is AiTaskState.Idle,
                is AiTaskState.Loading -> {
                    AiLoadingBlock(label = strings.aiGenerating)
                }

                is AiTaskState.Streaming -> {
                    AiResultTextCard(text = explainState.text)
                }

                is AiTaskState.Success -> {
                    AiResultTextCard(text = explainState.text)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(explainState.text))
                            onCopied()
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("ai_explain_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.copy)
                    }
                }

                is AiTaskState.Error -> {
                    Text(
                        text = explainState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(strings.retry)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWriterSheet(
    initialDraft: String,
    writerState: AiTaskState,
    onRewriteDraft: (String, AiRewriteTone) -> Unit,
    onGenerateFromIdea: (String) -> Unit,
    onInsertResult: (String) -> Unit,
    onSendResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var isRewriteMode by remember { mutableStateOf(initialDraft.isNotBlank()) }
    var draftInput by remember { mutableStateOf(initialDraft) }
    var ideaInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf(AiRewriteTone.POLITE) }

    val toneOptions = listOf(
        AiRewriteTone.POLITE to strings.aiTonePolite,
        AiRewriteTone.CASUAL to strings.aiToneCasual,
        AiRewriteTone.PROFESSIONAL to strings.aiToneProfessional,
        AiRewriteTone.BANGLA_POLISH to strings.aiToneBanglaPolish,
        AiRewriteTone.ENGLISH to strings.aiToneEnglish
    )

    val ideaPresets = listOf(
        strings.aiIdeaPreset1,
        strings.aiIdeaPreset2,
        strings.aiIdeaPreset3,
        strings.aiIdeaPreset4
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = null,
                    tint = BrandPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.aiWriterTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.aiPoweredByDeepSeek,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode selector chips
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = isRewriteMode,
                    onClick = { isRewriteMode = true },
                    label = { Text(strings.aiWriterTabRewrite) },
                    modifier = Modifier.testTag("ai_writer_tab_rewrite")
                )
                FilterChip(
                    selected = !isRewriteMode,
                    onClick = { isRewriteMode = false },
                    label = { Text(strings.aiWriterTabCompose) },
                    modifier = Modifier.testTag("ai_writer_tab_compose")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isRewriteMode) {
                OutlinedTextField(
                    value = draftInput,
                    onValueChange = { draftInput = it.take(1000) },
                    placeholder = { Text(strings.aiWriterRewriteHint) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_writer_draft_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(toneOptions, key = { it.first.name }) { (tone, label) ->
                        FilterChip(
                            selected = selectedTone == tone,
                            onClick = {
                                selectedTone = tone
                                if (draftInput.trim().isNotEmpty()) {
                                    onRewriteDraft(draftInput.trim(), tone)
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("ai_tone_${tone.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onRewriteDraft(draftInput.trim(), selectedTone) },
                    enabled = draftInput.trim().isNotEmpty() && writerState !is AiTaskState.Loading,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_writer_generate_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.aiWriterGenerateAction)
                }
            } else {
                OutlinedTextField(
                    value = ideaInput,
                    onValueChange = { ideaInput = it.take(500) },
                    placeholder = { Text(strings.aiWriterIdeaHint) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_writer_idea_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(ideaPresets, key = { it }) { preset ->
                        AssistChip(
                            onClick = {
                                ideaInput = preset
                                onGenerateFromIdea(preset)
                            },
                            label = { Text(preset, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onGenerateFromIdea(ideaInput.trim()) },
                    enabled = ideaInput.trim().isNotEmpty() && writerState !is AiTaskState.Loading,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_writer_generate_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.aiWriterGenerateAction)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (writerState) {
                is AiTaskState.Idle -> Unit

                is AiTaskState.Loading -> {
                    AiLoadingBlock(label = strings.aiGenerating)
                }

                is AiTaskState.Streaming -> {
                    AiResultTextCard(text = writerState.text)
                }

                is AiTaskState.Success -> {
                    AiResultTextCard(text = writerState.text)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onInsertResult(writerState.text) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_writer_insert_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.aiWriterInsertAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Button(
                            onClick = { onSendResult(writerState.text) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_writer_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.aiWriterSendAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                is AiTaskState.Error -> {
                    Text(
                        text = writerState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AiLoadingBlock(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = BrandPurple,
            modifier = Modifier.size(22.dp),
            strokeWidth = 2.5.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AiResultTextCard(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

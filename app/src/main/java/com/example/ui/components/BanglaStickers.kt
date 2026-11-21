package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings

data class BanglaSticker(
    val id: String,
    val emoji: String,
    val banglaText: String,
    val subText: String,
    val startColor: Color,
    val endColor: Color
)

object BanglaStickerCatalog {
    val all: List<BanglaSticker> = listOf(
        BanglaSticker(
            id = "salam",
            emoji = "🤲",
            banglaText = "আসসালামু আলাইকুম",
            subText = "Peace be upon you",
            startColor = Color(0xFF059669),
            endColor = Color(0xFF0D9488)
        ),
        BanglaSticker(
            id = "bhai_bhai",
            emoji = "😎",
            banglaText = "আরে ভাই ভাই!",
            subText = "Next level vibes",
            startColor = Color(0xFF2563EB),
            endColor = Color(0xFF7C3AED)
        ),
        BanglaSticker(
            id = "cha_khaben",
            emoji = "☕",
            banglaText = "চা খাবেন?",
            subText = "Adda + Cha time",
            startColor = Color(0xFFD97706),
            endColor = Color(0xFFEA580C)
        ),
        BanglaSticker(
            id = "para_nai",
            emoji = "😌",
            banglaText = "প্যারা নাই, চিল!",
            subText = "No stress at all",
            startColor = Color(0xFF0EA5E9),
            endColor = Color(0xFF6366F1)
        ),
        BanglaSticker(
            id = "khela_hobe",
            emoji = "🔥",
            banglaText = "খেলা হবে!",
            subText = "Game on!",
            startColor = Color(0xFFDC2626),
            endColor = Color(0xFFF97316)
        ),
        BanglaSticker(
            id = "haste_shesh",
            emoji = "😂",
            banglaText = "হাসতে হাসতে শেষ",
            subText = "LOL!",
            startColor = Color(0xFFEAB308),
            endColor = Color(0xFFF97316)
        ),
        BanglaSticker(
            id = "ki_khobor",
            emoji = "👋",
            banglaText = "কী খবর বন্ধু?",
            subText = "What's up?",
            startColor = Color(0xFF3B82F6),
            endColor = Color(0xFF06B6D4)
        ),
        BanglaSticker(
            id = "dhonnobad",
            emoji = "❤️",
            banglaText = "অনেক ধন্যবাদ",
            subText = "Thank you so much",
            startColor = Color(0xFFE11D48),
            endColor = Color(0xFFEC4899)
        ),
        BanglaSticker(
            id = "ok_done",
            emoji = "👍",
            banglaText = "ওকে ডান!",
            subText = "Confirmed",
            startColor = Color(0xFF16A34A),
            endColor = Color(0xFF10B981)
        ),
        BanglaSticker(
            id = "dukkhito",
            emoji = "🙏",
            banglaText = "সরি / দুঃখিত",
            subText = "Forgive me",
            startColor = Color(0xFF7C3AED),
            endColor = Color(0xFFA855F7)
        ),
        BanglaSticker(
            id = "shuvo_jonmodin",
            emoji = "🎂",
            banglaText = "শুভ জন্মদিন!",
            subText = "Happy Birthday",
            startColor = Color(0xFFDB2777),
            endColor = Color(0xFFF59E0B)
        ),
        BanglaSticker(
            id = "valobasha",
            emoji = "💖",
            banglaText = "ভালোবাসা রইলো",
            subText = "Lots of love",
            startColor = Color(0xFFF43F5E),
            endColor = Color(0xFFC026D3)
        ),
        BanglaSticker(
            id = "shuvo_ratri",
            emoji = "🌙",
            banglaText = "শুভ রাত্রি",
            subText = "Sweet dreams",
            startColor = Color(0xFF1E3A8A),
            endColor = Color(0xFF4F46E5)
        ),
        BanglaSticker(
            id = "eid_mubarak",
            emoji = "✨",
            banglaText = "ঈদ মোবারক!",
            subText = "Blessed festival",
            startColor = Color(0xFF047857),
            endColor = Color(0xFFF59E0B)
        ),
        BanglaSticker(
            id = "kothay_acho",
            emoji = "📍",
            banglaText = "কোথায় আছো?",
            subText = "Where are you?",
            startColor = Color(0xFF0284C7),
            endColor = Color(0xFF2563EB)
        ),
        BanglaSticker(
            id = "mashallah",
            emoji = "🌟",
            banglaText = "মাশাআল্লাহ!",
            subText = "Awesome!",
            startColor = Color(0xFF8B5CF6),
            endColor = Color(0xFFEC4899)
        )
    )

    fun findById(id: String): BanglaSticker? = all.firstOrNull { it.id == id }
}

@Composable
fun BanglaStickerCard(
    sticker: BanglaSticker,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val gradient = Brush.linearGradient(colors = listOf(sticker.startColor, sticker.endColor))
    val shape = RoundedCornerShape(if (compact) 18.dp else 22.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient)
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), shape)
            .padding(
                horizontal = if (compact) 10.dp else 16.dp,
                vertical = if (compact) 12.dp else 14.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 46.dp else 62.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sticker.emoji,
                    fontSize = if (compact) 26.sp else 36.sp
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
            Text(
                text = sticker.banglaText,
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.18f)
            ) {
                Text(
                    text = sticker.subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanglaStickerPickerSheet(
    onSelectSticker: (BanglaSticker) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎨",
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.stickersSheetTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.stickersSheetSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .testTag("bangla_sticker_grid")
            ) {
                items(BanglaStickerCatalog.all, key = { it.id }) { sticker ->
                    BanglaStickerCard(
                        sticker = sticker,
                        compact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSticker(sticker)
                                onDismiss()
                            }
                            .testTag("sticker_item_${sticker.id}")
                    )
                }
            }
        }
    }
}

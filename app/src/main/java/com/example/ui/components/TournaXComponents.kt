package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.ClayAccentShadow
import com.example.ui.theme.ClayDropShadow
import com.example.ui.theme.ClayInnerHighlight
import com.example.ui.theme.ClayInnerShadow
import com.example.ui.theme.ClaySurfaceBottom
import com.example.ui.theme.ClaySurfaceTop
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleEnd
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianInput
import com.example.ui.theme.SlateSubtle

/**
 * Reusable 3D Claymorphism surface modifier:
 * soft ambient outer shadow + puffy top-to-bottom fill + sculpted inner highlight & shadow rims.
 */
fun Modifier.claySurface(
    cornerRadius: Dp = 22.dp,
    topColor: Color = ClaySurfaceTop,
    bottomColor: Color = ClaySurfaceBottom,
    shadowColor: Color = ClayDropShadow,
    elevation: Dp = 10.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor,
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(colors = listOf(topColor, bottomColor)),
            shape = shape,
        )
        .drawWithContent {
            drawContent()
            val radiusPx = cornerRadius.toPx()
            val strokePx = 2.dp.toPx()
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(ClayInnerHighlight, Color.Transparent),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx),
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, ClayInnerShadow),
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
}

/**
 * Responsive edge-to-edge Obsidian background shell that works cleanly from compact 320dp old
 * handsets up to foldables, landscape mode, and tablets without artificial phone-box clipping.
 */
@Composable
fun TournaXViewportFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
    }
}

@Composable
fun TournaXEmblem(
    size: Dp = 84.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = ClayDropShadow,
                spotColor = ClayDropShadow,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.tournax_emblem),
            contentDescription = "TournaX Emblem",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = 2.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, NeonPurpleStart),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

fun avatarDrawableForIndex(index: Long): Int = when (index.toInt()) {
    1 -> R.drawable.img_avatar_1
    2 -> R.drawable.img_avatar_2
    3 -> R.drawable.img_avatar_3
    else -> R.drawable.img_avatar_0
}

@Composable
fun TournaXAvatarImage(
    avatarIndex: Long,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    contentDescription: String = "Player Esports Avatar",
) {
    Image(
        painter = painterResource(id = avatarDrawableForIndex(avatarIndex)),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = ClayDropShadow,
                spotColor = ClayDropShadow,
            )
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = Color.White,
                shape = CircleShape,
            ),
    )
}

@Composable
fun TournaXWordmark(
    fontSize: TextUnit = 30.sp,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = MetallicSilver,
                    fontWeight = FontWeight.ExtraBold,
                ),
            ) {
                append("TOURNA")
            }
            withStyle(
                SpanStyle(
                    color = NeonPurpleStart,
                    fontWeight = FontWeight.ExtraBold,
                ),
            ) {
                append("X")
            }
        },
        fontFamily = ChakraPetchFontFamily,
        fontSize = fontSize,
        letterSpacing = 1.2.sp,
        modifier = modifier,
    )
}

@Composable
fun TournaXGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val buttonShape = RoundedCornerShape(18.dp)
    val brush = if (enabled) {
        Brush.verticalGradient(colors = listOf(NeonPurpleStart, NeonPurpleEnd))
    } else {
        Brush.verticalGradient(colors = listOf(ObsidianBorder, ObsidianBorder))
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = MetallicSilverDim,
        ),
        shape = buttonShape,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = buttonShape,
                        ambientColor = ClayAccentShadow,
                        spotColor = ClayAccentShadow,
                    )
                } else {
                    Modifier
                },
            )
            .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = brush, shape = buttonShape)
                .drawWithContent {
                    drawContent()
                    if (enabled) {
                        val r = 18.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.42f),
                                    Color.Transparent,
                                    Color(0x337A1E00),
                                ),
                            ),
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.2.dp,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun TournaXTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = "",
    prefixText: String? = null,
    errorText: String? = null,
    helperText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                color = MetallicSilver,
            ),
            modifier = Modifier.padding(bottom = 6.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            isError = errorText != null,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(color = SlateSubtle),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = label,
                    tint = if (errorText != null) CrimsonAlert else NeonPurpleSoft,
                    modifier = Modifier.size(20.dp),
                )
            },
            prefix = if (prefixText != null) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prefixText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(ObsidianBorder),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            } else null,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ClaySurfaceTop,
                unfocusedContainerColor = ObsidianInput,
                errorContainerColor = ObsidianInput,
                focusedBorderColor = NeonPurpleStart,
                unfocusedBorderColor = ObsidianBorder,
                errorBorderColor = CrimsonAlert,
                focusedTextColor = MetallicSilver,
                unfocusedTextColor = MetallicSilver,
                cursorColor = NeonPurpleStart,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        } else if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
            )
        }
    }
}

@Composable
fun TournaXCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ObsidianBorderBright,
    content: @Composable () -> Unit,
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.2.dp, borderColor),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .claySurface(cornerRadius = 22.dp),
    ) {
        content()
    }
}

@Composable
fun TournaXBadge(
    text: String,
    accentColor: Color = NeonPurpleSoft,
    backgroundColor: Color = NeonPurpleGlow,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = accentColor,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

package com.example.mobileappproyect_android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mobileappproyect_android.R
import com.example.mobileappproyect_android.data.Subscription
import com.example.mobileappproyect_android.data.SubscriptionStatus
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.Boolean
import kotlin.String
import kotlin.run

@Composable
fun SubscriptionItem(
    subscription: Subscription,
    isRenewalSoon: Boolean,
) {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = subscription.imageUrl, // Puede ser null o una URL inválida
                contentDescription = "${subscription.name} logo",
                modifier = Modifier
                    .size(56.dp) // Tamaño de la imagen
                    .clip(CircleShape), // O RoundedCornerShape(8.dp)
                contentScale = ContentScale.Crop, // O ContentScale.Fit
                placeholder = painterResource(id = R.drawable.ic_placeholder_image), // Placeholder local
                error = painterResource(id = R.drawable.ic_placeholder_image) // También usa placeholder en error
                // Si tienes un ImageLoader global con placeholder/error, Coil podría usarlo
                // si aquí no especificas `placeholder` y `error`.
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Renews: ${subscription.renewalDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRenewalSoon) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Display Status
                Text(
                    text = run {
                        val lowerCasedStatus = subscription.status.name.replace("_", " ").lowercase() // Standard Kotlin lowercase()
                        if (lowerCasedStatus.isNotEmpty()) {
                            // Take the first char, convert it to String, then toUpperCase with Locale
                            // Then append the rest of the string
                            val firstCharUpper = lowerCasedStatus.substring(0, 1).uppercase(java.util.Locale.getDefault()) // Use java.util.Locale here for String.uppercase
                            val restOfString = lowerCasedStatus.substring(1)
                            firstCharUpper + restOfString
                        } else {
                            lowerCasedStatus // Should not happen with enum names, but defensive
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            color = statusColor(subscription.status).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "${subscription.currencySymbol}${String.format("%.2f", subscription.cost)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
fun statusColor(status: SubscriptionStatus): Color {
    return when (status) {
        SubscriptionStatus.ACTIVE -> Color.Green
        SubscriptionStatus.PAUSED -> Color.Gray
        SubscriptionStatus.CANCELED -> MaterialTheme.colorScheme.error
        SubscriptionStatus.PENDING_PAYMENT -> Color(0xFFFFA500) // Orange
    }
}
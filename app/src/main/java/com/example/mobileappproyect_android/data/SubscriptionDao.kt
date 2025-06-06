package com.example.mobileappproyect_android.data // Or your actual package

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity) // Changed to SubscriptionEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubscriptions(subscriptions: List<SubscriptionEntity>) // Changed to List<SubscriptionEntity>

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity) // Changed to SubscriptionEntity

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity) // Changed to SubscriptionEntity

    @Query("DELETE FROM subscriptions WHERE id = :subscriptionId")
    suspend fun deleteSubscriptionById(subscriptionId: String) // This remains the same as it operates on an ID

    @Query("SELECT * FROM subscriptions WHERE id = :subscriptionId")
    suspend fun getSubscriptionById(subscriptionId: String): SubscriptionEntity? // Changed to return SubscriptionEntity?

    @Query("SELECT * FROM subscriptions")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> // This was already correct
}
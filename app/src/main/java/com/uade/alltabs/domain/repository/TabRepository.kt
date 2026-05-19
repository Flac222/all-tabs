package com.uade.alltabs.domain.repository

import com.uade.alltabs.domain.model.Tab
import kotlinx.coroutines.flow.Flow

interface TabRepository {
    fun getAllTabs(): Flow<List<Tab>>
    suspend fun getTabById(id: String): Tab?
    suspend fun saveTab(tab: Tab)
}

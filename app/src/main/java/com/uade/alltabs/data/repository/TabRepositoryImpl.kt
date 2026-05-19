package com.uade.alltabs.data.repository

import com.uade.alltabs.data.local.TabDao
import com.uade.alltabs.data.local.TabEntity
import com.uade.alltabs.data.remote.MusicBrainzApi
import com.uade.alltabs.domain.model.Tab
import com.uade.alltabs.domain.repository.TabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TabRepositoryImpl @Inject constructor(
    private val tabDao: TabDao,
    private val musicBrainzApi: MusicBrainzApi
) : TabRepository {
    override fun getAllTabs(): Flow<List<Tab>> {
        return tabDao.getAllTabs().map { entities ->
            entities.map { Tab(it.id, it.title, it.artist, it.content, it.createdAt) }
        }
    }

    override suspend fun getTabById(id: String): Tab? {
        val entity = tabDao.getTabById(id) ?: return null
        return Tab(entity.id, entity.title, entity.artist, entity.content, entity.createdAt)
    }

    override suspend fun saveTab(tab: Tab) {
        val entity = TabEntity(tab.id, tab.title, tab.artist, tab.content, tab.createdAt)
        tabDao.insertTab(entity)
    }
}

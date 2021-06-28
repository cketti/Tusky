/* Copyright 2021 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.scheduled

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.keylesspalace.tusky.entity.ScheduledStatus
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.rx3.await

class ScheduledTootPagingSourceFactory(
    private val mastodonApi: MastodonApi
) : () -> ScheduledTootPagingSource {

    private val scheduledTootsCache = mutableListOf<ScheduledStatus>()

    private var pagingSource: ScheduledTootPagingSource? = null

    override fun invoke(): ScheduledTootPagingSource {
        return ScheduledTootPagingSource(mastodonApi, scheduledTootsCache).also {
            pagingSource = it
        }
    }

    fun remove(status: ScheduledStatus) {
        scheduledTootsCache.remove(status)
        pagingSource?.invalidate()
    }
}

class ScheduledTootPagingSource(
    private val mastodonApi: MastodonApi,
    private val scheduledTootsCache: MutableList<ScheduledStatus>
) : PagingSource<String, ScheduledStatus>() {

    override fun getRefreshKey(state: PagingState<String, ScheduledStatus>): String? {
        return null
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, ScheduledStatus> {
        return if (params is LoadParams.Refresh && scheduledTootsCache.isNotEmpty()) {
            LoadResult.Page(
                data = scheduledTootsCache,
                prevKey = null,
                nextKey = scheduledTootsCache.lastOrNull()?.id
            )
        } else {
            try {
                val result = mastodonApi.scheduledStatuses(
                    maxId = params.key,
                    limit = params.loadSize
                ).await()

                LoadResult.Page(
                    data = result,
                    prevKey = null,
                    nextKey = result.lastOrNull()?.id
                )
            } catch (e: Exception) {
                Log.w("ScheduledTootPgngSrc", "Error loading scheduled statuses", e)
                LoadResult.Error(e)
            }
        }
    }
}

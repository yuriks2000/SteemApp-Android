/*
 * Copyright 2018, BoomApps LLC. 
 * All rights reserved.
*/
package com.boomapps.steemapp.ui.feeds

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.PagedList
import android.content.Intent
import com.boomapps.steemapp.repository.FeedType
import com.boomapps.steemapp.repository.NetworkState
import com.boomapps.steemapp.repository.RepositoryProvider
import com.boomapps.steemapp.repository.db.entities.StoryEntity
import com.boomapps.steemapp.repository.steem.SteemRepository
import com.boomapps.steemapp.ui.BaseViewModel
import com.boomapps.steemapp.ui.ViewState
import timber.log.Timber


class FeedsViewModel : BaseViewModel() {

    // BLOG
    private val blogType = MutableLiveData<FeedType>()
    private val blogRepoResult = Transformations.map(blogType, {
        RepositoryProvider.getDaoRepository().storiesFor(it, RepositoryProvider.DATABASE_PAGE_SIZE)
    })
    val blogs = Transformations.switchMap(blogRepoResult, { it.pagedList })!!
    val blogsNetworkState = Transformations.switchMap(blogRepoResult, { it.networkState })!!
    val blogsRefreshState = Transformations.switchMap(blogRepoResult, { it.refreshState })!!

    // FEED
    private val feedType = MutableLiveData<FeedType>()
    private val feedRepoResult = Transformations.map(feedType, {
        RepositoryProvider.getDaoRepository().storiesFor(it, RepositoryProvider.DATABASE_PAGE_SIZE)
    })
    val feeds = Transformations.switchMap(feedRepoResult, { it.pagedList })!!
    val feedsNetworkState = Transformations.switchMap(feedRepoResult, { it.networkState })!!
    val feedsRefreshState = Transformations.switchMap(feedRepoResult, { it.refreshState })!!

    // TRENDING
    private val trendingType = MutableLiveData<FeedType>()
    private val trendingRepoResult = Transformations.map(trendingType, {
        RepositoryProvider.getDaoRepository().storiesFor(it, RepositoryProvider.DATABASE_PAGE_SIZE)
    })
    val trendings = Transformations.switchMap(trendingRepoResult, { it.pagedList })!!
    val trendingsNetworkState = Transformations.switchMap(trendingRepoResult, { it.networkState })!!
    val trendingsRefreshState = Transformations.switchMap(trendingRepoResult, { it.refreshState })!!

    // NEW
    private val novicesType = MutableLiveData<FeedType>()
    private val novicesRepoResult = Transformations.map(novicesType, {
        RepositoryProvider.getDaoRepository().storiesFor(it, RepositoryProvider.DATABASE_PAGE_SIZE)
    })
    val novices = Transformations.switchMap(novicesRepoResult, { it.pagedList })!!
    val novicesNetworkState = Transformations.switchMap(novicesRepoResult, { it.networkState })!!
    val novicesRefreshState = Transformations.switchMap(novicesRepoResult, { it.refreshState })!!

    fun getListLiveData(type: FeedType): LiveData<PagedList<StoryEntity>> {
        return when (type) {
            FeedType.BLOG -> blogs
            FeedType.TRENDING -> trendings
            FeedType.NEW -> novices
            else -> feeds
        }
    }

    fun getNetworkState(type: FeedType): LiveData<NetworkState> {
        return when (type) {
            FeedType.TRENDING -> trendingsNetworkState
            FeedType.NEW -> novicesNetworkState
            FeedType.BLOG -> blogsNetworkState
            else -> feedsNetworkState // FeedType.FEED
        }
    }

    fun getRefreshState(type: FeedType): LiveData<NetworkState> {
        return when (type) {
            FeedType.TRENDING -> trendingsRefreshState
            FeedType.NEW -> novicesRefreshState
            FeedType.BLOG -> blogsRefreshState
            else -> feedsRefreshState // FeedType.FEED
        }
    }

    fun getStory(type: FeedType, position: Int): StoryEntity? {
        return when (type) {
            FeedType.BLOG -> blogs.value?.get(position)
            FeedType.NEW -> novices.value?.get(position)
            FeedType.TRENDING -> trendings.value?.get(position)
            else -> feeds.value?.get(position) // FeedType.FEED
        }
    }

    fun refresh(type: FeedType) {
        when (type) {
            FeedType.BLOG -> blogRepoResult.value?.refresh?.invoke()
            FeedType.TRENDING -> trendingRepoResult.value?.refresh?.invoke()
            FeedType.NEW -> novicesRepoResult.value?.refresh?.invoke()
            else -> feedRepoResult.value?.refresh?.invoke() // FeedType.FEED
        }
    }

    fun showList(type: FeedType): Boolean {
        when (type) {
            FeedType.BLOG -> {
                if (blogType.value != type) {
                    blogType.value = type
                } else {
                    return false
                }
            }
            FeedType.TRENDING -> {
                if (trendingType.value != type) {
                    trendingType.value = type
                } else {
                    return false
                }
            }
            FeedType.NEW -> {
                if (novicesType.value != type) {
                    novicesType.value = type
                } else {
                    return false
                }
            }
            else -> { //FeedType.FEED
                if (feedType.value != type) {
                    feedType.value = type
                } else {
                    return false
                }
            }
        }
        return true
    }


    fun retry(type: FeedType) {
        val listing = when (type) {
            FeedType.BLOG -> blogRepoResult?.value
            FeedType.TRENDING -> trendingRepoResult?.value
            FeedType.NEW -> novicesRepoResult?.value
            else -> feedRepoResult?.value // FeedType.FEED
        }
        listing?.retry?.invoke()
    }

    fun isActivated(type: FeedType): Boolean {
        return when (type) {
            FeedType.BLOG -> blogType.value == type
            FeedType.TRENDING -> trendingType.value == type
            FeedType.NEW -> novices.value == type
            else -> feedType.value == FeedType.FEED // FeedType.FEED
        }
    }

    fun unVote(story: StoryEntity, type: FeedType) {
        state.value = ViewState.PROGRESS
        RepositoryProvider.getSteemRepository().unvoteWithUpdate(story, type, object : SteemRepository.Callback<Boolean> {
            override fun onSuccess(result: Boolean) {
                state.postValue(ViewState.COMMON)
            }

            override fun onError(error: Throwable) {
                stringError = "Canceling vote error\n${error.message}"
                state.postValue(ViewState.FAULT_RESULT)
            }
        })
    }

    fun vote(story: StoryEntity, type: FeedType, percent: Int) {
        state.value = ViewState.PROGRESS
        RepositoryProvider.getSteemRepository().voteWithUpdate(story, type, percent, object : SteemRepository.Callback<Boolean> {
            override fun onSuccess(result: Boolean) {
                state.postValue(ViewState.COMMON)
                Timber.d("VOTE SUCCESS")
            }

            override fun onError(error: Throwable) {
                stringError = "Sending vote Error\n${error.message}"
                state.postValue(ViewState.FAULT_RESULT)
                Timber.d("VOTE ERROR >> ${error.message}")
            }
        })
    }

    fun hasPostingKey(): Boolean {
        return RepositoryProvider.getSteemRepository().hasPostingKey()
    }

    fun saveNewPostingKey(data: Intent?): Boolean {
        if (data == null) {
            return false
        }
        if (!data.hasExtra("POSTING_KEY")) {
            return false
        }
        // save key in keystore
        RepositoryProvider.getPreferencesRepository().updatePostingKey(data.getStringExtra("POSTING_KEY"))
        // add key into steemJ object
        RepositoryProvider.getSteemRepository().updatePostingKey(data.getStringExtra("POSTING_KEY"))
        return true
    }

}
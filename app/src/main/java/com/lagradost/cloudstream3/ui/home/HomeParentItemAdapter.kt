package com.lagradost.cloudstream3.ui.home

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.HomeHighlightCardBinding
import com.lagradost.cloudstream3.databinding.HomeHighlightCardTvBinding
import com.lagradost.cloudstream3.databinding.HomepageParentBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.BaseAdapter
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.ui.newSharedPool
import com.lagradost.cloudstream3.ui.result.FOCUS_SELF
import com.lagradost.cloudstream3.ui.result.setLinearListLayout
import com.lagradost.cloudstream3.ui.search.SEARCH_ACTION_LOAD
import com.lagradost.cloudstream3.ui.search.SearchClickCallback
import com.lagradost.cloudstream3.ui.setRecycledViewPool
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.PHONE
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.AppContextUtils.isRecyclerScrollable
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage

const val HIGHLIGHT_ITEM_NAME = "__highlight__"

class LoadClickCallback(
    val action: Int = 0,
    val view: View,
    val position: Int,
    val response: LoadResponse
)

class HighlightViewHolder(val binding: ViewBinding) : ViewHolderState<Bundle>(binding) {
    override fun save(): Bundle = Bundle()
    override fun restore(state: Bundle) {}
}

open class ParentItemAdapter(
    id: Int,
    private val clickCallback: (SearchClickCallback) -> Unit,
    private val moreInfoClickCallback: (HomeViewModel.ExpandableHomepageList) -> Unit,
    private val expandCallback: ((String) -> Unit)? = null,
) : BaseAdapter<HomeViewModel.ExpandableHomepageList, Bundle>(
    id,
    diffCallback = BaseDiffCallback(
        itemSame = { a, b -> a.list.name == b.list.name },
        contentSame = { a, b ->
            a.list.list == b.list.list
        })
) {
    companion object {
        val sharedPool =
            newSharedPool { setMaxRecycledViews(CONTENT, 4) }
    }

    data class ParentItemHolder(val binding: ViewBinding) : ViewHolderState<Bundle>(binding) {
        override fun save(): Bundle = Bundle().apply {
            val recyclerView = (binding as? HomepageParentBinding)?.homeChildRecyclerview
            putParcelable(
                "value",
                recyclerView?.layoutManager?.onSaveInstanceState()
            )
            (recyclerView?.adapter as? BaseAdapter<*, *>)?.save(recyclerView)
        }

        override fun restore(state: Bundle) {
            (binding as? HomepageParentBinding)?.homeChildRecyclerview?.layoutManager?.onRestoreInstanceState(
                state.getSafeParcelable<Parcelable>("value")
            )
        }
    }

    override fun customContentViewType(item: HomeViewModel.ExpandableHomepageList): Int {
        return if (item.list.name == HIGHLIGHT_ITEM_NAME) 1 else 0
    }

    override fun onCreateCustomContent(parent: ViewGroup, viewType: Int): ViewHolderState<Bundle> {
        if (viewType == 1) {
            val inflater = LayoutInflater.from(parent.context)
            val binding = if (isLayout(TV or EMULATOR)) {
                HomeHighlightCardTvBinding.inflate(inflater, parent, false)
            } else {
                HomeHighlightCardBinding.inflate(inflater, parent, false)
            }
            return HighlightViewHolder(binding)
        }
        return onCreateContent(parent)
    }

    override fun submitList(
        list: Collection<HomeViewModel.ExpandableHomepageList>?,
        commitCallback: Runnable?
    ) {
        // Stable sort: empty categories go to the end, preserving original order within groups
        super.submitList(list?.sortedBy { it.list.list.isEmpty() }, commitCallback)
    }

    override fun onUpdateContent(
        holder: ViewHolderState<Bundle>,
        item: HomeViewModel.ExpandableHomepageList,
        position: Int
    ) {
        // Skip update for highlight cards
        if (item.list.name == HIGHLIGHT_ITEM_NAME) return
        val binding = holder.view
        if (binding !is HomepageParentBinding) return
        (binding.homeChildRecyclerview.adapter as? HomeChildItemAdapter)?.submitList(item.list.list)
    }

    override fun onBindContent(
        holder: ViewHolderState<Bundle>,
        item: HomeViewModel.ExpandableHomepageList,
        position: Int
    ) {
        val binding = holder.view

        // Handle highlight card
        if (item.list.name == HIGHLIGHT_ITEM_NAME && binding is HomeHighlightCardBinding) {
            val searchItem = item.list.list.firstOrNull() ?: return
            binding.apply {
                highlightTitle.text = searchItem.name
                highlightImage.loadImage(
                    searchItem.posterUrl,
                    headers = searchItem.posterHeaders
                )
                val typeText = searchItem.type?.let { type ->
                    when (type) {
                        com.lagradost.cloudstream3.TvType.Movie -> "Filme"
                        com.lagradost.cloudstream3.TvType.TvSeries -> "Serie"
                        com.lagradost.cloudstream3.TvType.Anime -> "Anime"
                        else -> type.name
                    }
                } ?: ""
                val scoreText = searchItem.score?.toStringNull(0.1, 10, 1, false)
                val parts = mutableListOf<String>()
                if (typeText.isNotEmpty()) parts.add(typeText)
                if (scoreText != null) parts.add("Nota: $scoreText")
                highlightSubtitle.text = parts.joinToString(" - ")
                highlightDescription.text = ""
                highlightPlayButton.setOnClickListener {
                    clickCallback(
                        SearchClickCallback(
                            SEARCH_ACTION_LOAD,
                            it,
                            -1,
                            searchItem
                        )
                    )
                }
                highlightCard.setOnClickListener {
                    clickCallback(
                        SearchClickCallback(
                            SEARCH_ACTION_LOAD,
                            it,
                            -1,
                            searchItem
                        )
                    )
                }
            }
            return
        }

        // Handle highlight card (TV variant)
        if (item.list.name == HIGHLIGHT_ITEM_NAME && binding is HomeHighlightCardTvBinding) {
            val searchItem = item.list.list.firstOrNull() ?: return
            binding.apply {
                highlightTitle.text = searchItem.name
                highlightImage.loadImage(
                    searchItem.posterUrl,
                    headers = searchItem.posterHeaders
                )
                val typeText = searchItem.type?.let { type ->
                    when (type) {
                        com.lagradost.cloudstream3.TvType.Movie -> "Filme"
                        com.lagradost.cloudstream3.TvType.TvSeries -> "Serie"
                        com.lagradost.cloudstream3.TvType.Anime -> "Anime"
                        else -> type.name
                    }
                } ?: ""
                val scoreText = searchItem.score?.toStringNull(0.1, 10, 1, false)
                val parts = mutableListOf<String>()
                if (typeText.isNotEmpty()) parts.add(typeText)
                if (scoreText != null) parts.add("Nota: $scoreText")
                highlightSubtitle.text = parts.joinToString(" - ")
                highlightDescription.text = ""
                highlightPlayButton.setOnClickListener {
                    clickCallback(
                        SearchClickCallback(
                            SEARCH_ACTION_LOAD,
                            it,
                            -1,
                            searchItem
                        )
                    )
                }
                highlightCard.setOnClickListener {
                    clickCallback(
                        SearchClickCallback(
                            SEARCH_ACTION_LOAD,
                            it,
                            -1,
                            searchItem
                        )
                    )
                }
            }
            return
        }

        // Handle regular category rows
        val startFocus = R.id.nav_rail_view
        val endFocus = FOCUS_SELF
        if (binding !is HomepageParentBinding) return
        val info = item.list
        binding.apply {
            val currentAdapter = homeChildRecyclerview.adapter as? HomeChildItemAdapter
            if (currentAdapter == null) {
                homeChildRecyclerview.setRecycledViewPool(HomeChildItemAdapter.sharedPool)
                homeChildRecyclerview.adapter = HomeChildItemAdapter(
                    id = id + position + 100,
                    clickCallback = clickCallback,
                    nextFocusUp = homeChildRecyclerview.nextFocusUpId,
                    nextFocusDown = homeChildRecyclerview.nextFocusDownId,
                ).apply {
                    isHorizontal = info.isHorizontalImages
                    hasNext = item.hasNext
                    submitList(item.list.list)
                }
            } else {
                currentAdapter.apply {
                    isHorizontal = info.isHorizontalImages
                    hasNext = item.hasNext
                    this.clickCallback = this@ParentItemAdapter.clickCallback
                    nextFocusUp = homeChildRecyclerview.nextFocusUpId
                    nextFocusDown = homeChildRecyclerview.nextFocusDownId
                    submitIncomparableList(item.list.list)
                }
            }

            homeChildRecyclerview.setLinearListLayout(
                isHorizontal = true,
                nextLeft = startFocus,
                nextRight = endFocus,
            )
            homeChildMoreInfo.text = info.name

            homeChildRecyclerview.addOnScrollListener(object :
                RecyclerView.OnScrollListener() {
                var expandCount = 0
                val name = item.list.name

                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int
                ) {
                    super.onScrollStateChanged(recyclerView, newState)

                    val adapter = recyclerView.adapter
                    if (adapter !is HomeChildItemAdapter) return

                    val count = adapter.itemCount
                    val hasNext = adapter.hasNext
                    /*println(
                        "scolling ${recyclerView.isRecyclerScrollable()} ${
                            recyclerView.canScrollHorizontally(
                                1
                            )
                        }"
                    )*/
                    //!recyclerView.canScrollHorizontally(1)
                    if (!recyclerView.isRecyclerScrollable() && hasNext && expandCount != count) {
                        expandCount = count
                        expandCallback?.invoke(name)
                    }
                }
            })

            //(recyclerView.adapter as HomeChildItemAdapter).notifyDataSetChanged()
            if (isLayout(PHONE)) {
                homeChildMoreInfo.setOnClickListener {
                    moreInfoClickCallback.invoke(item)
                }
            }
        }
    }

    override fun onCreateContent(parent: ViewGroup): ParentItemHolder {
        val layoutResId = when {
            isLayout(TV) -> R.layout.homepage_parent_tv
            isLayout(EMULATOR) -> R.layout.homepage_parent_emulator
            else -> R.layout.homepage_parent
        }

        val inflater = LayoutInflater.from(parent.context)
        val binding = try {
            HomepageParentBinding.bind(inflater.inflate(layoutResId, parent, false))
        } catch (t: Throwable) {
            logError(t)
            // just in case someone forgot we don't want to crash
            HomepageParentBinding.inflate(inflater)
        }

        return ParentItemHolder(binding)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T> Bundle.getSafeParcelable(key: String): T? =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) getParcelable(key)
    else getParcelable(key, T::class.java)
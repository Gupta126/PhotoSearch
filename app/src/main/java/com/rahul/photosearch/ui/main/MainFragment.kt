package com.rahul.photosearch.ui.main

import android.content.Context
import android.content.Intent
import android.database.MatrixCursor
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.paging.PagedList
import com.fivehundredpx.greedolayout.GreedoLayoutManager
import com.fivehundredpx.greedolayout.GreedoLayoutSizeCalculator
import com.fivehundredpx.greedolayout.GreedoSpacingItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.rahul.photosearch.R
import dagger.android.support.DaggerFragment
import com.rahul.photosearch.data.Result
import com.rahul.photosearch.data.flickr.FlickrPhoto
import com.rahul.photosearch.data.flickr.FlickrTag
import com.rahul.photosearch.data.glide.FlickrModelLoader
import com.rahul.photosearch.databinding.MainChipBinding
import com.rahul.photosearch.databinding.MainFragmentBinding
import com.rahul.photosearch.databinding.MainRecyclerViewItemBinding
import com.rahul.photosearch.databinding.MainSelectedChipBinding
import com.rahul.photosearch.di.GlideApp
import com.rahul.photosearch.ui.common.*
import com.rahul.photosearch.ui.photo.PhotoActivity
import javax.inject.Inject

class MainFragment : DaggerFragment(), OnActivityReenterListener {
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: MainFragmentBinding
    private lateinit var viewModel: MainViewModel

    private var currentPosition = 0
    private var hasPendingTransition = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (requireActivity() as? OnActivityReenterListener.Host)?.addListener(this)
    }

    override fun onDetach() {
        (requireActivity() as? OnActivityReenterListener.Host)?.removeListener(this)
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = MainFragmentBinding.inflate(inflater, container, false).also {
            it.setLifecycleOwner(this)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory).get()

        val adapter = Adapter(itemClickListener)

        binding.viewModel = viewModel
        binding.apply {
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
            recyclerView.apply {
                this.adapter = adapter
                layoutManager = GreedoLayoutManager(adapter).apply {
                    setMaxRowHeight(resources.getDimensionPixelSize(R.dimen.grid_max_row_height))
                }
                addItemDecoration(
                    GreedoSpacingItemDecoration(
                        resources.getDimensionPixelSize(R.dimen.grid_inner_inset)
                    )
                )

                attachExitSharedElementCallback(requireActivity(), {
                    currentPosition
                }) { viewHolder, names ->
                    val binding = viewHolder.let {
                        @Suppress("UNCHECKED_CAST")
                        it as DataBindingViewHolder<FlickrPhoto, MainRecyclerViewItemBinding>
                    }.binding
                    Log.d("Test", names.toString())
                    return@attachExitSharedElementCallback mapOf(names[0] to binding.imageView)
                }

                if (hasPendingTransition) {
                    executePostponedTransition(requireActivity(), currentPosition)
                }
            }
        }

        viewModel.tags.observe(viewLifecycleOwner, Observer {
            replaceTags(it)
        })

        viewModel.photos.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is Result.Success -> {
                    if (adapter.submitList(result.result)) {
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
                is Result.Error -> {
                    displayError(result.error)
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_fragment_menu, menu)
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.apply {
            setOnQueryTextListener(queryTextListener)

            suggestionsAdapter = SimpleCursorAdapter(
                requireContext(),
                R.layout.main_search_item,
                null,
                arrayOf("content"),
                intArrayOf(R.id.tag_text),
                0
            ).also { adapter ->
                viewModel.relatedTags.observe(viewLifecycleOwner, Observer { result ->
                    if (result !is Result.Success) {
                        return@Observer
                    }
                    val tags = result.result
                    MatrixCursor(arrayOf("_id", "content")).apply {
                        tags.forEachIndexed { index, tag -> addRow(arrayOf(index, tag.content)) }
                        adapter.changeCursor(this)
                    }
                })
            }

            setOnSuggestionListener(suggestionListener)
            setOnCloseListener {
                viewModel.queryTextChanged("")
                return@setOnCloseListener false
            }
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        val position = data?.getIntExtra(PhotoActivity.ARG_CURRENT_POSITION, currentPosition) ?: return
        requireActivity().supportPostponeEnterTransition()

        currentPosition = position
        if (::binding.isInitialized) {
            binding.recyclerView.executePostponedTransition(requireActivity(), currentPosition)
        } else {
            hasPendingTransition = true
        }
    }

    private fun replaceTags(it: List<ChipTag>) {
        binding.tagList.removeAllViews()
        it.forEach { chipTag ->
            when (chipTag) {
                is ChipTag.Added -> MainSelectedChipBinding.inflate(
                    requireActivity().layoutInflater,
                    binding.tagList,
                    true
                ).apply {
                    item = chipTag.tag
                    chip.setOnCloseIconClickListener {
                        viewModel.tagChanged(chipTag.tag, false)
                    }
                }

                is ChipTag.Suggested -> MainChipBinding.inflate(
                    requireActivity().layoutInflater,
                    binding.tagList,
                    true
                ).apply {
                    item = chipTag.tag
                    chip.setOnClickListener {
                        viewModel.tagChanged(chipTag.tag, true)
                    }
                }

                is ChipTag.Query -> MainSelectedChipBinding.inflate(
                    requireActivity().layoutInflater,
                    binding.tagList,
                    true
                ).apply {
                    item = FlickrTag(chipTag.text)
                    chip.setChipBackgroundColorResource(R.color.colorAccent)
                    chip.setOnCloseIconClickListener {
                        viewModel.queryTextSubmitted("")
                    }
                }
            }
        }
    }

    private fun displayError(error: Throwable) {
        Snackbar.make(binding.root, error.localizedMessage, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_retry) {
                viewModel.refresh()
            }
            .show()
    }

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            viewModel.queryTextSubmitted(query)
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            viewModel.queryTextChanged(newText)
            return false
        }
    }

    private val suggestionListener = object : SearchView.OnSuggestionListener {
        override fun onSuggestionSelect(position: Int): Boolean {
            return true
        }

        override fun onSuggestionClick(position: Int): Boolean {
            viewModel.suggestionClicked(position)
            return true
        }
    }

    private val itemClickListener = View.OnClickListener { v ->
        val holder = binding.recyclerView.findContainingViewHolder(v)?.let {
            @Suppress("UNCHECKED_CAST")
            it as? DataBindingViewHolder<FlickrPhoto, MainRecyclerViewItemBinding>
        } ?: return@OnClickListener

        currentPosition = holder.adapterPosition

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            Pair(holder.binding.imageView, holder.binding.imageView.transitionName)
        )
        startActivity(
            PhotoActivity.launchIntent(requireActivity(), currentPosition),
            options.toBundle()
        )
    }

    /**
     * Use a normal Adapter here since the number of items in the list can be in the millions,
     * and diff callbacks between PagedList are not really necessary.
     */
    private class Adapter(private val onItemClickListener: View.OnClickListener) :
        DataBindingAdapter<FlickrPhoto?, MainRecyclerViewItemBinding>(),
        GreedoLayoutSizeCalculator.SizeCalculatorDelegate {
        private var items: PagedList<FlickrPhoto>? = null
        private val callback = object : PagedList.Callback() {
            override fun onChanged(position: Int, count: Int) {
                notifyItemRangeChanged(position, count)
            }

            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
            }
        }

        override fun onBindViewHolder(
            holder: DataBindingViewHolder<FlickrPhoto?, MainRecyclerViewItemBinding>,
            position: Int
        ) {
            super.onBindViewHolder(holder, position)

            getItem(position)?.let { it ->
                holder.binding.apply {
                    ViewCompat.setTransitionName(imageView, it.id)

                    GlideApp.with(holder.itemView)
                        .load(it)
                        .set(FlickrModelLoader.THUMBNAIL, true)
                        .into(holder.binding.imageView)

                    root.setOnClickListener(onItemClickListener)
                    holder.binding.titleView.text = it.title
                }
            }
        }

        override fun getLayoutId(position: Int): Int {
            return R.layout.main_recycler_view_item
        }

        override fun getItem(position: Int): FlickrPhoto? {
            return items?.run {
                loadAround(position)
                get(position)
            }
        }

        override fun getItemCount(): Int {
            return items?.size ?: 0
        }

        override fun aspectRatioForIndex(position: Int): Double {
            val item = getItem(position) ?: return 1.0

            return (1.0 * item.thumbnailWidth) / item.thumbnailHeight
        }

        /**
         * Submit a new list. Return true if the list actually changes.
         */
        fun submitList(items: PagedList<FlickrPhoto>): Boolean {
            if (items === this.items) {
                return false
            }

            this.items?.apply {
                removeWeakCallback(callback)
            }
            items.addWeakCallback(null, callback)
            this.items = items
            notifyDataSetChanged()
            return true
        }
    }
}
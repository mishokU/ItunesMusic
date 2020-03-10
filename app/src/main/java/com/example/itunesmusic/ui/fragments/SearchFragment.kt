package com.example.itunesmusic.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.itunesmusic.databinding.FragmentSearchBinding
import com.example.itunesmusic.domain.converters.bindEmptyList
import com.example.itunesmusic.domain.converters.bindProgressBar
import com.example.itunesmusic.domain.viewModel.SearchAlbumsViewModel
import com.example.itunesmusic.domain.viewModelFactories.SearchAlbumsViewModelFactory
import com.example.itunesmusic.ui.adapters.AllAlbumsRecyclerViewAdapter
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SearchFragment : Fragment() {

    private lateinit var binding : FragmentSearchBinding
    private lateinit var adapter : AllAlbumsRecyclerViewAdapter
    private lateinit var viewModel : SearchAlbumsViewModel

    /*
    *   This composite disposable control user search input
    */
    private lateinit var compositeDisposable: CompositeDisposable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearchBinding.inflate(inflater)

        compositeDisposable = CompositeDisposable()

        initToolbar()
        initViewModel()
        initRecyclerView()
        initSearch()
        initObservables()

        return binding.root
    }

    private fun initObservables() {

        //Upcoming data trigger view model and we will get all possible albums
        viewModel.searchedAlbums.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })

        //This object is waiting for clicked album and clear value
        viewModel.fullAlbumDescription.observe(viewLifecycleOwner, Observer {
            it?.let {
                this.findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToOneAlbumFragment(it))
                viewModel.showFullAlbumComplete()
            }
        })

        viewModel.networkStatus.observe(viewLifecycleOwner, Observer {
            bindProgressBar(binding.searchPb, it)
            bindEmptyList(binding.searchTv, it)
        })
    }

    private fun initSearch() {
        /*
            When we add object wwe skip first value from text view
            after each press observer wait for 700 mill sec to fall down in the chain
            wait response on main thread and working in the background

        */
        compositeDisposable.add(
            RxTextView.textChanges(binding.searchText)
                .skipInitialValue()
                .debounce(700, TimeUnit.MILLISECONDS)
                .map(CharSequence::toString)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .doOnNext {
                    viewModel.searchAlbum(it)
                }
                .subscribe()
        )
    }

    private fun initRecyclerView() {
        adapter = AllAlbumsRecyclerViewAdapter(AllAlbumsRecyclerViewAdapter.OnAlbumClickListener {
            it?.let {
                viewModel.showFullAlbum(it)
            }
        })
        binding.searchedDataRv.adapter = adapter
    }

    private fun initViewModel() {
        //Init one single instance of view model
        val application = requireNotNull(activity).application
        val viewModelFactory = SearchAlbumsViewModelFactory(application)
        viewModel = ViewModelProvider(this,viewModelFactory).get(SearchAlbumsViewModel::class.java)
        binding.lifecycleOwner = this
    }

    private fun initToolbar() {
        //Init toolbar with home button and hide keyboard after press
        ((activity) as AppCompatActivity).setSupportActionBar(binding.searchToolbar)
        ((activity) as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.searchToolbar.title = ""
        binding.searchToolbar.setNavigationOnClickListener {
            this.findNavController().navigateUp()
            (activity as AppCompatActivity).hideKeyboard()
        }
    }

    private fun AppCompatActivity.hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onDestroyView() {
        //Clear all boundless from rx
        compositeDisposable.dispose()
        super.onDestroyView()
    }
}

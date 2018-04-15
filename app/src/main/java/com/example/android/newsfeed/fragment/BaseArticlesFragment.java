package com.example.android.newsfeed.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.newsfeed.EmptyRecyclerView;
import com.example.android.newsfeed.News;
import com.example.android.newsfeed.NewsLoader;
import com.example.android.newsfeed.R;
import com.example.android.newsfeed.adapter.NewsAdapter;
import com.example.android.newsfeed.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * The BaseArticlesFragment is a {@link Fragment} subclass that implements the LoaderManager.LoaderCallbacks
 * interface in order for Fragment to be a client that interacts with the LoaderManager. It is
 * base class that is responsible for displaying a set of articles, regardless of type.
 */
public class BaseArticlesFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<List<News>>{

    private static final String LOG_TAG = BaseArticlesFragment.class.getName();

    /** Constant value for the news loader ID. */
    private static final int NEWS_LOADER_ID = 1;

    /** Adapter for the list of news */
    private NewsAdapter mAdapter;

    /** TextView that is displayed when the recycler view is empty */
    private TextView mEmptyStateTextView;

    /** Loading indicator that is displayed before the first load is completed */
    private View mLoadingIndicator;

    /** The {@link android.support.v4.widget.SwipeRefreshLayout} that detects swipe gestures and
     * triggers callbacks in the app.
     */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Find a reference to the {@link RecyclerView} in the layout
        // Replaced RecyclerView with EmptyRecyclerView
        EmptyRecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setHasFixedSize(true);

        // Set the layoutManager on the {@link RecyclerView}
        mRecyclerView.setLayoutManager(layoutManager);

        // Find the SwipeRefreshLayout
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh);
        // Set the color scheme of the SwipeRefreshLayout
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.swipe_color_1),
                getResources().getColor(R.color.swipe_color_2),
                getResources().getColor(R.color.swipe_color_3),
                getResources().getColor(R.color.swipe_color_4));

        // Set up OnRefreshListener that is invoked when the user performs a swipe-to-refresh gesture.
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(LOG_TAG, "onRefresh called from SwipeRefreshLayout");
                // restart the loader
                initiateRefresh();
                Toast.makeText(getActivity(), getString(R.string.updated_just_now),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Find the loading indicator from the layout
        mLoadingIndicator = rootView.findViewById(R.id.loading_indicator);

        // Find the empty view from the layout and set it on the new recycler view
        mEmptyStateTextView = rootView.findViewById(R.id.empty_view);
        mRecyclerView.setEmptyView(mEmptyStateTextView);

        // Create a new adapter that takes an empty list of news as input
        mAdapter = new NewsAdapter(getActivity(), new ArrayList<News>());

        // Set the adapter on the {@link recyclerView}
        mRecyclerView.setAdapter(mAdapter);

        // Check for network connectivity and initialize the loader
        initializeLoader(isConnected());

        return rootView;
    }

    @Override
    public Loader<List<News>> onCreateLoader(int i, Bundle bundle) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // getString retrieves a String value from the preferences. The second parameter is the
        // default value for this preference.
        String numOfItems = sharedPrefs.getString(
                getString(R.string.settings_number_of_items_key),
                getString(R.string.settings_number_of_items_default));

        // Get the information from SharedPreferences and check for the value associated with the key
        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default));

        // Get the orderDate information from SharedPreferences and check for the value associated with the key
        String orderDate = sharedPrefs.getString(
                getString(R.string.settings_order_date_key),
                getString(R.string.settings_order_date_default));

        // Get the fromDate information from SharedPreferences and check for the value associated with the key
        String fromDate = sharedPrefs.getString(
                getString(R.string.settings_from_date_key),
                getString(R.string.settings_from_date_default));

        // Parse breaks apart the URI string that is passed into its parameter
        Uri baseUri = Uri.parse(Constants.NEWS_REQUEST_URL);

        // buildUpon prepares the baseUri that we just parsed so we can add query parameters to it
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Append query parameter and its value. (e.g. the 'show-tag=contributor')
        uriBuilder.appendQueryParameter(getString(R.string.q), "");
        uriBuilder.appendQueryParameter(getString(R.string.order_by), orderBy);
        uriBuilder.appendQueryParameter(getString(R.string.page_size), numOfItems);
        uriBuilder.appendQueryParameter(getString(R.string.order_date), orderDate);
        uriBuilder.appendQueryParameter(getString(R.string.from_date), fromDate);
        uriBuilder.appendQueryParameter(getString(R.string.show_fields), getString(R.string.thumbnail_and_trail_text));
        uriBuilder.appendQueryParameter(getString(R.string.format),getString(R.string.json));
        uriBuilder.appendQueryParameter(getString(R.string.show_tags), getString(R.string.contributor));
        uriBuilder.appendQueryParameter(getString(R.string.api_key), getString(R.string.test));

        Log.e(LOG_TAG,uriBuilder.toString());

        // Create a new loader for the given URL
        return new NewsLoader(getActivity(), uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> newsData) {
        // Hide loading indicator because the data has been loaded
        mLoadingIndicator.setVisibility(View.GONE);

        // Set empty state text to display "No news found."
        mEmptyStateTextView.setText(R.string.no_news);

        // Clear the adapter of previous news data
        mAdapter.clearAll();

        // If there is a valid list of {@link News}, then add them to the adapter's
        // data set. This will trigger the recyclerView to update.
        if (newsData != null && !newsData.isEmpty()) {
            mAdapter.addAll(newsData);
        }

        // Hide the swipe icon animation when the loader is done refreshing the data
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        // Loader reset, so we can clear out our existing data.
        mAdapter.clearAll();
    }

    /**
     * When the user returns to the previous screen by pressing the up button in the SettingsActivity,
     * restart the Loader to reflect the current value of the preference.
     */
    @Override
    public void onResume() {
        super.onResume();
        restartLoader(isConnected());
    }

    /**
     *  Check for network connectivity.
     */
    private boolean isConnected() {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * If there is internet connectivity, initialize the loader as
     * usual. Otherwise, hide loading indicator and set empty state TextView to display
     * "No internet connection."
     *
     * @param isConnected internet connection is available or not
     */
    private void initializeLoader(boolean isConnected) {
        if (isConnected) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();
            // Initialize the loader with the NEWS_LOADER_ID
            loaderManager.initLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            mLoadingIndicator.setVisibility(View.GONE);
            // Update empty state with no connection error message and image
            mEmptyStateTextView.setText(R.string.no_internet_connection);
            mEmptyStateTextView.setCompoundDrawablesWithIntrinsicBounds(Constants.DEFAULT_NUMBER,
                    R.drawable.ic_network_check,Constants.DEFAULT_NUMBER,Constants.DEFAULT_NUMBER);
        }
    }

    /**
     * Restart the loader if there is internet connectivity.
     * @param isConnected internet connection is available or not
     */
    private void restartLoader(boolean isConnected) {
        if (isConnected) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();
            // Restart the loader with the NEWS_LOADER_ID
            loaderManager.restartLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            mLoadingIndicator.setVisibility(View.GONE);
            // Update empty state with no connection error message and image
            mEmptyStateTextView.setText(R.string.no_internet_connection);
            mEmptyStateTextView.setCompoundDrawablesWithIntrinsicBounds(Constants.DEFAULT_NUMBER,
                    R.drawable.ic_network_check,Constants.DEFAULT_NUMBER,Constants.DEFAULT_NUMBER);

            // Hide SwipeRefreshLayout
            mSwipeRefreshLayout.setVisibility(View.GONE);
        }
    }

    /**
     * When the user performs a swipe-to-refresh gesture, restart the loader.
     */
    private void initiateRefresh() {
        restartLoader(isConnected());
    }
}
